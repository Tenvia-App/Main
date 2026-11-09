# Builds a per-turn judge file from a chat-v*.ps1 result file, for Hermes.
# Each item: the conversation so far, the new message, the notice on screen, the checked steps the
# model was given, and the reply.
# Usage: powershell -File ai-experiments\chat-judge-input.ps1 -In chat-dev2-2507.json -Out judge-input-chat-dev2-2507.json
param([Parameter(Mandatory)][string]$In, [Parameter(Mandatory)][string]$Out)
$here = $PSScriptRoot
$lib = Get-Content (Join-Path $here '..\android\app\src\main\assets\guides.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$gb = @{}; foreach ($g in $lib.guides) { $gb[$g.id] = $g }
$r = Get-Content "$here\$In" -Raw -Encoding UTF8 | ConvertFrom-Json
$items = @()
foreach ($c in $r) {
    $prev = @()
    foreach ($t in $c.turns) {
        $steps = @(); foreach ($id in ("$($t.active)" -split ',' | Where-Object { $_ })) { $steps += @($gb[$id].steps | ForEach-Object { $_.text }) }
        $items += [ordered]@{ id = "$($c.id).$($t.turn)"; earlier = @($prev); message = $t.user; notice_shown = $(if ($t.notice) { $t.notice } else { 'none' }); checked_steps = @($steps); reply = $t.reply }
        $prev += "PERSON: $($t.user)"; $prev += "APP: $($t.reply)"
    }
}
[IO.File]::WriteAllText("$here\$Out", (ConvertTo-Json @($items) -Depth 4), (New-Object Text.UTF8Encoding $false))
"items $($items.Count)"
