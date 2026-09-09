param([switch]$WithoutKafka)
. "$PSScriptRoot/common.ps1"
Import-ProjectEnv
Push-Location $script:ProjectRoot
try {
    Invoke-Checked docker @('info','--format','{{.ServerVersion}}')
    $services = @('compose','up','-d','--wait','postgres')
    if (-not $WithoutKafka) { $services += 'kafka' }
    Invoke-Checked docker $services
} finally { Pop-Location }
