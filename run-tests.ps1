# PowerShell Test Runner for GoCrawl
$ErrorActionPreference = "Stop"

Write-Host "Compiling GoCrawl main sources..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path target/classes | Out-Null
Remove-Item -Force -ErrorAction SilentlyContinue target/classes/module-info.class
$mainSources = (Get-ChildItem -Recurse src/main/java/*.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar;lib/flatlaf-3.8.jar" -d target/classes $mainSources

Write-Host "Compiling test sources..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path target/test-classes | Out-Null
$testSources = (Get-ChildItem -Recurse src/test/java/*.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar;lib/flatlaf-3.8.jar;target/classes" -d target/test-classes $testSources

Write-Host "Executing AllTests..." -ForegroundColor Green
java -cp "lib/jsoup-1.18.3.jar;lib/flatlaf-3.8.jar;target/classes;target/test-classes" id.gocrawl.AllTests
