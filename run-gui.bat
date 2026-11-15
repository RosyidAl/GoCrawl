@echo off
title GoCrawl Desktop

where javac >nul 2>nul
if %errorlevel% equ 0 if exist "src\main\java" (
    echo Menyiapkan versi terbaru GoCrawl...
    javac -encoding UTF-8 -cp "lib/jsoup-1.18.3.jar;lib/flatlaf-3.8.jar" -d target/classes src\main\java\id\gocrawl\*.java src\main\java\id\gocrawl\gui\*.java
    if errorlevel 1 (
        echo.
        echo GAGAL compile. Periksa pesan error di atas.
        pause
        exit /b 1
    )
    jar uf gocrawl.jar -C target/classes id
    if errorlevel 1 (
        echo.
        echo gocrawl.jar terkunci. Tutup dulu jendela GoCrawl yang lama, lalu jalankan lagi.
        pause
        exit /b 1
    )
)

where javaw >nul 2>nul
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Java tidak ditemukan di sistem ini.
    echo Pastikan Java 21 atau versi lebih baru sudah terpasang.
    echo Anda dapat mengunduh Java dari: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo Memulai GoCrawl Desktop GUI...
start javaw "-Djavax.net.ssl.trustStoreType=Windows-ROOT" -jar gocrawl.jar
