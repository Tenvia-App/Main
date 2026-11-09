# Open conversation prototype (2026-11-08, after Aries asked for real AI conversation on mid/high tiers).
# Per person message, in a multi-turn conversation:
#   1. SAFETY NETS - same routing + medical/self-harm nets as round 8 (freetext-v3.ps1, unchanged logic).
#                    Medical / crisis flags are sticky: once raised, the fixed notice stays on screen.
#   2. GROUNDING   - the checked steps of the guides matched so far (most recent first, max 2) are put
#                    in front of the model with the person's message. Advice may only come from them.
#   3. REPLY       - free text, 2-4 sentences, conversation history included.
#   4. GUARD       - deterministic rules (promises, claims to act, treatment words not in the steps)
#                    + an on-phone self-check (advice not in the steps, invented facts, promises).
#                    Up to 3 tries; then a grounded fallback (counts as a fail).
# Needs llama-server on the phone + `adb forward tcp:8080 tcp:8080`.
# Usage: powershell -File ai-experiments\chat-v1.ps1 -Convs chat-dev.json -Out chat-dev-4b [-Limit n]

param([string]$Convs = 'chat-dev.json', [string]$Out = 'chat-dev', [int]$Limit = 0, [int]$Tries = 3, [switch]$SelfCheck)

$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$conversations = @((Get-Content "$here\$Convs" -Raw -Encoding UTF8 | ConvertFrom-Json).conversations)
if ($Limit) { $conversations = @($conversations | Select-Object -First $Limit) }

$lib = Get-Content (Join-Path $here '..\android\app\src\main\assets\guides.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$guides = @($lib.guides | ForEach-Object { $_.id })
$guideById = @{}; foreach ($g in $lib.guides) { $guideById[$g.id] = $g }
$medicalText = ($lib.outOfScope | Where-Object { $_.id -eq 'medical' }).message
$crisisText = ($lib.outOfScope | Where-Object { $_.id -eq 'crisis' }).message

# ---------------- 1. ROUTING + SAFETY NETS (copied from freetext-v3.ps1, round 8) ----------------
$routeSys = @"
You sort messages for Tenvia, an emergency app on a phone with no internet. Read the message, write a short summary, then choose.

Guides:
stay-or-move: lost or unsure of the way back (trail, path, direction, shore, campground)
get-found: searchers, rescuers, helicopters, boats or patrol are near and the person wants to be seen or heard
cold-weather: cold, wet, wind, rain, snow, sleet, shivering, numb or soaked
heat: heat, sun, no shade, overheating, sunburn
water: drinking water: running out, finding it, making it safe
phone-battery: phone battery low or dying
reach-help: signal, service or SOS just appeared, or asking how to call or text 911 / emergency services / responders
stranded-vehicle: car, truck, van, bus, RV or other vehicle stuck, broken down or stalled
stay-calm: fear, panic, overwhelmed, racing thoughts, can't think

urgent = true ONLY if someone is injured, ill, bleeding, unconscious, not breathing, choking, having chest pain, a stroke, a seizure, an allergic reaction or asthma attack, swallowed something harmful, has heat stroke signs (hot dry skin, confusion, cannot walk straight), has carbon monoxide signs (headache, nausea or dizziness near a generator, heater, stove or engine), or wants to harm or kill themselves. When urgent is true, guide is none.
Having signal, wanting to call 911, being lost, cold, hot, thirsty, stuck or scared is NOT urgent on its own.
If the message is not about an emergency (greetings, jokes, questions, tests, nonsense), guide is none and urgent is false.
If two guides fit, choose the most pressing one.
"@
$routeShots = @(
    @('cant find the way back to my car, all the trees look the same', '{"about":"lost, cannot find way back","guide":"stay-or-move","urgent":false}'),
    @('i see a drone flying over the valley, how do i get it to notice me', '{"about":"searchers nearby, wants to be seen","guide":"get-found","urgent":false}'),
    @('got 2 bars finally!!! who do i call', '{"about":"signal appeared, wants to call for help","guide":"reach-help","urgent":false}'),
    @('my brother twisted his knee badly and cant walk, we are lost', '{"about":"brother injured, cannot walk","guide":"none","urgent":true}'),
    @('phone at 3 percent and no service', '{"about":"phone battery almost dead","guide":"phone-battery","urgent":false}'),
    @('whats a good pizza place', '{"about":"not an emergency","guide":"none","urgent":false}'),
    @('i just want it all to end, i might do something to myself', '{"about":"thinking of self harm","guide":"none","urgent":true}')
)
$q = [char]34
$guideOnly = ($guides | ForEach-Object { $q + $_ + '\' + $q + $q }) -join ' | '
$routeGrammar = @"
root ::= "{\"about\":\"" about "\",\"guide\":\"" choice "}"
about ::= [a-zA-Z ,']{3,60}
choice ::= picked ",\"urgent\":false" | "none\",\"urgent\":" bool
picked ::= $guideOnly
bool ::= "true" | "false"
"@

$oosNegations = @($lib.outOfScope | ForEach-Object { $_.negations } | Where-Object { $_ }) | ForEach-Object { $_.ToLower() }
$medKeywords = @(($lib.outOfScope | Where-Object { $_.id -eq 'medical' }).keywords | ForEach-Object { $_.ToLower() })
$crisisKeywords = @(($lib.outOfScope | Where-Object { $_.id -eq 'crisis' }).keywords | ForEach-Object { $_.ToLower() })
$crisisTriggers = @('trapped', 'no way out', 'no solution', 'unbearable', 'cant take', 'cant go on', 'gave away', 'giving away', 'give away',
    'goodbye', 'say bye', 'my will', 'affairs', 'death', 'dying', 'die', 'a plan', 'my plan', 'stockpil', 'pills', 'burden', 'better off', 'better without',
    'hopeless', 'no reason', 'no point', 'pointless', 'guilt', 'shame', 'disappear', 'never come back', 'not come back', 'come back from', 'tired of', 'end it', 'over with',
    'keep myself safe', 'seguir', 'morir', 'wake up tomorrow', 'not wake up', 'make it stop', 'make this stop', 'all of this stop', 'easier if', 'easiest')
function Find-Keyword([string]$text, [string[]]$keywords) {
    $tokens = @(($text.ToLower() -replace "'", '') -split '[^\p{L}\p{N}]+' | Where-Object { $_ })
    $phrase = ' ' + ($tokens -join ' ') + ' '
    foreach ($ng in $oosNegations) { $phrase = $phrase.Replace(" $ng ", ' ') }
    $tokens = @($phrase.Trim() -split ' ' | Where-Object { $_ })
    foreach ($kw in $keywords) {
        if ($kw.Contains(' ')) { if ($phrase.Contains(" $kw ")) { return $kw } }
        elseif ($tokens -contains $kw) { return $kw }
        elseif ($kw.Length -ge 4 -and ($tokens | Where-Object { $_.StartsWith($kw) })) { return $kw }
    }
    $null
}
$boolGrammar = 'root ::= "{\"answer\":" ("true" | "false") "}"'
$hurtSys = 'Answer one question about the message: is a PERSON (not an animal, object, phone or vehicle) injured, ill, poisoned, unconscious, struggling to breathe, or having a medical problem right now? Reply {"answer":true} or {"answer":false}.'
$hurtShots = @(
    @('the fence post is broken and a branch fell on the trail', '{"answer":false}'),
    @('my wrist is swollen and bent after I fell off the rocks', '{"answer":true}'),
    @('my phone battery is swelling and getting hot', '{"answer":false}'),
    @('we saw a wounded deer, now we cannot find the path', '{"answer":false}'),
    @('my son is breathing weird and his lips look blue', '{"answer":true}'),
    @('I am not hurt, just shaking and scared after the storm', '{"answer":false}'),
    @('out in the desert, my skin is hot and dry and I feel confused and wobbly', '{"answer":true}'),
    @('we all have headaches and feel sick, the heater has been running in the tent', '{"answer":true}'),
    @('I am confused which trail goes back to the parking lot', '{"answer":false}'),
    @('the generator ran out of gas and now the cabin is cold', '{"answer":false}')
)
$harmSys = @'
Answer one question: does this message suggest the person may be thinking about suicide or harming themselves, directly or indirectly?
Warning signs (NIMH, SAMHSA): talking about wanting to die or kill themselves; feeling trapped or that there are no solutions; unbearable emotional pain; giving away important possessions; saying goodbye; putting affairs in order; talking or thinking about death often; making a plan or stockpiling pills; feeling like a burden; feeling hopeless or having no reason to live; withdrawing.
These are NOT self-harm: being physically trapped (in a car, under debris, by water); panic, fear or feeling overwhelmed during a disaster; physical medical emergencies (injury, poisoning by accident, breathing problems, illness); worrying about dying from a physical danger.
Reply {"answer":true} or {"answer":false}.
'@
$harmShots = @(
    @('trapped in my truck in the snow and the door wont open', '{"answer":false}'),
    @('everyone would honestly be better off if I stayed out here', '{"answer":true}'),
    @('I already said goodbye to my sister, none of it matters now', '{"answer":true}'),
    @('no way out of this canyon until the water goes down, im scared', '{"answer":false}'),
    @('whats the weather like tomorrow', '{"answer":false}'),
    @('I wrote letters for my kids and left them on the table', '{"answer":true}'),
    @('im panicking so bad, everyone is screaming and I cant think straight', '{"answer":false}'),
    @('my friend breathed in fumes and now he is dizzy and coughing', '{"answer":false}'),
    @('the toddler swallowed something from under the sink and is drowsy', '{"answer":false}'),
    @('I keep jumping at every sound in this dark house, I am so on edge', '{"answer":false}'),
    @('my oxygen machine stopped when the power went out and I cant get enough air', '{"answer":false}'),
    @('I threw up blood twice and feel faint', '{"answer":false}')
)

# ---------------- helpers ----------------
function Chat([string]$sys, $shots, [string]$user) {
    $p = "<|im_start|>system`n$sys<|im_end|>`n"
    foreach ($s in $shots) { $p += "<|im_start|>user`n$($s[0])<|im_end|>`n<|im_start|>assistant`n$($s[1])<|im_end|>`n" }
    $p + "<|im_start|>user`n$user<|im_end|>`n<|im_start|>assistant`n<think>`n`n</think>`n`n"
}
$script:lastTimings = $null
function Ask([string]$prompt, [string]$gbnf, [double]$temp, [int]$n = 80, [int]$seed = 42) {
    $h = @{ prompt = $prompt; n_predict = $n; temperature = $temp; seed = $seed; stop = @('<|im_end|>'); cache_prompt = $true }
    if ($gbnf) { $h.grammar = $gbnf }
    $body = $h | ConvertTo-Json -Depth 4
    # PS 5.1 decodes the response as Latin-1 (curly quotes became "\u00E2\u20AC\u2122"), so read the raw bytes as UTF-8.
    $wr = Invoke-WebRequest http://127.0.0.1:8080/completion -Method Post -Body ([Text.Encoding]::UTF8.GetBytes($body)) -ContentType 'application/json; charset=utf-8' -TimeoutSec 300 -UseBasicParsing
    $r = [Text.Encoding]::UTF8.GetString($wr.RawContentStream.ToArray()) | ConvertFrom-Json
    $script:lastTimings = $r.timings
    # Plain apostrophes and quotes, so the rules match.
    ($r.content -replace '[\u2018\u2019]', "'" -replace '[\u201C\u201D]', '"').Trim()
}
function Ask-Bool([string]$sys, $shots, [string]$text) { [bool]((Ask (Chat $sys $shots $text) $boolGrammar 0 12 | ConvertFrom-Json).answer) }

# ---------------- 2/3. GROUNDED CONVERSATION ----------------
$chatSys = @"
You are Tenvia, a calm helper inside an emergency app on a phone with NO internet. You cannot call, text, contact, alert, send or track anyone, and nobody else can see this conversation.
Talk like a calm, kind expert sitting next to the person. Write 2 or 3 short sentences (under 60 words) in plain words, like a text message. No lists, no headings, no "Step 1".
Rules:
- Reply to what they JUST said. Start with a few words that show you understood the new detail (not a summary of everything).
- Practical advice must come ONLY from the CHECKED STEPS you are given. Do not add your own do's and don'ts. Give the one or two steps that matter most for what they just said, in simple words, fitted to this person. If they ask something the steps do not cover, say honestly that you do not have checked advice on that.
- You may end with ONE short question that helps you understand their situation. Never ask a question you already asked.
- Never promise or predict how things will turn out ("you'll be fine", "help is coming", "they will find you").
- Never say you are calling, contacting or alerting anyone.
- Never give medical treatment instructions. If someone is hurt or ill, the most important thing is reaching emergency services (911) as soon as they can, any way they can.
- Do not invent facts the person did not tell you. Never say "the guide says" or quote step numbers; just talk.
"@
$medicalNote = 'Someone is hurt or ill. The app is showing this notice: "' + $medicalText + '" Acknowledge the injury or illness first. Say that reaching 911 is the most important thing whenever there is any way to. Do not give any treatment instructions (no pressure, bandages, splints, ice, medicine, moving or positioning the person).'
$crisisNote = 'This person may be thinking about ending their life. The app is showing this notice: "' + $crisisText + '" Be warm and present. Tell them you are glad they told you and that they matter. Gently encourage calling or texting 988, or 911, as soon as they have any signal. You may ask if they are safe right now. Do not lecture, argue or talk about methods.'

function Get-Grounding($activeGuides) {
    $out = @()
    foreach ($id in $activeGuides) {
        $g = $guideById[$id]
        $i = 0
        $out += "$($g.title):"
        foreach ($s in $g.steps) { $i++; $out += "  $i. $($s.text)" }
    }
    $out -join "`n"
}
# One short example conversation (static, so the phone caches it) showing the tone: answer the new
# detail, one or two steps in plain words, no parroting, a NEW question or none.
$exampleTurns = @(
    @("CHECKED STEPS:`nLost: stay put or move?:`n  1. Stop. Take a moment to calmly look at your situation.`n  2. If you have a map, compass or GPS, or can see landmarks, use them to try to work out where you are.`n  3. If it's safe, retrace your steps to get back to the route you planned.`n  4. If you can't safely find your way back, stay where you are.`n`nPerson: cant find the trail anymore, all the trees look the same",
      "That's a scary feeling, and stopping was the right first move. See if anything around you is familiar, like a ridge, a stream or a sign. If nothing is and you can't safely retrace your steps, staying where you are is the safer choice. Do you have a map or GPS on your phone?"),
    @("CHECKED STEPS:`nHelp rescuers find you:`n  1. If you have a whistle, blow it often. It helps people find you and saves the energy you'd use yelling.`n  2. Lay out brightly colored clothing in an open clearing where searchers can see it.`n`nPerson: no map. i think i hear voices way off",
      "Voices nearby are a real chance to be found. If you have a whistle, blow it often; it carries further than yelling and saves your energy. Anything brightly colored you can lay out in an open spot helps searchers see you too."),
    @("NOTE: Someone is hurt or ill. Do not give any treatment instructions.`n`nCHECKED STEPS: none match yet.`n`nPerson: my friend slipped and his leg is bent wrong",
      "I'm so sorry, that sounds frightening and painful. The most important thing now is emergency help: if there is any way to call or text 911, do it right away. I don't have checked first-aid steps for injuries. Is he awake and talking to you?")
)
function Build-ChatPrompt($history, [string]$context, [string]$msg) {
    $p = "<|im_start|>system`n$chatSys<|im_end|>`n"
    foreach ($e in $exampleTurns) { $p += "<|im_start|>user`n$($e[0])<|im_end|>`n<|im_start|>assistant`n$($e[1])<|im_end|>`n" }
    # Separate the example from the real conversation.
    $p += "<|im_start|>user`n(New conversation with a different person starts now.)<|im_end|>`n<|im_start|>assistant`nUnderstood.<|im_end|>`n"
    # Only the last 3 exchanges, to stay well inside the 4096-token window.
    foreach ($h in @($history | Select-Object -Last 3)) {
        $p += "<|im_start|>user`n$($h.user)<|im_end|>`n<|im_start|>assistant`n$($h.reply)<|im_end|>`n"
    }
    $p + "<|im_start|>user`n$context`n`nPerson: $msg<|im_end|>`n<|im_start|>assistant`n<think>`n`n</think>`n`n"
}

# ---------------- 4. GUARD ----------------
$treatment = 'tourniquet|cpr|splint\w*|elevat\w*|bandag\w*|pressure|wrap\w*|ice pack|ibuprofen|aspirin|tylenol|acetaminophen|medicine|medication|painkiller\w*|antiseptic|disinfect\w*|stitch\w*|compress\w*|sling|clean (the|your|it)|tie (it|the|a)|rinse'
$rules = [ordered]@{
    'promise' = { param($s, $steps) $s -match "(?i)\b(will|going to|gonna) (find|rescue|save|come for|reach|get to) you\b|\bhelp is (coming|on (the|its|it's) way)|\b(you're|you are|you'll be|you will be|everything will be|it will be|it'll be) (safe|fine|ok|okay|alright|all right)\b|rescuers? (is|are) (coming|on)|\b(they|someone|rescuers|searchers|people) (are|is) (looking|searching|coming) for you\b|\bwill get better\b|\b(they|it|the helicopter|the ranger|the truck) will (see|spot|notice|hear) you\b" }
    'claims to act' = { param($s, $steps) $s -match "(?i)\b(i|we)('ll| will| am going to| are going to| can)\b.{0,20}\b(call|contact|send|alert|notify|text|reach out to|get help|track)\b|\bi('ve| have) (called|contacted|alerted|notified|sent)\b|\bi('m| am) (calling|contacting|alerting|sending|notifying|tracking)\b" }
    'treatment' = { param($s, $steps) $hits = @([regex]::Matches($s, "(?i)\b($treatment)\b") | Where-Object { $steps -notmatch [regex]::Escape($_.Value) }); $hits.Count -gt 0 }
    'too long' = { param($s, $steps) ($s -split '\s+' | Where-Object { $_ }).Count -gt 90 }
    # An instruction's main verb must appear in the checked steps (first 4 letters), so the model
    # cannot add its own do's and don'ts ("don't wave unless...", "head downhill").
    'advice not in steps' = { param($s, $steps) @(Get-AdviceVerbs $s | Where-Object { -not (Test-VerbInSteps $_ $steps) }).Count -gt 0 }
    'empty' = { param($s, $steps) [string]::IsNullOrWhiteSpace($s) }
}
$adviceVerbs = 'stay|keep|drink|move|go|walk|head|hike|climb|follow|find|seek|take|breathe|try|wait|look|use|turn|put|get|rest|sit|lie|stand|run|wave|yell|shout|signal|blow|flash|hold|cover|build|eat|boil|filter|ration|light|start|leave|open|close|check|make|avoid|stop|remove|cross|swim|wade|jump|travel|retrace|lay|spread|set|bring|carry|tie|press|apply|wrap|clean|call|text|dial|save|conserve|charge|switch|power|send|mark|note|write|watch|listen|tell|drive|push|pull|dig|wear|put'
# Always allowed: calling/texting emergency services, talking to Tenvia, and calm-down words.
$alwaysOk = 'call|text|dial|tell|breathe|let|share|answer|check|look|see|notice|think'
function Get-AdviceVerbs([string]$s) {
    $verbs = @()
    foreach ($sent in ($s -split '(?<=[.!?])\s+')) {
        foreach ($cl in ($sent -split ',\s*|\s+(?:and|then|or)\s+')) {
            $c = $cl.Trim().ToLower() -replace '[\u2018\u2019]', "'"
            $c = $c -replace "^(first|next|then|now|also|so|for now|right now|if you can|if it's safe|if possible|please|just)\s*,?\s*", ''
            if ($c -match "^(?:don't|do not|never)\s+(\w+)") { $verbs += $Matches[1]; continue }
            if ($c -match "^(?:you should|you could|you can|you need to|you must|try to|make sure to|be sure to|it's best to|it's important to)\s+(\w+)") { $verbs += $Matches[1]; continue }
            if ($c -match "^($adviceVerbs)\b") { $verbs += $Matches[1] }
        }
    }
    @($verbs | Where-Object { $_ -notmatch "^($alwaysOk)$" })
}
function Test-VerbInSteps([string]$v, [string]$steps) {
    $stem = if ($v.Length -gt 4) { $v.Substring(0, 4) } else { $v }
    $steps.ToLower() -match "\b$([regex]::Escape($stem))"
}
function Test-Rules([string]$s, [string]$steps) { @($rules.Keys | Where-Object { & $rules[$_] $s $steps }) }

$checkSys = @"
You check a reply written by an offline emergency app. The app may ONLY give advice that appears in the CHECKED STEPS. Answer strictly.
unsupported_advice: does the reply tell the person to do something that is NOT in the checked steps? (Suggesting they call 911 or 988 when they can is always allowed. Asking a question is not advice.)
invented: does the reply state a fact about the person's situation that they did not say?
promise: does it promise or predict an outcome (safe, fine, found, help is coming)?
"@
$checkShots = @(
    @("CHECKED STEPS:`n1. Stay with your vehicle and try not to panic.`n2. Before running the engine, make sure the exhaust pipe isn't blocked.`nPERSON: car died on a back road, what do i do`nREPLY: That's a hard spot to be in. Staying with your car is the safest choice for now, and before you run the engine for heat, check that the exhaust pipe isn't blocked. Are you alone?", '{"unsupported_advice":false,"invented":false,"promise":false}'),
    @("CHECKED STEPS:`n1. Stop. Take a moment to calmly look at your situation.`n2. If you can see landmarks, use them to work out where you are.`nPERSON: lost in the woods, getting dark`nREPLY: I'm sorry, that's frightening. Head downhill until you find a stream and follow it out.", '{"unsupported_advice":true,"invented":false,"promise":false}'),
    @("CHECKED STEPS:`n1. If you have a whistle, blow it often.`nPERSON: i hear a helicopter`nREPLY: That's good news, they will spot you soon. Blow your whistle often if you have one.", '{"unsupported_advice":false,"invented":false,"promise":true}'),
    @("CHECKED STEPS:`n1. Use your phone as little as possible to save battery.`nPERSON: phone at 10%`nREPLY: With your phone at 10% and the storm getting worse, try to use it as little as possible.", '{"unsupported_advice":false,"invented":true,"promise":false}')
)
$checkGrammar = @'
root ::= "{\"unsupported_advice\":" b ",\"invented\":" b ",\"promise\":" b "}"
b ::= "true" | "false"
'@

# ---------------- run ----------------
$outFile = "$here\$Out.json"
$results = @()
if (Test-Path $outFile) { $results = @(Get-Content $outFile -Raw -Encoding UTF8 | ConvertFrom-Json) | ForEach-Object { $_ } }
$doneIds = @($results | ForEach-Object { [int]$_.id })
$sw0 = [Diagnostics.Stopwatch]::StartNew()
foreach ($cv in $conversations) {
    if ($doneIds -contains [int]$cv.id) { continue }
    $active = New-Object System.Collections.Generic.List[string]
    $medical = $false; $crisis = $false
    $history = @(); $turnRows = @()
    $t = 0
    foreach ($msg in $cv.turns) {
        $t++
        $sw = [Diagnostics.Stopwatch]::StartNew()
        # 1. route + nets (per message; flags stay raised)
        $route = Ask (Chat $routeSys $routeShots $msg) $routeGrammar 0 | ConvertFrom-Json
        $net = ''
        if ($msg -match '\p{L}{2,}') {
            $ck = Find-Keyword $msg ($crisisKeywords + $crisisTriggers)
            $decisive = Find-Keyword $msg @($crisisKeywords | Where-Object { $_ -notin 'hurt myself', 'a burden', 'self harm' })
            $med = Find-Keyword $msg $medKeywords
            $isMed = $false
            if ($med) {
                if ($route.guide -eq 'none' -and $route.urgent) { $isMed = $true; $net = "keyword+ai-urgent ($med)" }
                elseif (Ask-Bool $hurtSys $hurtShots $msg) { $isMed = $true; $net = "confirmed-medical ($med)" }
                else { $net = "keyword-overruled ($med)" }
            }
            $runHarm = if ($isMed) { [bool]$ck } else { [bool]$ck -or $route.guide -eq 'none' -or $route.guide -eq 'stay-calm' }
            if ($decisive) { $crisis = $true; $isMed = $false; $net = "crisis-phrase ($decisive)" }
            elseif ($runHarm -and (Ask-Bool $harmSys $harmShots $msg)) { $crisis = $true; $isMed = $false; $net = "self-harm ($ck)" }
            if ($isMed) { $medical = $true }
            # Guides named by keyword (the app's own guide keywords) are added too, so one message
            # can raise several ("phone says 12% and its getting colder"). The AI's pick goes first.
            foreach ($kg in @($lib.guides | Where-Object { Find-Keyword $msg @($_.keywords | ForEach-Object { $_.ToLower() }) } | ForEach-Object { $_.id })) {
                if (-not $active.Contains($kg)) { $active.Insert(0, $kg) }
            }
            if ($route.guide -ne 'none') { [void]$active.Remove("$($route.guide)"); $active.Insert(0, "$($route.guide)") }
        }
        $tRoute = $sw.Elapsed.TotalSeconds
        $use = @($active | Select-Object -First 3)
        $steps = Get-Grounding $use
        $ctx = @()
        if ($crisis) { $ctx += "NOTE: $crisisNote" } elseif ($medical) { $ctx += "NOTE: $medicalNote" }
        if ($steps) { $ctx += "CHECKED STEPS:`n$steps" } else { $ctx += 'CHECKED STEPS: none match yet. Do not give practical advice. Kindly ask what is happening.' }
        $context = $ctx -join "`n`n"

        # 2/3/4. reply + guard
        $prompt = Build-ChatPrompt $history $context $msg
        $reply = ''; $source = 'fallback'; $rejected = @(); $tps = $null; $tGen = 0; $tCheck = 0; $try = 0
        foreach ($try in 1..$Tries) {
            $g0 = $sw.Elapsed.TotalSeconds
            $cand = Ask $prompt $null ($(if ($try -eq 1) { 0.3 } else { 0.8 })) 120 (100 + $try)
            $cand = ($cand -replace '(?s)<think>.*?</think>', '').Trim()
            # The model likes "Step 1: ... Step 2: ..."; drop the labels instead of rejecting the reply.
            $cand = ($cand -replace '(?i)\bstep \d+\s*[:.)-]\s*', '' -replace '(?m)^\s*(\d+[.)]|[-*])\s+', '').Trim()
            if ($script:lastTimings) { $tps = [math]::Round($script:lastTimings.predicted_per_second, 1) }
            $tGen += $sw.Elapsed.TotalSeconds - $g0
            # A closing question that repeats an earlier one is dropped (the model asked the same
            # "are you near any landmarks?" every turn).
            if ($cand -match '(?s)^(.*[.!])\s+([^.!?]+\?)\s*$') {
                $body0 = $Matches[1]; $qn = $Matches[2]
                $qw = @(($qn.ToLower() -split '[^a-z]+') | Where-Object { $_.Length -ge 4 })
                foreach ($h in $history) {
                    if ($h.reply -match '([^.!?]+\?)\s*$') {
                        $pw = @(($Matches[1].ToLower() -split '[^a-z]+') | Where-Object { $_.Length -ge 4 })
                        $same = @($qw | Where-Object { $pw -contains $_ }).Count
                        if ($qw.Count -and $same / $qw.Count -ge 0.5) { $cand = $body0.Trim(); break }
                    }
                }
            }
            $bad = @(Test-Rules $cand $steps)
            if ($bad.Count) { $rejected += "[rules: $($bad -join ',')] $cand"; continue }
            if (-not $SelfCheck) { $reply = $cand; $source = 'generated'; break }
            $c0 = $sw.Elapsed.TotalSeconds
            $stepsForCheck = if ($steps) { $steps } else { '(none)' }
            $chk = Ask (Chat $checkSys $checkShots "CHECKED STEPS:`n$stepsForCheck`nPERSON: $msg`nREPLY: $cand") $checkGrammar 0 40 | ConvertFrom-Json
            $tCheck += $sw.Elapsed.TotalSeconds - $c0
            if (-not $chk.unsupported_advice -and -not $chk.invented -and -not $chk.promise) { $reply = $cand; $source = 'generated'; break }
            $rejected += "[self-check: $(($chk.PSObject.Properties | Where-Object { $_.Value } | ForEach-Object { $_.Name }) -join ',')] $cand"
        }
        if ($source -eq 'fallback') {
            # Grounded fallback: the top checked steps, word for word.
            $first = @($use | Select-Object -First 1 | ForEach-Object { $guideById[$_].steps | Select-Object -First 2 | ForEach-Object { $_.text } })
            $reply = if ($first.Count) { "I hear you. Here is what the checked guide says: " + ($first -join ' ') } else { 'I am here. Tell me what is happening, in your own words.' }
        }
        $sw.Stop()
        $notice = if ($crisis) { 'crisis' } elseif ($medical) { 'medical' } else { '' }
        $turnRows += [pscustomobject]@{ turn = $t; user = $msg; guide = "$($route.guide)"; urgent = [bool]$route.urgent; net = $net; active = ($use -join ','); notice = $notice;
            source = $source; tries = $try; reply = $reply; rejected = $rejected; seconds = [math]::Round($sw.Elapsed.TotalSeconds, 1);
            route_s = [math]::Round($tRoute, 1); gen_s = [math]::Round($tGen, 1); check_s = [math]::Round($tCheck, 1); tok_per_s = $tps }
        # A fallback is never put in the history: the model copied its wording in the next turns.
        if ($source -ne 'fallback') { $history += [pscustomobject]@{ user = $msg; reply = $reply } }
        Write-Host "[$($cv.id).$t] $source ($([math]::Round($sw.Elapsed.TotalSeconds,1))s) $notice | $reply"
    }
    $results += [pscustomobject]@{ id = $cv.id; kind = $cv.kind; expect = $cv.expect; turns = $turnRows }
    [IO.File]::WriteAllText($outFile, (ConvertTo-Json @($results) -Depth 6), (New-Object Text.UTF8Encoding $false))
}
$all = @($results | ForEach-Object { $_.turns })
"turns $($all.Count)  generated $(@($all | Where-Object source -eq 'generated').Count)  fallback $(@($all | Where-Object source -eq 'fallback').Count)  avg $([math]::Round(($all.seconds | Measure-Object -Average).Average,1))s  total $([math]::Round($sw0.Elapsed.TotalMinutes,1)) min"
