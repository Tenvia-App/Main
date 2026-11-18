# One run of a frozen model on sealed set 4 (chat-sealed4.json). Run once per model, never to tune.
#   powershell -ExecutionPolicy Bypass -File sealed-run.ps1 -Which small   (1.7B student, short prompts, chat-v6 -Short)
#   powershell -ExecutionPolicy Bypass -File sealed-run.ps1 -Which big     (4B phone model, full prompts, chat-v5)
param([ValidateSet('small', 'big')][string]$Which = 'small')
$root  = 'C:\Users\aries\tenvia-train'
$here  = Split-Path -Parent $MyInvocation.MyCommand.Path
$llama = 'C:\Users\aries\Emberithm\tools\llama.cpp\b12302-cuda'
if ($Which -eq 'small') {
    $model = "$root\tenvia-1.7b-v1-Q4_K_M.gguf"; $script = 'chat-v6.ps1'; $extra = @('-Short'); $out = 'chat-sealed4-small1'
} else {
    $model = 'C:\Users\aries\tenvia-models\Qwen3-4B-Instruct-2507-Q4_K_M.gguf'; $script = 'chat-v5.ps1'; $extra = @(); $out = 'chat-sealed4-4b'
}
if (-not (Test-Path $model)) { "FAILED: no model file at $model"; exit 1 }
$srv = Start-Process -FilePath "$llama\llama-server.exe" -ArgumentList @('-m', $model, '-ngl', '99', '-c', '8192', '--parallel', '1', '--port', '8082', '--host', '127.0.0.1') -RedirectStandardOutput "$root\sealed-server.log" -RedirectStandardError "$root\sealed-server.err" -WindowStyle Hidden -PassThru
Start-Sleep 1; try { $srv.PriorityClass = 'BelowNormal' } catch {}
$ok = $false
for ($i = 0; $i -lt 60; $i++) { Start-Sleep 5; try { if ((Invoke-RestMethod 'http://127.0.0.1:8082/health' -TimeoutSec 5).status -eq 'ok') { $ok = $true; break } } catch {} }
if (-not $ok) { Stop-Process -Id $srv.Id -Force -ErrorAction SilentlyContinue; 'FAILED: model server did not start'; exit 1 }
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$here\..\$script" -Url 'http://127.0.0.1:8082' @extra -Convs 'chat-sealed4.json' -Out $out > "$root\run-$out.log" 2> "$root\run-$out.err"
Stop-Process -Id $srv.Id -Force -ErrorAction SilentlyContinue
"DONE $out"
