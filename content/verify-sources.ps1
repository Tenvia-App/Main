# Checks every drafted guide step against its source page:
#  1. the source is an official US government site (.gov or .mil)
#  2. the quoted sentence really appears on that page (after normalizing spaces and punctuation)
# Usage: powershell -File content\verify-sources.ps1 content\drafts\batch1.json [more files...]

param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Files)

$ProgressPreference = 'SilentlyContinue'
# Windows PowerShell 5.1 doesn't enable modern TLS by default; some .gov sites require it.
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]'Tls13'
$cache = @{}

function Normalize([string]$s) {
    $s = [System.Net.WebUtility]::HtmlDecode($s)
    # Curly quotes, dashes and odd spaces all become plain spaces in the next line.
    $s = $s.ToLowerInvariant() -replace '[^a-z0-9]+', ' '
    return $s.Trim()
}

function Get-PageText([string]$url) {
    if ($cache.ContainsKey($url)) { return $cache[$url] }
    try {
        $html = (Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 40 -Headers @{ 'User-Agent' = 'Mozilla/5.0 (Tenvia source check)' }).Content
        $text = $html -replace '(?s)<script.*?</script>', ' ' -replace '(?s)<style.*?</style>', ' ' -replace '<[^>]+>', ' '
        $cache[$url] = Normalize $text
    } catch { $cache[$url] = $null }
    return $cache[$url]
}

$total = 0; $ok = 0
foreach ($f in $Files) {
    $data = Get-Content $f -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($g in $data.guides) {
        $i = 0
        foreach ($s in $g.steps) {
            $i++; $total++
            $host_ = ([uri]$s.source_url).Host
            $official = $host_ -match '\.(gov|mil)$'
            $page = Get-PageText $s.source_url
            $status =
                if (-not $official) { 'NOT-OFFICIAL' }
                elseif ($null -eq $page) { 'PAGE-UNREACHABLE' }
                elseif ($page.Contains((Normalize $s.quote))) { 'OK' }
                else { 'QUOTE-NOT-FOUND' }
            if ($status -eq 'OK') { $ok++ }
            "{0,-16} {1}#{2}  {3}" -f $status, $g.id, $i, $s.source_url
        }
    }
}
"---"
"$ok of $total steps verified"
