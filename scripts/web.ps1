param([ValidateSet('install','lint','test','build','dev','start','e2e')][string]$Action = 'dev', [string]$Hostname = '0.0.0.0')
. "$PSScriptRoot/common.ps1"
Initialize-Toolchain
Push-Location (Join-Path $script:ProjectRoot 'web')
try {
    switch ($Action) {
        'install' { Invoke-Checked npm.cmd @('install') }
        'dev' { Invoke-Checked npm.cmd @('run','dev','--','--hostname',$Hostname) }
        'start' { Invoke-Checked npm.cmd @('run','start','--','--hostname',$Hostname) }
        'e2e' { Invoke-Checked npm.cmd @('run','test:e2e') }
        default { Invoke-Checked npm.cmd @('run',$Action) }
    }
} finally { Pop-Location }
