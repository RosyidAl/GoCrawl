$ErrorActionPreference = "Stop"

Write-Host "Compiling main sources..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force target/classes | Out-Null
Remove-Item -Force -ErrorAction SilentlyContinue target/classes/module-info.class

$srcFiles = (Get-ChildItem -Recurse -Filter *.java src/main/java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar;lib/flatlaf-3.8.jar" -d target/classes $srcFiles

Write-Host "Unpacking libraries..." -ForegroundColor Cyan
Push-Location target/classes
jar -xf ../../lib/jsoup-1.18.3.jar
jar -xf ../../lib/flatlaf-3.8.jar
Pop-Location

Write-Host "Packaging gocrawl.jar..." -ForegroundColor Cyan
jar --create --file gocrawl.jar --main-class id.gocrawl.Main -C target/classes .

Write-Host "gocrawl.jar successfully built!" -ForegroundColor Green
