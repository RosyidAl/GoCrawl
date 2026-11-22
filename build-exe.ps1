$ErrorActionPreference = "Stop"

Write-Host "Building gocrawl.jar..." -ForegroundColor Cyan
& "$PSScriptRoot\build-jar.ps1"

$distDir = Join-Path $PSScriptRoot "dist"
$appDir = Join-Path $distDir "GoCrawl"
$inputDir = Join-Path $PSScriptRoot "target\jpackage-input"

if (Test-Path $appDir) {
    Remove-Item -Recurse -Force $appDir
}
if (Test-Path $inputDir) {
    Remove-Item -Recurse -Force $inputDir
}
New-Item -ItemType Directory -Force $distDir | Out-Null
New-Item -ItemType Directory -Force $inputDir | Out-Null

Copy-Item (Join-Path $PSScriptRoot "gocrawl.jar") (Join-Path $inputDir "gocrawl.jar")

Write-Host "Packaging GoCrawl.exe (app-image)..." -ForegroundColor Cyan
jpackage `
    --type app-image `
    --name GoCrawl `
    --input $inputDir `
    --main-jar gocrawl.jar `
    --main-class id.gocrawl.Main `
    --dest $distDir `
    --java-options "-Djavax.net.ssl.trustStoreType=Windows-ROOT" `
    --app-version "1.0.1" `
    --vendor "GoCrawl"

Write-Host "Copying data folder into dist/GoCrawl..." -ForegroundColor Cyan
Copy-Item -Recurse -Force (Join-Path $PSScriptRoot "data") (Join-Path $appDir "data")

Remove-Item -Recurse -Force $inputDir
Write-Host "Done! Aplikasi siap pakai di: $appDir\GoCrawl.exe" -ForegroundColor Green
