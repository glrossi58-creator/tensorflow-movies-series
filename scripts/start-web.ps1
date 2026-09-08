param([switch]$Production, [string]$Hostname = '0.0.0.0')
if ($Production) { & "$PSScriptRoot/web.ps1" -Action start -Hostname $Hostname }
else { & "$PSScriptRoot/web.ps1" -Action dev -Hostname $Hostname }
