param([switch]$ProductionWeb, [switch]$WithoutKafka, [switch]$LanWeb)
. "$PSScriptRoot/common.ps1"
Import-ProjectEnv
Initialize-Toolchain
if (-not (Test-Path (Join-Path $script:ProjectRoot 'web/node_modules'))) { & "$PSScriptRoot/web.ps1" -Action install }
& "$PSScriptRoot/start-infra.ps1" -WithoutKafka:$WithoutKafka
if (-not $env:KAFKA_ENABLED) { $env:KAFKA_ENABLED = if ($WithoutKafka) { 'false' } else { 'true' } }
$runDirectory = Join-Path $script:ProjectRoot '.run'
New-Item -ItemType Directory -Force $runDirectory | Out-Null
$lanIp = Get-LocalIPv4
if ($LanWeb) {
    $env:NEXT_PUBLIC_API_BASE_URL = "http://${lanIp}:8080"
    $env:CORS_ALLOWED_ORIGINS = "http://localhost:3000,http://${lanIp}:3000"
    if ($ProductionWeb) { & "$PSScriptRoot/web.ps1" -Action build }
}
function Start-LocalService {
    param([string]$Name, [int]$Port, [string]$Script, [string[]]$Extra = @())
    $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($listener) { Write-Output "Porta $Port já está em uso; serviço existente preservado."; return }
    $arguments = @('-NoProfile','-ExecutionPolicy','Bypass','-File',('"' + $Script + '"')) + @($Extra | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $process = Start-Process powershell.exe -ArgumentList $arguments -WorkingDirectory $script:ProjectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runDirectory "$Name.log") -RedirectStandardError (Join-Path $runDirectory "$Name.error.log")
    [PSCustomObject]@{ id=$process.Id; script=$Script; started=$process.StartTime.ToUniversalTime().ToString('o') } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runDirectory "$Name.json") -Encoding UTF8
    Write-Output "$Name iniciado (PID $($process.Id)). Log: .run/$Name.log"
}
Start-LocalService -Name backend -Port 8080 -Script (Join-Path $PSScriptRoot 'start-backend.ps1')
$extra = @()
if ($ProductionWeb) { $extra = @('-Production') }
Start-LocalService -Name web -Port 3000 -Script (Join-Path $PSScriptRoot 'start-web.ps1') -Extra $extra
function Wait-LocalService {
    param([string]$Name, [string]$Url)
    $deadline = [DateTime]::UtcNow.AddSeconds(120)
    while ([DateTime]::UtcNow -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
            if ($response.StatusCode -eq 200) { Write-Output "$Name pronto."; return }
        } catch { }
        Start-Sleep -Seconds 1
    }
    throw "$Name nao respondeu em $Url. Consulte .run/$Name.log e .run/$Name.error.log."
}
Wait-LocalService -Name backend -Url 'http://localhost:8080/users'
Wait-LocalService -Name web -Url 'http://localhost:3000'
Write-Output 'Web: http://localhost:3000'
if ($LanWeb) { Write-Output "Web na rede: http://${lanIp}:3000" }
Write-Output "API para Android: http://${lanIp}:8080"
Write-Output 'Servicos prontos. Logs em .run.'
