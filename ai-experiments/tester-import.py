"""Import real tester chats into a private test set (outside the public repo).

Usage (run with the tenvia-train venv python, -I):
  python -I tester-import.py add <file.txt> [--iphone] [--name WORD ...]
  python -I tester-import.py list

<file.txt> is the pasted email body: either a "Send this chat" export
("Person: ..." / "Tenvia: ..." blocks) or an iPhone tester email (free text,
the first message plus optional follow-ups, one per paragraph or numbered).

What it does:
  - strips personal details: email addresses, phone numbers, web links,
    GPS coordinates, street addresses, and any --name words you pass;
  - prints every Capitalized word it kept so a human can check for names/places;
  - appends one conversation to C:\\Users\\aries\\tenvia-private\\testers\\tester-set.json
    in the same shape as the sealed sets: {id, kind, expect, turns, source, app, orig_replies}.
    kind/expect start empty ("?") and are filled in by hand before scoring.
The original text is never saved, only the cleaned version.
"""
import json, os, re, sys

PRIV = os.environ.get('TENVIA_TESTERS', r'C:\Users\aries\tenvia-private\testers')
SET = os.path.join(PRIV, 'tester-set.json')
FIRST_ID = 5001

SCRUB = [
    (re.compile(r'[\w.+-]+@[\w-]+(\.[\w-]+)+'), '[email]'),
    (re.compile(r'https?://\S+|www\.\S+'), '[link]'),
    (re.compile(r'-?\d{1,3}\.\d{3,}\s*,\s*-?\d{1,3}\.\d{3,}'), '[coordinates]'),
    (re.compile(r'(\+?1[\s.-]?)?\(?\d{3}\)?[\s.-]\d{3}[\s.-]\d{4}\b'), '[phone]'),
    (re.compile(r'\b\d{2,6}\s+(?:[A-Z][a-z]+\s+){1,3}(?:St|Street|Ave|Avenue|Rd|Road|Dr|Drive|Ln|Lane|Blvd|Ct|Court|Way|Pl|Hwy)\b\.?', re.I), '[address]'),
]
# Capitalized words that are normal in chats and don't need checking.
COMMON = set('''I I'm I've I'll I'd Tenvia Person App Ok OK Okay Hi Hey Help Please Thanks Thank Yes No
The A An My We It Its It's Is Are Was What Where When How Why Should Can Could Would Will Do Does Did
Im Ive Dont Cant Its Our Your They He She This That There Then And But So Or If Just Now Still Also Not
Stay Try Make Keep Call Text If Before In On At Of To For With From Wool Cotton Move Find Use
Monday Tuesday Wednesday Thursday Friday Saturday Sunday GPS SOS AI'''.split())


def load():
    if os.path.exists(SET):
        return json.load(open(SET, encoding='utf-8'))
    return {'conversations': []}


def scrub(text, names):
    for rx, rep in SCRUB:
        text = rx.sub(rep, text)
    for n in names:
        text = re.sub(r'\b' + re.escape(n) + r'\b', '[name]', text, flags=re.I)
    return text


def parse_export(text):
    """'Person:' / 'Tenvia:' blocks from the app's Send this chat."""
    app = ''
    m = re.search(r'App (\S+), model (\S+), (\d{4}-\d{2}-\d{2})', text)
    if m:
        app = f'{m.group(1)} {m.group(2)} {m.group(3)}'
    turns, replies, cur, buf = [], [], None, []

    def flush():
        if cur and buf:
            (turns if cur == 'p' else replies).append(' '.join(' '.join(buf).split()))
    for line in text.splitlines():
        s = line.strip()
        if s.startswith('Person:'):
            flush(); cur, buf = 'p', [s[7:]]
        elif s.startswith('Tenvia:'):
            flush(); cur, buf = 't', [s[7:]]
        elif cur and s:
            buf.append(s)
    flush()
    return turns, replies, app


def parse_iphone(text):
    """Free text: paragraphs (or numbered lines) are the person's messages in order."""
    text = re.sub(r'(?im)^\s*(subject|from|to|date|sent):.*$', '', text)
    parts = [p for p in re.split(r'\n\s*\n|\n\s*(?=\d+[.)]\s)', text) if p.strip()]
    turns = [' '.join(re.sub(r'^\s*\d+[.)]\s*', '', p).split()) for p in parts]
    return turns[:6], [], ''


def main():
    a = sys.argv[1:]
    if not a or a[0] not in ('add', 'list'):
        print(__doc__); return
    data = load()
    if a[0] == 'list':
        for c in data['conversations']:
            print(c['id'], c['source'], c['kind'], len(c['turns']), 'turns')
        print(len(data['conversations']), 'conversations in', SET)
        return
    src = a[1]
    iphone = '--iphone' in a
    names = a[a.index('--name') + 1:] if '--name' in a else []
    raw = open(src, encoding='utf-8-sig').read()
    if not iphone and 'Person:' not in raw:
        iphone = True
    turns, replies, app = parse_iphone(raw) if iphone else parse_export(raw)
    turns = [scrub(t, names) for t in turns if t]
    replies = [scrub(t, names) for t in replies]
    if not turns:
        print('FAILED: found no messages'); sys.exit(1)
    ids = [c['id'] for c in data['conversations']]
    cid = max(ids) + 1 if ids else FIRST_ID
    conv = {'id': cid, 'kind': '?', 'expect': [], 'turns': turns,
            'source': 'iphone' if iphone else 'android', 'app': app, 'orig_replies': replies}
    data['conversations'].append(conv)
    os.makedirs(PRIV, exist_ok=True)
    json.dump(data, open(SET, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    caps = sorted({w for t in turns + replies for w in re.findall(r"\b[A-Z][a-zA-Z']+\b", t)} - COMMON)
    print(f'added {cid} ({conv["source"]}, {len(turns)} turns) to {SET}')
    print('check these capitalized words for names/places:', ', '.join(caps) or '(none)')


if __name__ == '__main__':
    main()
