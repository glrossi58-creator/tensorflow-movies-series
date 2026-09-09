$ErrorActionPreference = 'Stop'
$script:ProjectRoot = Split-Path -Parent $PSScriptRoot

function Import-ProjectEnv {
    param([string]$File = (Join-Path $script:ProjectRoot '.env'))
    if (Test-Path -LiteralPath $File) {
        foreach ($line in Get-Content -LiteralPath $File -Encoding UTF8) {
            if ($line -match '^\s*([A-Z][A-Z0-9_]*)\s*=(.*)$') {
                $key = $matches[1]
                $value = $matches[2].Trim().Trim('"').Trim("'")
                if (-not [Environment]::GetEnvironmentVariable($key, 'Process')) {
                    [Environment]::SetEnvironmentVariable($key, $value, 'Process')
                }
            }
        }
    }
}

function Initialize-Toolchain {
    $flutterBin = Join-Path $script:ProjectRoot '.tools/flutter/bin'
    if (Test-Path -LiteralPath $flutterBin) { $env:PATH = "$flutterBin;$env:PATH" }
    $androidSdk = Join-Path $script:ProjectRoot '.tools/android-sdk'
    if (Test-Path -LiteralPath $androidSdk) { $env:ANDROID_HOME = $androidSdk; $env:ANDROID_SDK_ROOT = $androidSdk; $env:PATH = "$androidSdk/platform-tools;$env:PATH" }
    # NVM for Windows may expose a shim with no selected runtime. Prefer an actual installation.
    $nvmInstalls = Join-Path $env:LOCALAPPDATA 'Author Software/nvm/installs'
    if (Test-Path -LiteralPath $nvmInstalls) {
        $nodeVersion = Get-ChildItem -LiteralPath $nvmInstalls -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'node.exe') } | Sort-Object Name -Descending | Select-Object -First 1
        if ($nodeVersion) { $env:PATH = "$($nodeVersion.FullName);$env:PATH" }
    }
    $env:GRADLE_USER_HOME = Join-Path $script:ProjectRoot '.gradle-user'
    $env:NEXT_TELEMETRY_DISABLED = '1'
}

function Invoke-Checked {
    param([string]$Command, [string[]]$Arguments = @())
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Falha em $Command (exit $LASTEXITCODE)." }
}

function Get-LocalIPv4 {
    $route = Get-NetRoute -DestinationPrefix '0.0.0.0/0' -ErrorAction SilentlyContinue | Sort-Object RouteMetric | Select-Object -First 1
    if ($route) {
        $address = Get-NetIPAddress -InterfaceIndex $route.InterfaceIndex -AddressFamily IPv4 -ErrorAction SilentlyContinue | Where-Object { $_.IPAddress -notlike '169.254.*' } | Select-Object -First 1
        if ($address) { return $address.IPAddress }
    }
    throw 'Não foi possível identificar o IPv4 ativo. Use ipconfig e informe -ApiBaseUrl http://IP_DO_PC:8080.'
}
