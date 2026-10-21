Set-StrictMode -Version Latest

function Get-LocalLogPaths {
    param(
        [Parameter(Mandatory = $true)][string]$RootPath,
        [string]$Prefix = 'backend'
    )

    $logDirectory = Join-Path $RootPath 'logs'
    [pscustomobject]@{
        Directory = $logDirectory
        Out       = Join-Path $logDirectory "$Prefix.out.log"
        Err       = Join-Path $logDirectory "$Prefix.err.log"
        Prefix    = $Prefix
    }
}

function Get-LocalLogStamp {
    return (Get-Date).ToUniversalTime().ToString('yyyyMMdd-HHmmssfff')
}

function Move-LocalActiveLog {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Prefix,
        [Parameter(Mandatory = $true)][string]$Stream,
        [Parameter(Mandatory = $true)][string]$Directory
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return
    }

    $destination = Join-Path $Directory "$Prefix-$(Get-LocalLogStamp)-$Stream.log"
    while (Test-Path -LiteralPath $destination -PathType Leaf) {
        $destination = Join-Path $Directory "$Prefix-$(Get-LocalLogStamp)-$([guid]::NewGuid().ToString('N'))-$Stream.log"
    }
    # El entorno Windows puede bloquear Move-Item entre volúmenes/sandbox;
    # copiar y eliminar conserva la misma operación dentro del directorio.
    Copy-Item -LiteralPath $Path -Destination $destination -Force
    Remove-Item -LiteralPath $Path -Force
}

function Invoke-LocalLogRetention {
    param(
        [Parameter(Mandatory = $true)][string]$Directory,
        [Parameter(Mandatory = $true)][string]$Prefix,
        [ValidateRange(0, 3650)][int]$RetentionDays = 7,
        [ValidateRange(0, 10000)][int]$MaxHistoricalFiles = 20,
        [ValidateRange(1, 4096)][int]$MaxHistoricalSizeMb = 25
    )

    if (-not (Test-Path -LiteralPath $Directory -PathType Container)) {
        return
    }

    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    $maxBytes = [int64]$MaxHistoricalSizeMb * 1MB
    $historical = @(Get-ChildItem -LiteralPath $Directory -File -Filter "$Prefix-*.log" |
        Where-Object { $_.Name -notin @("$Prefix.out.log", "$Prefix.err.log") } |
        Sort-Object LastWriteTime -Descending)

    $index = 0
    foreach ($file in $historical) {
        $index++
        $expired = $file.LastWriteTime -lt $cutoff
        $tooMany = $index -gt $MaxHistoricalFiles
        $tooLarge = $file.Length -gt $maxBytes
        if ($expired -or $tooMany -or $tooLarge) {
            Remove-Item -LiteralPath $file.FullName -Force
        }
    }
}

function Initialize-LocalLogDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$RootPath,
        [string]$Prefix = 'backend',
        [ValidateRange(0, 3650)][int]$RetentionDays = 7,
        [ValidateRange(0, 10000)][int]$MaxHistoricalFiles = 20,
        [ValidateRange(1, 4096)][int]$MaxHistoricalSizeMb = 25
    )

    $paths = Get-LocalLogPaths -RootPath $RootPath -Prefix $Prefix
    New-Item -ItemType Directory -Path $paths.Directory -Force | Out-Null

    Move-LocalActiveLog -Path $paths.Out -Prefix $Prefix -Stream 'out' -Directory $paths.Directory
    Move-LocalActiveLog -Path $paths.Err -Prefix $Prefix -Stream 'err' -Directory $paths.Directory
    Invoke-LocalLogRetention `
        -Directory $paths.Directory `
        -Prefix $Prefix `
        -RetentionDays $RetentionDays `
        -MaxHistoricalFiles $MaxHistoricalFiles `
        -MaxHistoricalSizeMb $MaxHistoricalSizeMb

    return $paths
}

