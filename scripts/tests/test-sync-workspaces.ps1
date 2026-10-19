[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptPath = Join-Path $PSScriptRoot '..\sync-workspaces.ps1'
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ("asistenciasuco-sync-tests-" + [guid]::NewGuid().ToString('N'))
$passed = 0
$failed = 0

function Invoke-TestGit {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [switch]$CaptureOutput
    )
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& git -C $Path @Arguments 2>&1)
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed in ${Path}: $($output -join [Environment]::NewLine)"
    }
    if ($CaptureOutput) {
        return (($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine).Trim()
    }
}

function Assert-True {
    param(
        [Parameter(Mandatory = $true)][bool]$Condition,
        [Parameter(Mandatory = $true)][string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Contains {
    param(
        [Parameter(Mandatory = $true)][string]$Text,
        [Parameter(Mandatory = $true)][string]$Expected,
        [Parameter(Mandatory = $true)][string]$Message
    )
    Assert-True -Condition ($Text.Contains($Expected)) -Message "$Message. Expected '$Expected' in: $Text"
}

function New-RepositoryFixture {
    param([Parameter(Mandatory = $true)][string]$Name)

    $caseRoot = Join-Path $testRoot $Name
    $remote = Join-Path $caseRoot 'remote.git'
    $seed = Join-Path $caseRoot 'seed'
    $local = Join-Path $caseRoot 'local'
    $producer = Join-Path $caseRoot 'producer'
    New-Item -ItemType Directory -Path $caseRoot -Force | Out-Null
    Invoke-TestGit -Path $caseRoot -Arguments @('init', '--bare', $remote) | Out-Null
    New-Item -ItemType Directory -Path $seed -Force | Out-Null
    Invoke-TestGit -Path $seed -Arguments @('init', '-b', 'develop') | Out-Null
    Invoke-TestGit -Path $seed -Arguments @('config', 'user.name', 'sync-test') | Out-Null
    Invoke-TestGit -Path $seed -Arguments @('config', 'user.email', 'sync-test@example.invalid') | Out-Null
    Set-Content -LiteralPath (Join-Path $seed 'README.txt') -Value 'base' -NoNewline
    Invoke-TestGit -Path $seed -Arguments @('add', 'README.txt') | Out-Null
    Invoke-TestGit -Path $seed -Arguments @('commit', '-m', 'base') | Out-Null
    Invoke-TestGit -Path $seed -Arguments @('remote', 'add', 'origin', $remote) | Out-Null
    Invoke-TestGit -Path $seed -Arguments @('push', '-u', 'origin', 'develop') | Out-Null
    Invoke-TestGit -Path $caseRoot -Arguments @('clone', '-b', 'develop', $remote, $local) | Out-Null
    Invoke-TestGit -Path $local -Arguments @('config', 'user.name', 'sync-test') | Out-Null
    Invoke-TestGit -Path $local -Arguments @('config', 'user.email', 'sync-test@example.invalid') | Out-Null
    Invoke-TestGit -Path $caseRoot -Arguments @('clone', '-b', 'develop', $remote, $producer) | Out-Null
    Invoke-TestGit -Path $producer -Arguments @('config', 'user.name', 'sync-test') | Out-Null
    Invoke-TestGit -Path $producer -Arguments @('config', 'user.email', 'sync-test@example.invalid') | Out-Null

    [pscustomobject]@{
        Root     = $caseRoot
        Remote   = $remote
        Local    = $local
        Producer = $producer
        Expected = (Invoke-TestGit -Path $local -Arguments @('remote', 'get-url', 'origin') -CaptureOutput)
    }
}

function Write-SyncConfig {
    param(
        [Parameter(Mandatory = $true)][string]$CaseRoot,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedOrigin,
        [string]$Name = 'backend'
    )

    $configPath = Join-Path $CaseRoot 'sync.json'
    $config = [ordered]@{
        repositories = @([ordered]@{
                name = $Name
                path = $Path
                expectedOrigin = $ExpectedOrigin
                integrationBranch = 'develop'
            })
    }
    $config | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $configPath
    return $configPath
}

function Run-Sync {
    param(
        [Parameter(Mandatory = $true)][string]$ConfigPath,
        [Parameter(Mandatory = $true)][ValidateSet('Status', 'Apply')][string]$Mode
    )

    $output = (& powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $scriptPath "-$Mode" -ConfigPath $ConfigPath 2>&1 | Out-String)
    [pscustomobject]@{
        ExitCode = $LASTEXITCODE
        Output   = $output
    }
}

function New-RemoteCommit {
    param(
        [Parameter(Mandatory = $true)]$Fixture,
        [Parameter(Mandatory = $true)][string]$Text
    )
    Set-Content -LiteralPath (Join-Path $Fixture.Producer 'README.txt') -Value $Text -NoNewline
    Invoke-TestGit -Path $Fixture.Producer -Arguments @('add', 'README.txt') | Out-Null
    Invoke-TestGit -Path $Fixture.Producer -Arguments @('commit', '-m', $Text) | Out-Null
    Invoke-TestGit -Path $Fixture.Producer -Arguments @('push', 'origin', 'develop') | Out-Null
}

function Invoke-Case {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Body
    )
    try {
        & $Body
        Write-Output "PASS $Name"
        $script:passed++
    } catch {
        Write-Output "FAIL $Name :: $($_.Exception.Message)"
        $script:failed++
    }
}

try {
    New-Item -ItemType Directory -Path $testRoot -Force | Out-Null

    Invoke-Case 'up-to-date status' {
        $fixture = New-RepositoryFixture 'up-to-date'
        $config = Write-SyncConfig -CaseRoot $fixture.Root -Path $fixture.Local -ExpectedOrigin $fixture.Expected
        $run = Run-Sync -ConfigPath $config -Mode Status
        Assert-True ($run.ExitCode -eq 0) 'Status debe terminar 0 cuando está actualizado.'
        Assert-Contains $run.Output 'UP_TO_DATE' 'Debe informar el repositorio actualizado.'
    }

    Invoke-Case 'fast-forward apply' {
        $fixture = New-RepositoryFixture 'fast-forward'
        New-RemoteCommit -Fixture $fixture -Text 'remote-ahead'
        $config = Write-SyncConfig -CaseRoot $fixture.Root -Path $fixture.Local -ExpectedOrigin $fixture.Expected
        $run = Run-Sync -ConfigPath $config -Mode Apply
        Assert-True ($run.ExitCode -eq 0) 'Fast-forward seguro debe terminar 0.'
        Assert-Contains $run.Output 'FAST_FORWARDED' 'Debe informar fast-forward.'
        Assert-True ((Invoke-TestGit -Path $fixture.Local -Arguments @('rev-parse', 'develop') -CaptureOutput) -eq (Invoke-TestGit -Path $fixture.Local -Arguments @('rev-parse', 'origin/develop') -CaptureOutput)) 'La rama local debe quedar alineada.'
        $repeat = Run-Sync -ConfigPath $config -Mode Apply
        Assert-True ($repeat.ExitCode -eq 0) 'La repetición idempotente debe terminar 0.'
        Assert-Contains $repeat.Output 'UP_TO_DATE' 'La repetición no debe mover la rama.'
    }

    Invoke-Case 'feature branch preserved' {
        $fixture = New-RepositoryFixture 'feature-branch'
        Invoke-TestGit -Path $fixture.Local -Arguments @('switch', '-c', 'feature/safe') | Out-Null
        Set-Content -LiteralPath (Join-Path $fixture.Local 'feature.txt') -Value 'feature' -NoNewline
        Invoke-TestGit -Path $fixture.Local -Arguments @('add', 'feature.txt') | Out-Null
        Invoke-TestGit -Path $fixture.Local -Arguments @('commit', '-m', 'feature') | Out-Null
        New-RemoteCommit -Fixture $fixture -Text 'remote-ahead'
        $config = Write-SyncConfig -CaseRoot $fixture.Root -Path $fixture.Local -ExpectedOrigin $fixture.Expected
        $run = Run-Sync -ConfigPath $config -Mode Apply
        Assert-True ($run.ExitCode -eq 0) 'Una feature limpia debe poder actualizar la línea base inactiva.'
        Assert-Contains $run.Output 'FAST_FORWARDED_INACTIVE_BRANCH' 'Debe preservar la feature y actualizar develop.'
        Assert-True ((Invoke-TestGit -Path $fixture.Local -Arguments @('branch', '--show-current') -CaptureOutput) -eq 'feature/safe') 'La rama activa no debe cambiar.'
    }

    Invoke-Case 'dirty worktree and untracked preservation' {
        $fixture = New-RepositoryFixture 'dirty-worktree'
        New-RemoteCommit -Fixture $fixture -Text 'remote-ahead'
        Set-Content -LiteralPath (Join-Path $fixture.Local 'README.txt') -Value 'local-change' -NoNewline
        $untracked = Join-Path $fixture.Local 'keep-me.txt'
        Set-Content -LiteralPath $untracked -Value 'must-survive' -NoNewline
        $before = Invoke-TestGit -Path $fixture.Local -Arguments @('rev-parse', 'develop') -CaptureOutput
        $config = Write-SyncConfig -CaseRoot $fixture.Root -Path $fixture.Local -ExpectedOrigin $fixture.Expected
        $run = Run-Sync -ConfigPath $config -Mode Apply
        Assert-True ($run.ExitCode -ne 0) 'Worktree dirty debe quedar bloqueado.'
        Assert-Contains $run.Output 'DIRTY_WORKTREE_FETCH_ONLY' 'Debe informar fetch-only.'
        Assert-True ((Get-Content -LiteralPath $untracked -Raw) -eq 'must-survive') 'El archivo untracked debe preservarse.'
        Assert-True ((Invoke-TestGit -Path $fixture.Local -Arguments @('rev-parse', 'develop') -CaptureOutput) -eq $before) 'La rama no debe moverse con cambios locales.'
    }

    Invoke-Case 'divergent branches' {
        $fixture = New-RepositoryFixture 'divergent'
        Set-Content -LiteralPath (Join-Path $fixture.Local 'local.txt') -Value 'local' -NoNewline
        Invoke-TestGit -Path $fixture.Local -Arguments @('add', 'local.txt') | Out-Null
        Invoke-TestGit -Path $fixture.Local -Arguments @('commit', '-m', 'local-only') | Out-Null
        New-RemoteCommit -Fixture $fixture -Text 'remote-only'
        $config = Write-SyncConfig -CaseRoot $fixture.Root -Path $fixture.Local -ExpectedOrigin $fixture.Expected
        $run = Run-Sync -ConfigPath $config -Mode Apply
        Assert-True ($run.ExitCode -ne 0) 'Divergencia debe bloquear.'
        Assert-Contains $run.Output 'DIVERGED' 'Debe informar divergencia.'
    }

    Invoke-Case 'incorrect remote' {
        $fixture = New-RepositoryFixture 'incorrect-remote'
        $config = Write-SyncConfig -CaseRoot $fixture.Root -Path $fixture.Local -ExpectedOrigin 'https://github.com/AsistenciasUCO/AsistenciasUCO-Backend.git'
        $run = Run-Sync -ConfigPath $config -Mode Status
        Assert-True ($run.ExitCode -ne 0) 'Remoto incorrecto debe bloquear.'
        Assert-Contains $run.Output 'REMOTE_MISMATCH' 'Debe informar remoto incorrecto.'
    }

    Invoke-Case 'missing repository' {
        $caseRoot = Join-Path $testRoot 'missing-repository'
        New-Item -ItemType Directory -Path $caseRoot -Force | Out-Null
        $missing = Join-Path $caseRoot 'does-not-exist'
        $config = Write-SyncConfig -CaseRoot $caseRoot -Path $missing -ExpectedOrigin 'https://github.com/AsistenciasUCO/AsistenciasUCO-Backend.git'
        $run = Run-Sync -ConfigPath $config -Mode Status
        Assert-True ($run.ExitCode -ne 0) 'Repositorio faltante debe bloquear.'
        Assert-Contains $run.Output 'MISSING_REPOSITORY' 'Debe informar repositorio faltante.'
    }

    Invoke-Case 'network error' {
        $fixture = New-RepositoryFixture 'network-error'
        Invoke-TestGit -Path $fixture.Local -Arguments @('remote', 'set-url', 'origin', 'file:///path-that-does-not-exist/asistencias.git') | Out-Null
        $config = Write-SyncConfig -CaseRoot $fixture.Root -Path $fixture.Local -ExpectedOrigin 'file:///path-that-does-not-exist/asistencias.git'
        $run = Run-Sync -ConfigPath $config -Mode Apply
        Assert-True ($run.ExitCode -ne 0) 'Error de fetch debe bloquear.'
        Assert-Contains $run.Output 'FETCH_FAILED' 'Debe informar el error de red/fetch.'
    }
} finally {
    if (Test-Path -LiteralPath $testRoot) {
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    }
}

Write-Output "sync-workspaces tests: passed=$passed failed=$failed"
if ($failed -gt 0) {
    exit 1
}
exit 0
