param([ValidateSet('get','analyze','test','devices')][string]$Action = 'analyze')
. "$PSScriptRoot/common.ps1"
Initialize-Toolchain
Push-Location (Join-Path $script:ProjectRoot 'mobile')
try {
    if ($Action -eq 'get') { Invoke-Checked flutter.bat @('pub','get') }
    else { Invoke-Checked flutter.bat @($Action) }
} finally { Pop-Location }
