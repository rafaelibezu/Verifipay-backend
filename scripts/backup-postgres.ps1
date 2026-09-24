#requires -Version 5.1
[CmdletBinding()]
param(
    [string]$BackupDirectory
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDirectory = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($scriptDirectory)) {
    throw 'Could not resolve the script directory. Run this script with powershell -File.'
}
if ([string]::IsNullOrWhiteSpace($BackupDirectory)) {
    $BackupDirectory = Join-Path $scriptDirectory '..\backups'
}

function ConvertTo-WindowsCommandLineArgument {
    param([Parameter(Mandatory)][string]$Value)

    if ($Value.Length -eq 0) { return '""' }
    if ($Value -notmatch '[\s"]') { return $Value }

    $escaped = New-Object System.Text.StringBuilder
    [void]$escaped.Append('"')
    $backslashCount = 0
    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') {
            $backslashCount++
        } elseif ($character -eq '"') {
            [void]$escaped.Append('\', ($backslashCount * 2) + 1)
            [void]$escaped.Append('"')
            $backslashCount = 0
        } else {
            [void]$escaped.Append('\', $backslashCount)
            [void]$escaped.Append($character)
            $backslashCount = 0
        }
    }
    [void]$escaped.Append('\', $backslashCount * 2)
    [void]$escaped.Append('"')
    return $escaped.ToString()
}

function Invoke-DockerToFile {
    param([string[]]$Arguments, [string]$OutputPath)

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo.FileName = 'docker'
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.Arguments = (($Arguments | ForEach-Object { ConvertTo-WindowsCommandLineArgument $_ }) -join ' ')

    if (-not $process.Start()) { throw 'Could not start Docker.' }
    try {
        $stream = [System.IO.File]::Open($OutputPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
        try { $process.StandardOutput.BaseStream.CopyTo($stream) }
        finally { $stream.Dispose() }
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw "pg_dump failed with exit code $($process.ExitCode)." }
    } finally {
        $process.Dispose()
    }
}

$projectDirectory = [System.IO.Path]::GetFullPath((Join-Path $scriptDirectory '..'))
$BackupDirectory = [System.IO.Path]::GetFullPath($BackupDirectory)
New-Item -ItemType Directory -Force -Path $BackupDirectory | Out-Null
$timestamp = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
$backupPath = Join-Path $BackupDirectory "verifipay_$timestamp.dump"
$temporaryPath = "$backupPath.tmp.$PID"
$remoteCommand = ': "${POSTGRES_USER:?POSTGRES_USER is required}"; : "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"; : "${POSTGRES_DB:?POSTGRES_DB is required}"; exec env PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc'

Push-Location $projectDirectory
try {
    Invoke-DockerToFile -Arguments @('compose', 'exec', '-T', 'postgres', 'sh', '-ec', $remoteCommand) -OutputPath $temporaryPath
    Move-Item -LiteralPath $temporaryPath -Destination $backupPath
    Write-Output "Backup created: $backupPath"
} finally {
    if (Test-Path -LiteralPath $temporaryPath) { Remove-Item -LiteralPath $temporaryPath -Force }
    Pop-Location
}
