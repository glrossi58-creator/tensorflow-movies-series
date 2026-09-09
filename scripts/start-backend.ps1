. "$PSScriptRoot/common.ps1"
Import-ProjectEnv
Initialize-Toolchain
if (-not $env:TMDB_READ_ACCESS_TOKEN) { Write-Warning 'TMDB não configurado. Busca local e avaliações funcionam; defina TMDB_READ_ACCESS_TOKEN no .env para importar e descobrir novos títulos.' }
Push-Location $script:ProjectRoot
try { Invoke-Checked .\gradlew.bat @('--no-daemon','bootRun') } finally { Pop-Location }
