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
import glob, json, os, random, re, sys

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
    """t-*.json: the 4B teacher's runs. p-*.json: the small model's own runs (ids get "p"; v5 runs q-*.json get "q")."""
    convs = []
    for pat, pre in (("t-*.json", ""), ("p-*.json", "p"), ("q-*.json", "q")):
        for f in sorted(glob.glob(os.path.join(HERE, pat))):
            data = json.load(open(f, encoding="utf-8-sig"))
            if isinstance(data, dict):
                data = [data]
            for c in data:
                turns = c["turns"] if isinstance(c["turns"], list) else [c["turns"]]
                convs.append({"id": f'{pre}{c["id"]}', "turns": turns})
    return convs


def next_num(prefix):
    nums = [int(re.match(prefix + r"(\d+)", os.path.basename(f)).group(1)) for f in glob.glob(os.path.join(HERE, prefix + "*.json"))]
    return max(nums + [0]) + 1


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


def verdicts(pattern="judge-out-*.json"):
    v = {}
    for f in glob.glob(os.path.join(HERE, pattern)):
        for j in json.load(open(f, encoding="utf-8-sig")):
            v[str(j["id"])] = j
    return v


def is_ok(j, need_good=True):
    return j is not None and str(j.get("safety", "")).upper() == "SAFE" and (not need_good or str(j.get("helpful", "")).upper() == "GOOD")


def rewritein(only_new=False):
    """Replies that were not SAFE+GOOD, and fallback turns -> rw-in-NN.json for Hermes to write a better reply."""
    v = verdicts()
    have = set(rewrites()) if only_new else set()
    items = []
    for c in load_runs():
        prev = []
        for t in c["turns"]:
            tid = f'{c["id"]}.{t["turn"]}'
            if "context" in t:
                j = v.get(tid)
                if (t["source"] != "generated" or not is_ok(j)) and tid not in have and (t["source"] != "generated" or j is not None):
                    steps = [s for gid in str(t.get("active") or "").split(",") if gid for s in STEPS.get(gid, [])]
                    items.append({"id": tid, "earlier": list(prev), "message": t["user"], "notice_shown": t.get("notice") or "none",
                                  "checked_steps": steps, "old_reply": t["reply"],
                                  "problem": (j or {}).get("reason") or "The app could not write a reply that passed its rules, so it showed plain guide text."})
            prev += [f'PERSON: {t["user"]}', f'APP: {t["reply"]}']
    first = next_num("rw-out-") if only_new else 1
    if not only_new:
        for old in glob.glob(os.path.join(HERE, "rw-in-*.json")):
            os.remove(old)
    names = []
    for n in range(0, len(items), 60):
        names.append(f"rw-in-{n // 60 + first:02d}.json")
        json.dump(items[n:n + 60], open(os.path.join(HERE, names[-1]), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("replies to rewrite", len(items), "files", names)


def rewrites():
    """Hermes rewrites (rw-out-*.json) -> {id: reply}."""
    out = {}
    for f in glob.glob(os.path.join(HERE, "rw-out-*.json")):
        for r in json.load(open(f, encoding="utf-8-sig")):
            out[str(r["id"])] = r["reply"]
    return out


def rwcheck():
    """Rule-check input for chat-v6 -Replay (teacher runs with the rewrites swapped in) + judge-in files for the rewrites."""
    rw = rewrites()
    judged = set(verdicts("rwj-out-*.json"))
    convs, items = [], []
    if True:
        for c in load_runs():
            turns = c["turns"]
            prev, new = [], []
            for t in turns:
                tid = f'{c["id"]}.{t["turn"]}'
                t2 = dict(t)
                if tid in rw and tid in judged:
                    t2["reply"], t2["source"] = rw[tid], "generated"
                elif tid in rw:
                    t2["reply"], t2["source"] = rw[tid], "generated"
                    steps = [s for gid in str(t.get("active") or "").split(",") if gid for s in STEPS.get(gid, [])]
                    items.append({"id": tid, "earlier": list(prev), "message": t["user"], "notice_shown": t.get("notice") or "none", "checked_steps": steps, "reply": rw[tid]})
                prev += [f'PERSON: {t["user"]}', f'APP: {t["reply"]}']
                new.append(t2)
            c2 = dict(c); c2["turns"] = new; convs.append(c2)
    json.dump(convs, open(os.path.join(HERE, "rw-replay.json"), "w", encoding="utf-8"), ensure_ascii=False)
    json.dump([], open(os.path.join(HERE, "rw-empty-judge.json"), "w", encoding="utf-8"))
    # judge files: only rewrites not judged yet (written after the rule check, see rwjudgein)
    json.dump(items, open(os.path.join(HERE, "rw-unjudged.json"), "w", encoding="utf-8"), ensure_ascii=False)
    print("rewrites", len(rw), "not judged yet", len(items))


def rwjudgein():
    """After chat-v6 -Replay wrote rw-check-replay.json: unjudged rewrites that passed the rules -> rwj-in-NN.json."""
    fail = {str(r["id"]) for r in json.load(open(os.path.join(HERE, "rw-check-replay.json"), encoding="utf-8-sig")) if r.get("rules")}
    items = [i for i in json.load(open(os.path.join(HERE, "rw-unjudged.json"), encoding="utf-8")) if i["id"] not in fail]
    first = next_num("rwj-out-")
    names = []
    for n in range(0, len(items), 60):
        names.append(f"rwj-in-{n // 60 + first:02d}.json")
        json.dump(items[n:n + 60], open(os.path.join(HERE, names[-1]), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("rewrites to judge", len(items), "files", names)


VEHICLE = re.compile(r"\b(car|cars|truck|suv|van|jeep|vehicle|rv|camper|minivan|sedan|pickup|engine|tire|tires|dashboard|windshield)\b", re.I)
SIGNAL = re.compile(r"\b(signal|bars?|service|reception|wifi|wi-fi|on the phone|calls? (go|went|goes|are going) through|dispatcher|911 (said|told))\b", re.I)


def misfit(reply, said, active):
    """Stock lines the small model learned to use in the wrong place (v3 on set 5). Returns a reason or ''."""
    r = reply.lower()
    if "stay where you are" in r and "stay-or-move" not in active:
        return "stay-put without the lost guide"
    if re.search(r"\b(your|the) (vehicle|car)\b", r) and not VEHICLE.search(said):
        return "vehicle the person never mentioned"
    if "most important thing right now" in r:
        return "unconditioned 911"
    for s in re.split(r"(?<=[.!?])\s+", reply):
        if re.search(r"\b(911|988|emergency services)\b", s) and not re.search(r"signal|if you can|when you can|as soon as|if your", s, re.I) and not SIGNAL.search(said):
            return "911 without the signal condition"
    return ""


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


def build(require_judge=True, need_good=False, prefer_rw=False, filter_misfits=False):
    verdict = verdicts()
    rw, rwv, rulefail = rewrites(), verdicts("rwj-out-*.json"), set()
    rp = os.path.join(HERE, "rw-check-replay.json")
    if os.path.exists(rp):
        rulefail = {str(r["id"]) for r in json.load(open(rp, encoding="utf-8-sig")) if r.get("rules")}
    out, n = [], {"route": 0, "hurt": 0, "harm": 0, "reply": 0, "rewrite": 0, "repair": 0, "unsafe_skipped": 0, "weak_skipped": 0, "unjudged_skipped": 0, "fallback_skipped": 0}
    for c in load_runs():
        history, said = [], ""
        for t in c["turns"]:
            if "context" not in t:
                continue
            msg = t["user"]
            for call in ([] if str(c["id"])[:1] in "pq" else as_list(t.get("calls"))):  # sorting/checks: teacher only
                sysm = {"route": ROUTE_SYS, "hurt": HURT_SYS, "harm": HARM_SYS}[call["kind"]]
                out.append({"task": call["kind"], "messages": [{"role": "system", "content": sysm}, {"role": "user", "content": msg}, {"role": "assistant", "content": call["out"]}]})
                n[call["kind"]] += 1
            user = f'{t["context"]}\n\nPerson: {msg}'
            base = [{"role": "system", "content": CHAT_SYS}]
            for h in history[-5:]:
                base += [{"role": "user", "content": h[0]}, {"role": "assistant", "content": h[1]}]
            base.append({"role": "user", "content": user})
            tid = f'{c["id"]}.{t["turn"]}'
            said += " " + msg
            active = [g for g in str(t.get("active") or "").split(",") if g]
            bad_fit = (lambda r: misfit(r, said, active)) if filter_misfits else (lambda r: "")
            # a Hermes rewrite that passed the rules, was judged SAFE + GOOD, and uses no stock line out of place
            good_rw = tid in rw and tid not in rulefail and is_ok(rwv.get(tid)) and not bad_fit(rw[tid])
            if t["source"] != "generated":
                if good_rw:
                    out.append({"task": "rewrite", "messages": base + [{"role": "assistant", "content": rw[tid]}]}); n["rewrite"] += 1
                else:
                    n["fallback_skipped"] += 1
                continue
            history.append((user, t["reply"]))  # the app keeps every generated reply in its history
            v = verdict.get(tid)
            if v is None and require_judge:
                n["unjudged_skipped"] += 1
                continue
            teacher_ok = v is None or is_ok(v, need_good)
            if teacher_ok and bad_fit(t["reply"]):
                teacher_ok = False; n["misfit_skipped"] = n.get("misfit_skipped", 0) + 1
            if good_rw and (not teacher_ok or (prefer_rw and not is_ok(v, True))):  # prefer the rewrite over a WEAK teacher reply
                out.append({"task": "rewrite", "messages": base + [{"role": "assistant", "content": rw[tid]}]}); n["rewrite"] += 1
                continue
            if not teacher_ok:
                n["unsafe_skipped" if not is_ok(v, False) else "weak_skipped"] += 1
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
    elif mode == "rewritein":
        rewritein(only_new="--new" in sys.argv)
    elif mode == "rwcheck":
        rwcheck()
    elif mode == "rwjudgein":
        rwjudgein()
    else:
        # v3: --good   v4: --prefer-rw --filter (SAFE teacher replies kept, GOOD rewrites preferred, stock lines out of place dropped)
        build(require_judge="--nojudge" not in sys.argv, need_good="--good" in sys.argv,
              prefer_rw="--prefer-rw" in sys.argv, filter_misfits="--filter" in sys.argv)
