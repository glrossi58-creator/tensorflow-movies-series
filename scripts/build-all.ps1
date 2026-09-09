param([string]$ApiBaseUrl, [switch]$AppBundle)
. "$PSScriptRoot/common.ps1"
Initialize-Toolchain
Push-Location $script:ProjectRoot
try {
    Invoke-Checked .\gradlew.bat @('--no-daemon','clean','test')
    Invoke-Checked .\gradlew.bat @('--no-daemon','build')
    & "$PSScriptRoot/web.ps1" -Action install
    & "$PSScriptRoot/web.ps1" -Action lint
    & "$PSScriptRoot/web.ps1" -Action test
    & "$PSScriptRoot/web.ps1" -Action build
    & "$PSScriptRoot/build-android.ps1" -ApiBaseUrl $ApiBaseUrl -AppBundle:$AppBundle
} finally { Pop-Location }
