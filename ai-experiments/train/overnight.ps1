# Overnight run for the small model (Qwen3 1.7B + LoRA). Start it before bed:
#   powershell -ExecutionPolicy Bypass -File overnight.ps1
# 1. train (low priority)  2. convert to GGUF f16  3. shrink to Q4_K_M
# 4. serve the new model on port 8082 and replay the SEEN dev2 conversations (chat-v6 -Short)
# It does NOT touch sealed set 4: that run happens once, after a person looks at the seen check.
# Keeps the PC awake while it runs. Everything is logged to C:\Users\aries\tenvia-train\overnight.log.

$ErrorActionPreference = 'Stop'
$root  = 'C:\Users\aries\tenvia-train'
$here  = Split-Path -Parent $MyInvocation.MyCommand.Path
$py    = "$root\.venv\Scripts\python.exe"
$llama = 'C:\Users\aries\Emberithm\tools\llama.cpp\b12302-cuda'
$name  = 'tenvia-1.7b-v1'
$hf    = "$root\$name"
$f16   = "$root\$name-f16.gguf"
$q4    = "$root\$name-Q4_K_M.gguf"
$log   = "$root\overnight.log"
$env:PYTHONIOENCODING = 'utf-8'; $env:PYTHONUNBUFFERED = '1'

function Say($t) {  # never stop the run just because something else is reading the log
    $line = (Get-Date -Format 'HH:mm:ss') + '  ' + $t
    for ($i = 0; $i -lt 10; $i++) { try { [IO.File]::AppendAllText($log, $line + "`r`n"); break } catch { Start-Sleep -Milliseconds 500 } }
    Write-Output $line
}

# Keep the PC from sleeping while this script runs (released automatically when it exits).
Add-Type -Namespace Win -Name Power -MemberDefinition '[DllImport("kernel32.dll")] public static extern uint SetThreadExecutionState(uint f);'
[void][Win.Power]::SetThreadExecutionState([uint32]'0x80000001')

function Run($exe, $argList, $out, $cwd) {
    $p = Start-Process -FilePath $exe -ArgumentList $argList -WorkingDirectory $cwd -RedirectStandardOutput $out -RedirectStandardError "$out.err" -WindowStyle Hidden -PassThru
    $null = $p.Handle  # without this, Windows PowerShell loses the exit code
    Start-Sleep 1; try { $p.PriorityClass = 'BelowNormal' } catch {}
    $p.WaitForExit(); return [int]$p.ExitCode
}

Say 'START overnight run'

# 1. Train
if (-not (Test-Path "$hf\model.safetensors") -and -not (Test-Path "$hf\model.safetensors.index.json")) {
    Say 'step 1: training (about 200 steps)'
    $code = Run $py @('train_lora.py', '--base', "$root/qwen3-1.7b", '--out', $hf, '--batch', '2', '--accum', '8', '--maxlen', '1536', '--lr', '1e-4', '--epochs', '2') "$root\train-v2.log" $here
    Get-Content "$root\train-v2.log" | Select-String 'val loss|STOPPED|saved' | ForEach-Object { Say ('  ' + $_.Line) }
    if ($code -ne 0) { Say "FAILED: training exited with code $code (see train-v2.log / train-v2.log.err)"; exit 1 }
} else { Say 'step 1: trained model already there, skipping' }

# 2-3. Convert and shrink
if (-not (Test-Path $q4)) {
    if (-not (Select-String -Path "$root\convert.log.err" -Pattern 'successfully exported' -Quiet -ErrorAction SilentlyContinue)) {
        Say 'step 2: converting to GGUF'
        $code = Run $py @("$root\llama-src\convert_hf_to_gguf.py", $hf, '--outtype', 'f16', '--outfile', $f16) "$root\convert.log" "$root\llama-src"
        if ($code -ne 0 -or -not (Test-Path $f16)) { Say "FAILED: conversion exited with code $code (see convert.log.err)"; exit 1 }
    } else { Say 'step 2: converted file already there, skipping' }
    Say 'step 3: shrinking to Q4_K_M'
    $code = Run "$llama\llama-quantize.exe" @($f16, $q4, 'Q4_K_M') "$root\quantize.log" $root
    if ($code -ne 0 -or -not (Test-Path $q4)) { Say "FAILED: quantize exited with code $code (see quantize.log.err)"; exit 1 }
    Say ('  model file: ' + $q4 + ' (' + [math]::Round((Get-Item $q4).Length / 1MB) + ' MB)')
} else { Say 'steps 2-3: model file already there, skipping' }

# 4. Seen-data check
Say 'step 4: starting the new model on port 8082'
$srv = Start-Process -FilePath "$llama\llama-server.exe" -ArgumentList @('-m', $q4, '-ngl', '99', '-c', '8192', '--parallel', '1', '--port', '8082', '--host', '127.0.0.1') -RedirectStandardOutput "$root\student-server.log" -RedirectStandardError "$root\student-server.err" -WindowStyle Hidden -PassThru
Start-Sleep 1; try { $srv.PriorityClass = 'BelowNormal' } catch {}
$ok = $false
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep 5
    try { if ((Invoke-RestMethod 'http://127.0.0.1:8082/health' -TimeoutSec 5).status -eq 'ok') { $ok = $true; break } } catch {}
}
if (-not $ok) { Say 'FAILED: the new model did not start (see student-server.err)'; Stop-Process -Id $srv.Id -Force -ErrorAction SilentlyContinue; exit 1 }
Say '  server ready; replaying seen conversations (dev2) with short prompts'
$code = Run 'powershell.exe' @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "$here\..\chat-v6.ps1", '-Url', 'http://127.0.0.1:8082', '-Short', '-Convs', 'chat-dev2.json', '-Out', 'train\s-chat-dev2') "$root\run-student-dev2.log" "$here\.."
Stop-Process -Id $srv.Id -Force -ErrorAction SilentlyContinue
if ($code -ne 0) { Say "FAILED: seen check exited with code $code (see run-student-dev2.log.err)"; exit 1 }
$turns = @(Get-Content "$root\run-student-dev2.log" | Select-String '^\[\d')
$gen = @($turns | Where-Object { $_.Line -match '\] generated' }).Count
Say "  seen check done: $gen of $($turns.Count) replies written by the model (rest were safe fallbacks)"
Say 'DONE. Next (with Claude): Hermes judges the seen check, then one run on sealed set 4.'
