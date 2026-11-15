@echo off
title GoCrawl Desktop

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

start javaw "-Djavax.net.ssl.trustStoreType=Windows-ROOT" -jar gocrawl.jar
