#requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$BackupPath,
    [switch]$Yes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDirectory = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($scriptDirectory)) {
    throw 'Could not resolve the script directory. Run this script with powershell -File.'
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

function Invoke-DockerWithInput {
    param([string[]]$Arguments, [string]$InputPath)

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo.FileName = 'docker'
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.RedirectStandardInput = $true
    $process.StartInfo.Arguments = (($Arguments | ForEach-Object { ConvertTo-WindowsCommandLineArgument $_ }) -join ' ')

    if (-not $process.Start()) { throw 'Could not start Docker.' }
    try {
        $stream = [System.IO.File]::OpenRead($InputPath)
        try { $stream.CopyTo($process.StandardInput.BaseStream) }
        finally {
            $stream.Dispose()
            $process.StandardInput.Close()
        }
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw "pg_restore failed with exit code $($process.ExitCode)." }
    } finally {
        $process.Dispose()
    }
}

if (-not (Test-Path -LiteralPath $BackupPath -PathType Leaf)) {
    throw "Backup file does not exist: $BackupPath"
}

$projectDirectory = [System.IO.Path]::GetFullPath((Join-Path $scriptDirectory '..'))
$resolvedBackupPath = (Resolve-Path -LiteralPath $BackupPath).Path
$restoreCommand = ': "${POSTGRES_USER:?POSTGRES_USER is required}"; : "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"; : "${POSTGRES_DB:?POSTGRES_DB is required}"; exec env PGPASSWORD="$POSTGRES_PASSWORD" pg_restore --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

Push-Location $projectDirectory
try {
    Invoke-DockerWithInput -Arguments @('compose', 'exec', '-T', 'postgres', 'pg_restore', '--list') -InputPath $resolvedBackupPath
    if (-not $Yes) {
        $answer = Read-Host "Restore $resolvedBackupPath into the existing PostgreSQL database? This replaces existing objects. Type YES to continue"
        if ($answer -cne 'YES') {
            Write-Output 'Restore cancelled.'
            return
        }
    }

    Invoke-DockerWithInput -Arguments @('compose', 'exec', '-T', 'postgres', 'sh', '-ec', $restoreCommand) -InputPath $resolvedBackupPath
    Write-Output "Restore completed from: $resolvedBackupPath"
} finally {
    Pop-Location
}
