# Runs every scenario in scenarios.json through each model on the connected phone
# (llama.cpp in /data/local/tmp/tenvia-ai) and scores whether it picked the right guide.
# Usage: powershell -File ai-experiments\run-quality-test.ps1 [-Models a.gguf,b.gguf] [-Only 1]

param([string[]]$Models = @('Qwen3-0.6B-Q4_0.gguf', 'Qwen3-0.6B-Q8_0.gguf', 'Qwen3-1.7B-Q4_0.gguf', 'Qwen3-1.7B-Q8_0.gguf'), [int]$Only = 0)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$cfg = Get-Content (Join-Path $PSScriptRoot 'scenarios.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$tmp = Join-Path $env:TEMP 'tenvia-prompt.txt'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$results = @()

foreach ($model in $Models) {
    $right = 0; $n = 0
    foreach ($s in $cfg.scenarios) {
        if ($Only -and $s.id -ne $Only) { continue }
        # Qwen3 chat format; the empty think block turns off slow "thinking" mode.
        $prompt = "<|im_start|>system`n$($cfg.system)<|im_end|>`n<|im_start|>user`n$($s.text)<|im_end|>`n<|im_start|>assistant`n<think>`n`n</think>`n`n"
        [IO.File]::WriteAllText($tmp, $prompt, $utf8)
        & $adb push $tmp /data/local/tmp/tenvia-ai/prompt.txt | Out-Null
        $sw = [Diagnostics.Stopwatch]::StartNew()
        $out = & $adb shell "cd /data/local/tmp/tenvia-ai && LD_LIBRARY_PATH=bin bin/llama-completion -m models/$model -f prompt.txt -n 80 --temp 0 -t 4 -no-cnv --no-display-prompt 2>/dev/null"
        $sw.Stop()
        $text = (($out -join ' ') -replace '\[end of text\]', '').Trim()
        $json = $null; if ($text -match '\{.*?\}') { try { $json = $Matches[0] | ConvertFrom-Json } catch {} }
        $guide = if ($json) { "$($json.guide)" } else { '(no json)' }
        $med = if ($json) { [bool]$json.medical } else { $false }
        $ok = ($guide -eq $s.expect) -and ((-not $s.medical) -or $med)
        $n++; if ($ok) { $right++ }
        $results += [pscustomobject]@{ model = $model; id = $s.id; expect = $s.expect; got = $guide; medical = $med; ok = $ok; seconds = [math]::Round($sw.Elapsed.TotalSeconds, 1); say = if ($json) { $json.say } else { $text } }
    }
    "{0}: {1} of {2} correct" -f $model, $right, $n
}
$results | ConvertTo-Json -Depth 3 | Set-Content (Join-Path $PSScriptRoot 'quality-results.json') -Encoding UTF8
