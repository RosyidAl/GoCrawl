@echo off
title GoCrawl Web Dashboard
echo ========================================================
echo        GoCrawl Academic Harvester Web Dashboard
echo ========================================================
echo Menjalankan server lokal di port 3000...
echo Buka di browser: http://localhost:3000
echo Tekan Ctrl+C untuk menghentikan server.
echo ========================================================
node web/server.mjs
pause
