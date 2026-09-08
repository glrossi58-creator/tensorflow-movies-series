param([string]$ApiBaseUrl, [switch]$AppBundle, [switch]$SkipChecks)
. "$PSScriptRoot/common.ps1"
Initialize-Toolchain
if (-not $ApiBaseUrl) { $ApiBaseUrl = "http://$(Get-LocalIPv4):8080" }
$uri = [Uri]$ApiBaseUrl
if (-not $uri.IsAbsoluteUri -or $uri.Scheme -notin @('http','https')) { throw 'ApiBaseUrl deve ser uma URL HTTP ou HTTPS absoluta.' }
$defines = @("--dart-define=API_BASE_URL=$ApiBaseUrl")
if ($uri.Scheme -eq 'http') { $defines += '--dart-define=ALLOW_LOCAL_HTTP=true' }
Push-Location (Join-Path $script:ProjectRoot 'mobile')
try {
    Invoke-Checked flutter.bat @('pub','get')
    if (-not $SkipChecks) { Invoke-Checked flutter.bat @('analyze'); Invoke-Checked flutter.bat @('test') }
    Invoke-Checked flutter.bat (@('build','apk','--release') + $defines)
    Write-Output "APK: $(Join-Path (Get-Location) 'build/app/outputs/flutter-apk/app-release.apk')"
    Write-Output "API compilada: $ApiBaseUrl"
    if ($AppBundle) {
        Invoke-Checked flutter.bat (@('build','appbundle','--release') + $defines)
        Write-Output "AAB: $(Join-Path (Get-Location) 'build/app/outputs/bundle/release/app-release.aab')"
    }
} finally { Pop-Location }
