"""Builds the small model's training file from the 4B teacher's recorded runs (train/t-*.json).

  python build_data.py judgein   -> train/judge-in-NN.json (60 replies each, for Hermes to grade)
  python build_data.py build     -> train/train.jsonl (+ counts), using train/judge-out-NN.json

The student is trained on SHORT prompts (no long instructions or examples): it learns the behaviour,
so the phone doesn't have to read 3,000 tokens of fixed text. Four jobs, one model:
  route  : message -> {"about","guide","urgent"}      (the teacher's answer)
  hurt   : message -> {"answer":bool}                 (the teacher's answer)
  harm   : message -> {"answer":bool}                 (the teacher's answer)
  reply  : steps + history + message -> reply         (only replies that passed the rules AND were judged SAFE)
  repair : rejected reply + reason -> the reply that then passed (same filter)
"""
import glob, json, os, random, sys

HERE = os.path.dirname(os.path.abspath(__file__))
GUIDES = json.load(open(os.path.join(HERE, "..", "..", "android", "app", "src", "main", "assets", "guides.json"), encoding="utf-8"))
GUIDE_IDS = [g["id"] for g in GUIDES["guides"]]
STEPS = {g["id"]: [s["text"] for s in g["steps"]] for g in GUIDES["guides"]}

ROUTE_SYS = "Sort this message for Tenvia, an offline emergency app. Reply as JSON with about, guide and urgent. Guides: " + ", ".join(GUIDE_IDS) + ", none."
HURT_SYS = 'Is a person injured, ill or having a medical problem right now? Reply {"answer":true} or {"answer":false}.'
HARM_SYS = 'May this person be thinking about suicide or harming themselves? Reply {"answer":true} or {"answer":false}.'
CHAT_SYS = ("You are Tenvia, a calm helper in an offline emergency app. You cannot contact anyone. "
            "Use only the CHECKED STEPS given for this message and keep their conditions. "
            "If they don't cover something, say so honestly. No first aid. 2 to 4 short sentences.")
REWRITE = "Rewrite your reply. Problem: {hint} Keep everything else, 2 to 4 short sentences."


def load_runs():
    convs = []
    for f in sorted(glob.glob(os.path.join(HERE, "t-*.json"))):
        data = json.load(open(f, encoding="utf-8-sig"))
        if isinstance(data, dict):
            data = [data]
        for c in data:
            turns = c["turns"] if isinstance(c["turns"], list) else [c["turns"]]
            convs.append({"id": c["id"], "turns": turns})
    return convs


def as_list(x):
    return [] if x is None else (x if isinstance(x, list) else [x])


def judge_items(convs):
    items = []
    for c in convs:
        prev = []
        for t in c["turns"]:
            steps = [s for gid in str(t.get("active") or "").split(",") if gid for s in STEPS.get(gid, [])]
            if t["source"] == "generated" and "context" in t:
                items.append({"id": f'{c["id"]}.{t["turn"]}', "earlier": list(prev), "message": t["user"],
                              "notice_shown": t.get("notice") or "none", "checked_steps": steps, "reply": t["reply"]})
            prev += [f'PERSON: {t["user"]}', f'APP: {t["reply"]}']
    return items


def judged_ids():
    return {str(j["id"]) for f in glob.glob(os.path.join(HERE, "judge-out-*.json")) for j in json.load(open(f, encoding="utf-8-sig"))}


def judgein(only_new=False):
    items = judge_items(load_runs())
    first = 1
    if only_new:  # keep the old files; write only replies with no verdict yet, numbered after the last judge-out file
        done = judged_ids()
        items = [i for i in items if i["id"] not in done]
        first = max([int(os.path.basename(f)[10:12]) for f in glob.glob(os.path.join(HERE, "judge-out-*.json"))] + [0]) + 1
    else:
        for old in glob.glob(os.path.join(HERE, "judge-in-*.json")):
            os.remove(old)
    for n in range(0, len(items), 60):
        json.dump(items[n:n + 60], open(os.path.join(HERE, f"judge-in-{n // 60 + first:02d}.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("replies to judge", len(items), "files", [f"judge-in-{n // 60 + first:02d}.json" for n in range(0, len(items), 60)])


def build(require_judge=True):
    verdict = {}
    for f in glob.glob(os.path.join(HERE, "judge-out-*.json")):
        for j in json.load(open(f, encoding="utf-8-sig")):
            verdict[str(j["id"])] = j
    out, n = [], {"route": 0, "hurt": 0, "harm": 0, "reply": 0, "repair": 0, "unsafe_skipped": 0, "unjudged_skipped": 0, "fallback_skipped": 0}
    for c in load_runs():
        history = []
        for t in c["turns"]:
            if "context" not in t:
                continue
            msg = t["user"]
            for call in as_list(t.get("calls")):
                sysm = {"route": ROUTE_SYS, "hurt": HURT_SYS, "harm": HARM_SYS}[call["kind"]]
                out.append({"task": call["kind"], "messages": [{"role": "system", "content": sysm}, {"role": "user", "content": msg}, {"role": "assistant", "content": call["out"]}]})
                n[call["kind"]] += 1
            user = f'{t["context"]}\n\nPerson: {msg}'
            base = [{"role": "system", "content": CHAT_SYS}]
            for h in history[-5:]:
                base += [{"role": "user", "content": h[0]}, {"role": "assistant", "content": h[1]}]
            base.append({"role": "user", "content": user})
            if t["source"] != "generated":
                n["fallback_skipped"] += 1
                continue
            history.append((user, t["reply"]))  # the app keeps every generated reply in its history
            v = verdict.get(f'{c["id"]}.{t["turn"]}')
            if v is None:
                if require_judge:
                    n["unjudged_skipped"] += 1
                    continue
            elif str(v.get("safety", "")).upper() != "SAFE":
                n["unsafe_skipped"] += 1
                continue
            out.append({"task": "reply", "messages": base + [{"role": "assistant", "content": t["reply"]}]})
            n["reply"] += 1
            tries = as_list(t.get("trylog"))
            if len(tries) >= 2 and tries[-2].get("bad") and not tries[-1].get("bad"):
                out.append({"task": "repair", "messages": base + [
                    {"role": "assistant", "content": tries[-2]["cand"]},
                    {"role": "user", "content": REWRITE.format(hint=tries[-2]["hint"])},
                    {"role": "assistant", "content": t["reply"]}]})
                n["repair"] += 1
    random.Random(7).shuffle(out)
    with open(os.path.join(HERE, "train.jsonl"), "w", encoding="utf-8") as f:
        for ex in out:
            f.write(json.dumps(ex, ensure_ascii=False) + "\n")
    print("examples", len(out), n)


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "build"
    if mode == "judgein":
        judgein(only_new="--new" in sys.argv)
    else:
        build(require_judge="--nojudge" not in sys.argv)
