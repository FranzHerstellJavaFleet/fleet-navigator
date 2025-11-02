# Fleet Navigator - GraalVM Native Image Build (Windows)
# ======================================================

Write-Host "🚀 Fleet Navigator - GraalVM Native Image Build" -ForegroundColor Cyan
Write-Host "================================================`n" -ForegroundColor Cyan

# Visual Studio Environment laden
Write-Host "🔧 Loading Visual Studio environment..." -ForegroundColor Yellow
$vsPath = & "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe" `
    -latest -property installationPath

if ($vsPath) {
    Push-Location "$vsPath\Common7\Tools"
    cmd /c "VsDevCmd.bat&set" |
    ForEach-Object {
        if ($_ -match "=") {
            $v = $_.split("=", 2)
            Set-Item -Force -Path "ENV:\$($v[0])" -Value "$($v[1])"
        }
    }
    Pop-Location
    Write-Host "✅ Visual Studio environment loaded`n" -ForegroundColor Green
} else {
    Write-Host "❌ Visual Studio 2022 not found!" -ForegroundColor Red
    Write-Host "   Please install Visual Studio 2022 with C++ Build Tools" -ForegroundColor Red
    exit 1
}

# GraalVM Version prüfen
Write-Host "📦 GraalVM Version:" -ForegroundColor Yellow
try {
    java -version 2>&1 | Write-Host
    $nativeImageExists = Get-Command native-image -ErrorAction SilentlyContinue
    if (-not $nativeImageExists) {
        Write-Host "❌ native-image not found! Run: gu install native-image" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
} catch {
    Write-Host "❌ Java/GraalVM not found!" -ForegroundColor Red
    exit 1
}

# Build Native Image
Write-Host "`n🎯 Building Native Image with integrated frontend..." -ForegroundColor Yellow
Write-Host "   This may take 10-15 minutes depending on your system...`n" -ForegroundColor Gray

$startTime = Get-Date
mvn -Pnative clean package -DskipTests
$exitCode = $LASTEXITCODE
$endTime = Get-Date
$duration = $endTime - $startTime

if ($exitCode -eq 0) {
    Write-Host "`n✅ Build Complete!" -ForegroundColor Green
    Write-Host "`n📦 Native executable created:" -ForegroundColor Cyan
    if (Test-Path "target\fleet-navigator.exe") {
        $fileInfo = Get-Item "target\fleet-navigator.exe"
        Write-Host "   Location: $($fileInfo.FullName)" -ForegroundColor White
        Write-Host "   Size: $([math]::Round($fileInfo.Length / 1MB, 2)) MB" -ForegroundColor White
    }

    Write-Host "`n⏱️  Build time: $($duration.ToString('mm\:ss'))" -ForegroundColor Cyan

    Write-Host "`n🚀 To run the native image:" -ForegroundColor Yellow
    Write-Host "   .\target\fleet-navigator.exe" -ForegroundColor White

    Write-Host "`n📊 Performance comparison:" -ForegroundColor Cyan
    Write-Host "   Startup:" -ForegroundColor White
    Write-Host "     JVM:    ~3-5 seconds" -ForegroundColor Gray
    Write-Host "     Native: ~0.1-0.5 seconds (10-50x faster!)" -ForegroundColor Green
    Write-Host "   Memory:" -ForegroundColor White
    Write-Host "     JVM:    ~300-500 MB" -ForegroundColor Gray
    Write-Host "     Native: ~50-100 MB (5-10x less!)" -ForegroundColor Green
} else {
    Write-Host "`n❌ Build failed!" -ForegroundColor Red
    Write-Host "   Check the output above for errors" -ForegroundColor Red
    exit 1
}
