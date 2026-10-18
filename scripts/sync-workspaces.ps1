[CmdletBinding()]
param(
    [switch]$Status,
    [switch]$Apply,
    [string]$ConfigPath,
    [string]$BackendPath,
    [string]$FrontendPath,
    [string]$DbPath,
    [string]$InfraPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
    $ConfigPath = Join-Path $scriptDirectory 'sync-workspaces.config.json'
}

if ($Status -and $Apply) {
    throw 'Use -Status o -Apply, no ambos.'
}

$mode = if ($Apply) { 'APPLY' } else { 'STATUS' }

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)][string]$Repository,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& git -C $Repository @Arguments 2>&1)
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $exitCode = $LASTEXITCODE
    [pscustomobject]@{
        ExitCode = $exitCode
        Output   = (($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine).Trim()
    }
}

function Get-GitValue {
    param(
        [Parameter(Mandatory = $true)][string]$Repository,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $result = Invoke-Git -Repository $Repository -Arguments $Arguments
    if ($result.ExitCode -ne 0) {
        return $null
    }
    return $result.Output.Trim()
}

function Resolve-ConfiguredPath {
    param([Parameter(Mandatory = $true)][string]$Value)

    $expanded = [Environment]::ExpandEnvironmentVariables($Value.Trim())
    if ($expanded.StartsWith('~')) {
        $userProfile = $env:USERPROFILE
        if ([string]::IsNullOrWhiteSpace($userProfile)) {
            throw 'No se pudo resolver ~ porque USERPROFILE no está disponible.'
        }
        $expanded = Join-Path $userProfile $expanded.Substring(1).TrimStart([char[]]"\/")
    }
    return [IO.Path]::GetFullPath($expanded)
}

function Normalize-RemoteUrl {
    param([Parameter(Mandatory = $true)][string]$Value)

    $normalized = $Value.Trim().ToLowerInvariant()
    $normalized = $normalized -replace '/+$', ''
    $normalized = $normalized -replace '\.git$', ''
    return $normalized
}

function Test-Ancestor {
    param(
        [Parameter(Mandatory = $true)][string]$Repository,
        [Parameter(Mandatory = $true)][string]$Ancestor,
        [Parameter(Mandatory = $true)][string]$Descendant
    )

    $result = Invoke-Git -Repository $Repository -Arguments @('merge-base', '--is-ancestor', $Ancestor, $Descendant)
    return $result.ExitCode -eq 0
}

function Get-AheadBehind {
    param(
        [Parameter(Mandatory = $true)][string]$Repository,
        [Parameter(Mandatory = $true)][string]$Left,
        [Parameter(Mandatory = $true)][string]$Right
    )

    $result = Invoke-Git -Repository $Repository -Arguments @('rev-list', '--left-right', '--count', "$Left...$Right")
    if ($result.ExitCode -ne 0) {
        return $null
    }
    $parts = $result.Output -split '\s+'
    if ($parts.Count -lt 2) {
        return $null
    }
    [pscustomobject]@{
        Ahead  = [int]$parts[0]
        Behind = [int]$parts[1]
    }
}

function Get-MergeBase {
    param(
        [Parameter(Mandatory = $true)][string]$Repository,
        [Parameter(Mandatory = $true)][string]$Left,
        [Parameter(Mandatory = $true)][string]$Right
    )

    return Get-GitValue -Repository $Repository -Arguments @('merge-base', $Left, $Right)
}

function Get-WorktreeBranches {
    param([Parameter(Mandatory = $true)][string]$Repository)

    $result = Invoke-Git -Repository $Repository -Arguments @('worktree', 'list', '--porcelain')
    $branches = @{}
    if ($result.ExitCode -ne 0) {
        return $branches
    }

    $worktreePath = $null
    foreach ($line in ($result.Output -split "`r?`n")) {
        if ($line.StartsWith('worktree ')) {
            $worktreePath = $line.Substring(9).Trim()
            continue
        }
        if ($line.StartsWith('branch ') -and $null -ne $worktreePath) {
            $branches[$line.Substring(7).Trim()] = $worktreePath
        }
    }
    return $branches
}

function Get-RepositoryConfig {
    param(
        [string]$Path,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$ExpectedOrigin,
        [Parameter(Mandatory = $true)][string]$IntegrationBranch
    )

    [pscustomobject]@{
        Name              = $Name
        Path              = $Path
        ExpectedOrigin    = $ExpectedOrigin
        IntegrationBranch = $IntegrationBranch
    }
}

$defaultRepositories = @(
    (Get-RepositoryConfig -Name 'backend' -Path $BackendPath -ExpectedOrigin 'https://github.com/AsistenciasUCO/AsistenciasUCO-Backend.git' -IntegrationBranch 'develop'),
    (Get-RepositoryConfig -Name 'frontend' -Path $FrontendPath -ExpectedOrigin 'https://github.com/AsistenciasUCO/AsistenciasUCO-Frontend.git' -IntegrationBranch 'develop'),
    (Get-RepositoryConfig -Name 'db' -Path $DbPath -ExpectedOrigin 'https://github.com/AsistenciasUCO/AsistenciasUCO-DB.git' -IntegrationBranch 'develop'),
    (Get-RepositoryConfig -Name 'infra' -Path $InfraPath -ExpectedOrigin 'https://github.com/AsistenciasUCO/AsistenciasUCO-Infra.git' -IntegrationBranch 'main')
)

$hasPathOverride = @(@($BackendPath, $FrontendPath, $DbPath, $InfraPath) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
$configExists = Test-Path -LiteralPath $ConfigPath -PathType Leaf

if ($configExists) {
    $configuration = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json
    if ($null -eq $configuration.repositories -or @($configuration.repositories).Count -eq 0) {
        throw "El archivo de configuración '$ConfigPath' no contiene repositories."
    }
    $repositories = @($configuration.repositories | ForEach-Object {
            if ([string]::IsNullOrWhiteSpace([string]$_.name) -or
                [string]::IsNullOrWhiteSpace([string]$_.path) -or
                [string]::IsNullOrWhiteSpace([string]$_.expectedOrigin) -or
                [string]::IsNullOrWhiteSpace([string]$_.integrationBranch)) {
                throw 'Cada repositorio requiere name, path, expectedOrigin e integrationBranch.'
            }
            Get-RepositoryConfig `
                -Name ([string]$_.name) `
                -Path (Resolve-ConfiguredPath -Value ([string]$_.path)) `
                -ExpectedOrigin ([string]$_.expectedOrigin) `
                -IntegrationBranch ([string]$_.integrationBranch)
        })
} elseif ($hasPathOverride.Count -eq 4) {
    $repositories = $defaultRepositories | ForEach-Object {
        $override = switch ($_.Name) {
            'backend' { $BackendPath }
            'frontend' { $FrontendPath }
            'db' { $DbPath }
            'infra' { $InfraPath }
        }
        Get-RepositoryConfig -Name $_.Name -Path (Resolve-ConfiguredPath -Value $override) `
            -ExpectedOrigin $_.ExpectedOrigin -IntegrationBranch $_.IntegrationBranch
    }
} else {
    throw "No existe '$ConfigPath'. Cree un archivo a partir de scripts/sync-workspaces.config.example.json o proporcione las cuatro rutas -BackendPath/-FrontendPath/-DbPath/-InfraPath."
}

$results = New-Object System.Collections.Generic.List[object]
$hadBlockingResult = $false

foreach ($repositoryConfig in $repositories) {
    $result = [ordered]@{
        Component       = $repositoryConfig.Name
        State           = 'NOT_RUN'
        Path            = $repositoryConfig.Path
        Origin          = ''
        ActiveBranch    = ''
        Integration     = $repositoryConfig.IntegrationBranch
        ShaBefore       = ''
        ShaAfter        = ''
        Ahead           = ''
        Behind          = ''
        MergeBase       = ''
        Message         = ''
    }

    if (-not (Test-Path -LiteralPath $repositoryConfig.Path -PathType Container)) {
        $result.State = 'MISSING_REPOSITORY'
        $result.Message = 'La ruta configurada no existe.'
        $hadBlockingResult = $true
        $results.Add([pscustomobject]$result)
        continue
    }

    $topLevel = Get-GitValue -Repository $repositoryConfig.Path -Arguments @('rev-parse', '--show-toplevel')
    if ([string]::IsNullOrWhiteSpace($topLevel)) {
        $result.State = 'INVALID_REPOSITORY'
        $result.Message = 'La carpeta no es un repositorio Git válido.'
        $hadBlockingResult = $true
        $results.Add([pscustomobject]$result)
        continue
    }

    $actualOrigin = Get-GitValue -Repository $repositoryConfig.Path -Arguments @('remote', 'get-url', 'origin')
    $result.Origin = [string]$actualOrigin
    if ([string]::IsNullOrWhiteSpace($actualOrigin)) {
        $result.State = 'MISSING_ORIGIN'
        $result.Message = 'No existe el remoto origin.'
        $hadBlockingResult = $true
        $results.Add([pscustomobject]$result)
        continue
    }
    if ((Normalize-RemoteUrl -Value $actualOrigin) -ne (Normalize-RemoteUrl -Value $repositoryConfig.ExpectedOrigin)) {
        $result.State = 'REMOTE_MISMATCH'
        $result.Message = "origin no coincide con la URL oficial esperada: $($repositoryConfig.ExpectedOrigin)"
        $hadBlockingResult = $true
        $results.Add([pscustomobject]$result)
        continue
    }

    $activeBranch = Get-GitValue -Repository $repositoryConfig.Path -Arguments @('symbolic-ref', '--quiet', '--short', 'HEAD')
    if ([string]::IsNullOrWhiteSpace($activeBranch)) {
        $activeBranch = 'DETACHED_HEAD'
    }
    $result.ActiveBranch = $activeBranch

    $localRef = "refs/heads/$($repositoryConfig.IntegrationBranch)"
    $remoteRef = "refs/remotes/origin/$($repositoryConfig.IntegrationBranch)"
    $localShaBefore = Get-GitValue -Repository $repositoryConfig.Path -Arguments @('rev-parse', '--verify', $localRef)
    $remoteSha = Get-GitValue -Repository $repositoryConfig.Path -Arguments @('rev-parse', '--verify', $remoteRef)
    $result.ShaBefore = [string]$localShaBefore

    $fetchFailed = $false
    if ($Apply) {
        $fetch = Invoke-Git -Repository $repositoryConfig.Path -Arguments @('fetch', 'origin', '--prune')
        if ($fetch.ExitCode -ne 0) {
            $fetchFailed = $true
            $result.Message = "fetch falló: $($fetch.Output)"
        }
        $remoteSha = Get-GitValue -Repository $repositoryConfig.Path -Arguments @('rev-parse', '--verify', $remoteRef)
    }

    $dirtyOutput = (Invoke-Git -Repository $repositoryConfig.Path -Arguments @('status', '--porcelain', '--untracked-files=all')).Output
    $isDirty = -not [string]::IsNullOrWhiteSpace($dirtyOutput)
    $worktreeBranches = Get-WorktreeBranches -Repository $repositoryConfig.Path
    $integrationCheckedOutElsewhere = $false
    if ($worktreeBranches.ContainsKey("refs/heads/$($repositoryConfig.IntegrationBranch)")) {
        $integrationPath = Resolve-ConfiguredPath -Value ([string]$worktreeBranches["refs/heads/$($repositoryConfig.IntegrationBranch)"])
        $repositoryRoot = Resolve-ConfiguredPath -Value $topLevel
        $integrationCheckedOutElsewhere = $integrationPath -ne $repositoryRoot
    }

    if ([string]::IsNullOrWhiteSpace($localShaBefore)) {
        $result.State = if ($fetchFailed) { 'FETCH_FAILED' } else { 'MISSING_LOCAL_INTEGRATION_BRANCH' }
        if ([string]::IsNullOrWhiteSpace($result.Message)) {
            $result.Message = 'No existe la rama local de integración; no se crea automáticamente en -Status.'
        }
        $hadBlockingResult = $true
        $results.Add([pscustomobject]$result)
        continue
    }
    if ([string]::IsNullOrWhiteSpace($remoteSha)) {
        $result.State = if ($fetchFailed) { 'FETCH_FAILED' } else { 'MISSING_REMOTE_INTEGRATION_REF' }
        if ([string]::IsNullOrWhiteSpace($result.Message)) {
            $result.Message = "No existe origin/$($repositoryConfig.IntegrationBranch)."
        }
        $hadBlockingResult = $true
        $results.Add([pscustomobject]$result)
        continue
    }

    $comparison = Get-AheadBehind -Repository $repositoryConfig.Path -Left $localRef -Right $remoteRef
    $result.Ahead = [string]$comparison.Ahead
    $result.Behind = [string]$comparison.Behind
    $result.MergeBase = [string](Get-MergeBase -Repository $repositoryConfig.Path -Left $localRef -Right $remoteRef)
    $result.ShaAfter = [string]$localShaBefore

    $featureComparison = $null
    if ($activeBranch -ne $repositoryConfig.IntegrationBranch -and $activeBranch -ne 'DETACHED_HEAD') {
        $featureComparison = Get-AheadBehind -Repository $repositoryConfig.Path -Left 'HEAD' -Right $remoteRef
    }

    if ($fetchFailed) {
        $result.State = 'FETCH_FAILED'
        $hadBlockingResult = $true
    } elseif ($comparison.Ahead -eq 0 -and $comparison.Behind -eq 0) {
        $result.State = if ($isDirty -and $Apply) { 'UP_TO_DATE_DIRTY' } else { 'UP_TO_DATE' }
        if ($isDirty -and $Apply) {
            $result.Message = 'fetch ejecutado; worktree con cambios, no se escribieron ramas ni archivos.'
        }
    } elseif (Test-Ancestor -Repository $repositoryConfig.Path -Ancestor $localRef -Descendant $remoteRef) {
        if (-not $Apply) {
            $result.State = 'FAST_FORWARD_AVAILABLE'
            $result.Message = 'Hay commits nuevos; -Status no modifica el checkout.'
        } elseif ($isDirty) {
            $result.State = 'DIRTY_WORKTREE_FETCH_ONLY'
            $result.Message = 'Hay cambios modificados o sin seguimiento; solo se hizo fetch.'
            $hadBlockingResult = $true
        } elseif ($activeBranch -eq $repositoryConfig.IntegrationBranch) {
            $merge = Invoke-Git -Repository $repositoryConfig.Path -Arguments @('merge', '--ff-only', "origin/$($repositoryConfig.IntegrationBranch)")
            if ($merge.ExitCode -eq 0) {
                $result.State = 'FAST_FORWARDED'
                $result.ShaAfter = [string](Get-GitValue -Repository $repositoryConfig.Path -Arguments @('rev-parse', $localRef))
                $result.Message = 'Rama de integración actualizada con fast-forward.'
            } else {
                $result.State = 'FAST_FORWARD_FAILED'
                $result.Message = "merge --ff-only falló: $($merge.Output)"
                $hadBlockingResult = $true
            }
        } elseif ($integrationCheckedOutElsewhere) {
            $result.State = 'INTEGRATION_BRANCH_IN_OTHER_WORKTREE'
            $result.Message = 'No se modificó la rama porque está checkout en otro worktree.'
            $hadBlockingResult = $true
        } else {
            $update = Invoke-Git -Repository $repositoryConfig.Path -Arguments @('update-ref', $localRef, $remoteSha, $localShaBefore)
            if ($update.ExitCode -eq 0) {
                $result.State = 'FAST_FORWARDED_INACTIVE_BRANCH'
                $result.ShaAfter = [string](Get-GitValue -Repository $repositoryConfig.Path -Arguments @('rev-parse', $localRef))
                $result.Message = 'Rama de integración inactiva actualizada por fast-forward; la rama de trabajo se preservó.'
            } else {
                $result.State = 'FAST_FORWARD_FAILED'
                $result.Message = "update-ref seguro falló: $($update.Output)"
                $hadBlockingResult = $true
            }
        }
    } elseif (Test-Ancestor -Repository $repositoryConfig.Path -Ancestor $remoteRef -Descendant $localRef) {
        $result.State = 'LOCAL_AHEAD'
        $result.Message = 'La rama local tiene commits no publicados; no se reemplazó.'
        $hadBlockingResult = $true
    } else {
        $result.State = 'DIVERGED'
        $result.Message = 'Las ramas divergen; se detuvo la actualización y no se hizo merge automático.'
        $hadBlockingResult = $true
    }

    if ($null -ne $featureComparison) {
        if ($featureComparison.Ahead -gt 0 -and $featureComparison.Behind -gt 0) {
            $result.Message = "$($result.Message) Feature '$activeBranch' necesita reconciliación posterior (ahead=$($featureComparison.Ahead), behind=$($featureComparison.Behind))."
        } else {
            $result.Message = "$($result.Message) Feature '$activeBranch' preservada; no se fusionó automáticamente."
        }
    }

    $results.Add([pscustomobject]$result)
}

Write-Output "P0-B sync-workspaces mode=$mode"
Write-Output "Official remotes are validated from each repository configuration; no merge or push is performed."
$results |
    Select-Object Component, State, ActiveBranch, Integration, ShaBefore, ShaAfter, Ahead, Behind, MergeBase |
    Format-Table -AutoSize |
    Out-String -Width 240 |
    Write-Output

foreach ($item in $results) {
    Write-Output ("[{0}] {1}: {2}" -f $item.State, $item.Component, $item.Message)
}

if ($hadBlockingResult) {
    exit 1
}
exit 0
