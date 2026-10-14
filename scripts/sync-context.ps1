[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Import', 'Export')]
    [string]$Direction,
    [string]$OneDriveRoot = $env:OneDrive,
    [switch]$Overwrite
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$localContext = Join-Path $repositoryRoot '.collaboration'
$fileNames = @('AGENTS.md', 'design.md', 'development.md', 'handoff.md')

if ([string]::IsNullOrWhiteSpace($OneDriveRoot)) {
    throw 'OneDrive is not configured. Supply -OneDriveRoot with your existing OneDrive folder.'
}
$resolvedOneDrive = [System.IO.Path]::GetFullPath($OneDriveRoot)
if (-not (Test-Path -LiteralPath $resolvedOneDrive -PathType Container)) {
    throw "OneDrive root does not exist: $resolvedOneDrive. Finish OneDrive setup first."
}
$cloudContext = Join-Path $resolvedOneDrive 'BlocksideFoley-context'
$sourceFolder = if ($Direction -eq 'Export') { $localContext } else { $cloudContext }
$targetFolder = if ($Direction -eq 'Export') { $cloudContext } else { $localContext }

$changedFiles = @()
foreach ($fileName in $fileNames) {
    $sourceFile = Join-Path $sourceFolder $fileName
    $targetFile = Join-Path $targetFolder $fileName
    if (-not (Test-Path -LiteralPath $sourceFile -PathType Leaf)) {
        throw "Missing source file: $sourceFile. No files were copied."
    }
    if (Test-Path -LiteralPath $targetFile) {
        if (-not (Test-Path -LiteralPath $targetFile -PathType Leaf)) {
            throw "Target is not a regular file: $targetFile"
        }
        if ((Get-FileHash -LiteralPath $sourceFile).Hash -ne (Get-FileHash -LiteralPath $targetFile).Hash) {
            $changedFiles += $fileName
        }
    }
}
if ($changedFiles.Count -gt 0 -and -not $Overwrite) {
    throw ("Different target content: " + ($changedFiles -join ', ') +
        '. Review both versions and direction, then use -Overwrite. No files were copied.')
}

if ($changedFiles.Count -gt 0) {
    $backupName = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
    $backupFolder = Join-Path $repositoryRoot ('work/context-backups/' + $backupName)
    New-Item -ItemType Directory -Path $backupFolder -Force | Out-Null
    foreach ($fileName in $changedFiles) {
        Copy-Item -LiteralPath (Join-Path $targetFolder $fileName) -Destination (Join-Path $backupFolder $fileName)
    }
    Write-Output "Previous target files backed up to: $backupFolder"
}

New-Item -ItemType Directory -Path $targetFolder -Force | Out-Null
foreach ($fileName in $fileNames) {
    $sourceFile = Join-Path $sourceFolder $fileName
    $targetFile = Join-Path $targetFolder $fileName
    if (-not (Test-Path -LiteralPath $targetFile) -or
        (Get-FileHash -LiteralPath $sourceFile).Hash -ne (Get-FileHash -LiteralPath $targetFile).Hash) {
        Copy-Item -LiteralPath $sourceFile -Destination $targetFile -Force
    }
}

$entryFile = Join-Path $repositoryRoot 'AGENTS.md'
if ($Direction -eq 'Import' -and -not (Test-Path -LiteralPath $entryFile)) {
    $entryText = "# Local collaboration entry`n`nRead .collaboration/AGENTS.md and the other three context documents before working.`nPrivate context and this entry are ignored by Git.`n"
    [System.IO.File]::WriteAllText($entryFile, $entryText, [System.Text.UTF8Encoding]::new($false))
}
Write-Output "$Direction complete: four context files copied to $targetFolder"
Write-Output 'This confirms local file copies only. Wait for OneDrive to finish syncing before switching computers.'
