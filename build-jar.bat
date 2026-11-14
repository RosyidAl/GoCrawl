@echo off
title GoCrawl Build
echo [1/3] Meng-compile source utama...
javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar;lib/flatlaf-3.8.jar" -d target/classes src\main\java\id\gocrawl\*.java src\main\java\id\gocrawl\gui\*.java
if errorlevel 1 (
    echo GAGAL compile. Periksa error di atas.
    pause
    exit /b 1
)

if exist target\classes\module-info.class del target\classes\module-info.class

echo [2/3] Memperbarui gocrawl.jar...
jar uf gocrawl.jar -C target/classes id
if errorlevel 1 (
    echo GAGAL update jar. Pastikan aplikasi GoCrawl tidak sedang berjalan.
    pause
    exit /b 1
)

echo [3/3] Selesai. gocrawl.jar sudah berisi versi terbaru.
pause
