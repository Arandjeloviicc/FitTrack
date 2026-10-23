$ErrorActionPreference = "Stop"

# Project paths
$frontendDir = Split-Path -Parent $PSScriptRoot
$packagingDir = $PSScriptRoot
$inputDir = Join-Path $frontendDir "package-input"
$outputDir = Join-Path $frontendDir "dist"
$jarName = "fittrack-frontend-1.0.0.jar"
$jarPath = Join-Path $frontendDir "target\$jarName"
$iconPath = Join-Path $packagingDir "FitTrack.ico"

# Application metadata
$appName = "FitTrack"
$appVersion = "1.0.0"
$appVendor = "FitTrack"
$appDescription = "Desktop application for tracking nutrition, body measurements, and weight progress."


# Locate WiX Toolset
$wixBin = "C:\Program Files (x86)\WiX Toolset v3.14\bin"

if (-not (Get-Command candle -ErrorAction SilentlyContinue) -or
        -not (Get-Command light -ErrorAction SilentlyContinue)) {

    if (-not (Test-Path (Join-Path $wixBin "candle.exe")) -or
            -not (Test-Path (Join-Path $wixBin "light.exe"))) {
        throw "WiX Toolset not found. Install WiX Toolset 3.14."
    }

    $env:Path = "$wixBin;$env:Path"
}

# Verify required tools
foreach ($tool in @("jpackage", "java", "candle", "light")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "Required tool not found: $tool"
    }
}

# Verify assets
if (-not (Test-Path $iconPath)) {
    throw "Icon not found: $iconPath"
}

# Build frontend JAR
Push-Location $frontendDir

try {
    & ".\mvnw.cmd" clean package

    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed."
    }

    if (-not (Test-Path $jarPath)) {
        throw "Frontend JAR not found: $jarPath"
    }

    # Prepare clean package input
    if (Test-Path $inputDir) {
        Remove-Item $inputDir -Recurse -Force
    }

    New-Item -ItemType Directory -Path $inputDir | Out-Null
    Copy-Item $jarPath -Destination $inputDir

    # Prepare output directory
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

    $commonArgs = @(
        "--name", $appName,
        "--app-version", $appVersion,
        "--vendor", $appVendor,
        "--description", $appDescription,
        "--icon", $iconPath,
        "--input", $inputDir,
        "--main-jar", $jarName,
        "--main-class", "com.fittrack.app.MainLauncher",
        "--dest", $outputDir,
        "--win-per-user-install",
        "--win-dir-chooser",
        "--win-menu",
        "--win-shortcut",
        "--win-shortcut-prompt",
        "--java-options", "-Dfile.encoding=UTF-8"
    )

    foreach ($type in @("exe", "msi")) {
        Write-Host "Building $type installer..."

        $installerPath = Join-Path $outputDir "$appName-$appVersion.$type"

        if (Test-Path $installerPath) {
            Remove-Item $installerPath -Force
        }

        & jpackage --type $type @commonArgs

        if ($LASTEXITCODE -ne 0) {
            throw "Failed to create $type installer."
        }

        if (-not (Test-Path $installerPath)) {
            throw "Expected installer not found: $installerPath"
        }
    }

    Write-Host ""
    Write-Host "FitTrack installers created successfully:"
    Write-Host (Join-Path $outputDir "$appName-$appVersion.exe")
    Write-Host (Join-Path $outputDir "$appName-$appVersion.msi")
}
finally {
    Pop-Location
}