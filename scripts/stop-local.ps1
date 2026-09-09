param([switch]$StopInfra)
. "$PSScriptRoot/common.ps1"
$runDirectory = Join-Path $script:ProjectRoot '.run'
foreach ($name in @('web','backend')) {
    $record = Join-Path $runDirectory "$name.json"
    if (-not (Test-Path -LiteralPath $record)) { continue }
    $info = Get-Content -LiteralPath $record -Raw | ConvertFrom-Json
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($info.id)" -ErrorAction SilentlyContinue
    if (-not $process) { continue }
    if (-not $process.CommandLine.Contains($info.script)) { throw "PID $($info.id) pertence a outro processo. Nada foi encerrado." }
    $owned = [Collections.Generic.List[int]]::new()
    function Add-Children([int]$parent) {
        foreach ($child in Get-CimInstance Win32_Process -Filter "ParentProcessId=$parent" -ErrorAction SilentlyContinue) { Add-Children $child.ProcessId; $owned.Add($child.ProcessId) }
    }
    Add-Children $info.id
    foreach ($processId in $owned) { Stop-Process -Id $processId -ErrorAction SilentlyContinue }
    Stop-Process -Id $info.id -ErrorAction SilentlyContinue
    Write-Output "$name encerrado."
}
if ($StopInfra) {
    Push-Location $script:ProjectRoot
    try { Invoke-Checked docker @('compose','stop') } finally { Pop-Location }
}
