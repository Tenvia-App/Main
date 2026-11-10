# Starts the Tenvia AI test server (llama-server, Qwen3-4B-Instruct-2507) ON THE PHONE, detached from USB,
# so the app's "Talk to Tenvia (AI test)" screen keeps working after the cable is unplugged.
# Needed again after every phone restart. Run with the phone plugged in:
#   powershell -ExecutionPolicy Bypass -File ai-experiments\start-phone-ai.ps1
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb wait-for-device
$running = & $adb shell "ps -A -o NAME | grep -c llama-server"
if ([int]$running -gt 0) { 'The AI server is already running on the phone.'; exit 0 }
# The job is started in the background and the adb call is ended after a few seconds (adb waits otherwise).
$job = Start-Job -ScriptBlock { param($a) & $a shell 'cd /data/local/tmp/tenvia-ai/bin && LD_LIBRARY_PATH=. setsid nohup ./llama-server -m ../models/Qwen3-4B-Instruct-2507-Q4_K_M.gguf --host 127.0.0.1 --port 8080 -c 4096 -t 4 > ../server.log 2>&1 < /dev/null &' } -ArgumentList $adb
Start-Sleep -Seconds 5
Stop-Job $job; Remove-Job $job -Force
foreach ($i in 1..30) {
    $log = & $adb shell "tail -1 /data/local/tmp/tenvia-ai/server.log"
    if ($log -match 'listening on') { 'The AI server is running on the phone. You can unplug it now.'; exit 0 }
    Start-Sleep -Seconds 2
}
'The server did not report ready within a minute. Last log line:'
$log
