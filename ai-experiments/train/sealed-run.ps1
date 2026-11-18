# One run of a frozen model on a sealed set. Run once per model, never to tune.
#   powershell -ExecutionPolicy Bypass -File sealed-run.ps1 -Which small -Convs chat-sealed5.json -Res chat-sealed5-small1
#   powershell -ExecutionPolicy Bypass -File sealed-run.ps1 -Which big   -Convs chat-sealed5.json -Res chat-sealed5-4b
#   -Which small = a student model (short prompts, chat-v6 -Short); big = the 4B phone model (full prompts, chat-v5)
#   -ModelFile: another model file; -Convs: conversation set (relative to ai-experiments); -Res: result name
# (PowerShell names are case-insensitive, so script variables must not share a name with a parameter.)
param([ValidateSet('small', 'big')][string]$Which = 'small', [string]$ModelFile = '', [string]$Convs = 'chat-sealed4.json', [string]$Res = '')
$root  = 'C:\Users\aries\tenvia-train'
$here  = Split-Path -Parent $MyInvocation.MyCommand.Path
$llama = 'C:\Users\aries\Emberithm\tools\llama.cpp\b12302-cuda'
if ($Which -eq 'small') {
    $gguf = "$root\tenvia-1.7b-v1-Q4_K_M.gguf"; $script = 'chat-v6.ps1'; $extra = @('-Short'); $name = 'chat-sealed4-small1'
} else {
    $gguf = 'C:\Users\aries\tenvia-models\Qwen3-4B-Instruct-2507-Q4_K_M.gguf'; $script = 'chat-v5.ps1'; $extra = @(); $name = 'chat-sealed4-4b'
}
if ($ModelFile) { $gguf = $ModelFile }
if ($Res) { $name = $Res }
if (Test-Path "$here\..\$name.json") { "FAILED: $name.json already exists (would resume into it)"; exit 1 }
if (-not (Test-Path $gguf)) { "FAILED: no model file at $gguf"; exit 1 }
$srv = Start-Process -FilePath "$llama\llama-server.exe" -ArgumentList @('-m', $gguf, '-ngl', '99', '-c', '8192', '--parallel', '1', '--port', '8082', '--host', '127.0.0.1') -RedirectStandardOutput "$root\sealed-server.log" -RedirectStandardError "$root\sealed-server.err" -WindowStyle Hidden -PassThru
Start-Sleep 1; try { $srv.PriorityClass = 'BelowNormal' } catch {}
$ok = $false
for ($i = 0; $i -lt 60; $i++) { Start-Sleep 5; try { if ((Invoke-RestMethod 'http://127.0.0.1:8082/health' -TimeoutSec 5).status -eq 'ok') { $ok = $true; break } } catch {} }
if (-not $ok) { Stop-Process -Id $srv.Id -Force -ErrorAction SilentlyContinue; 'FAILED: model server did not start'; exit 1 }
$logName = $name -replace '[\\/]', '-'
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$here\..\$script" -Url 'http://127.0.0.1:8082' @extra -Convs $Convs -Out $name > "$root\run-$logName.log" 2> "$root\run-$logName.err"
Stop-Process -Id $srv.Id -Force -ErrorAction SilentlyContinue
"DONE $name"
