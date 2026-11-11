Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "       GoCrawl Academic Harvester Web Dashboard" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Memulai server lokal pada http://localhost:3000 ..." -ForegroundColor Yellow
Write-Host "Portabel untuk diakses melalui browser desktop & mobile." -ForegroundColor Gray
Write-Host "Tekan Ctrl+C untuk menghentikan server." -ForegroundColor DarkGray
Write-Host "========================================================" -ForegroundColor Cyan
node web/server.mjs
