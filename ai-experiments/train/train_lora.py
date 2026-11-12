"""LoRA fine-tune of Qwen3 1.7B on train.jsonl, then merge to a plain model folder for GGUF conversion.

  python train_lora.py --base C:/Users/aries/tenvia-train/qwen3-1.7b --out C:/Users/aries/tenvia-train/tenvia-1.7b-v1

Prompts are plain ChatML, exactly as the app sends them to /completion (no <think> block).
Only the last assistant message of each example is learned (everything before it is context).
"""
import argparse, json, math, os, random, time
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import LoraConfig, get_peft_model

ap = argparse.ArgumentParser()
ap.add_argument("--base", required=True)
ap.add_argument("--out", required=True)
ap.add_argument("--data", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "train.jsonl"))
ap.add_argument("--epochs", type=float, default=3)
ap.add_argument("--lr", type=float, default=2e-4)
ap.add_argument("--rank", type=int, default=16)
ap.add_argument("--batch", type=int, default=4)
ap.add_argument("--accum", type=int, default=4)
ap.add_argument("--maxlen", type=int, default=2048)
args = ap.parse_args()

tok = AutoTokenizer.from_pretrained(args.base)
model = AutoModelForCausalLM.from_pretrained(args.base, torch_dtype=torch.bfloat16, device_map={"": 0})
model.gradient_checkpointing_enable()
model = get_peft_model(model, LoraConfig(r=args.rank, lora_alpha=2 * args.rank, lora_dropout=0.05, task_type="CAUSAL_LM",
                                         target_modules=["q_proj", "k_proj", "v_proj", "o_proj", "gate_proj", "up_proj", "down_proj"]))
model.print_trainable_parameters()


def encode(ex):
    msgs = ex["messages"]
    prefix = "".join(f'<|im_start|>{m["role"]}\n{m["content"]}<|im_end|>\n' for m in msgs[:-1]) + "<|im_start|>assistant\n"
    target = msgs[-1]["content"] + "<|im_end|>"
    p = tok(prefix, add_special_tokens=False)["input_ids"]
    t = tok(target, add_special_tokens=False)["input_ids"]
    if len(p) + len(t) > args.maxlen:  # drop the oldest context, keep the end (the steps and the message)
        p = p[len(p) + len(t) - args.maxlen:]
    return p + t, [-100] * len(p) + t


data = [encode(json.loads(l)) for l in open(args.data, encoding="utf-8")]
random.Random(1).shuffle(data)
val, train = data[:40], data[40:]
print("train", len(train), "val", len(val), "max tokens", max(len(x[0]) for x in data))


def batches(items, bs, shuffle):
    idx = list(range(len(items)))
    if shuffle:
        random.shuffle(idx)
    idx.sort(key=lambda i: len(items[i][0]) // 256 + random.random()) if shuffle else None  # rough length buckets
    groups = [idx[i:i + bs] for i in range(0, len(idx), bs)]
    if shuffle:
        random.shuffle(groups)
    for g in groups:
        n = max(len(items[i][0]) for i in g)
        ids = torch.full((len(g), n), tok.pad_token_id or 0)
        lab = torch.full((len(g), n), -100)
        att = torch.zeros((len(g), n), dtype=torch.long)
        for r, i in enumerate(g):
            x, y = items[i]
            ids[r, :len(x)] = torch.tensor(x); lab[r, :len(y)] = torch.tensor(y); att[r, :len(x)] = 1
        yield ids.cuda(), att.cuda(), lab.cuda()


def evaluate():
    model.eval(); tot = 0; cnt = 0
    with torch.no_grad():
        for ids, att, lab in batches(val, args.batch, False):
            tot += model(input_ids=ids, attention_mask=att, labels=lab).loss.item(); cnt += 1
    model.train()
    return tot / max(cnt, 1)


steps_total = math.ceil(len(train) / (args.batch * args.accum) * args.epochs)
opt = torch.optim.AdamW([p for p in model.parameters() if p.requires_grad], lr=args.lr, weight_decay=0.0)
sched = torch.optim.lr_scheduler.LambdaLR(opt, lambda s: min(1.0, (s + 1) / 10) * max(0.05, 1 - s / steps_total))
print("optimizer steps", steps_total, "val loss before", round(evaluate(), 3))
model.train(); step = 0; t0 = time.time(); done = False
while not done:
    for i, (ids, att, lab) in enumerate(batches(train, args.batch, True)):
        loss = model(input_ids=ids, attention_mask=att, labels=lab).loss / args.accum
        loss.backward()
        if (i + 1) % args.accum == 0:
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            opt.step(); sched.step(); opt.zero_grad(); step += 1
            if step % 10 == 0:
                print(f"step {step}/{steps_total} loss {loss.item() * args.accum:.3f} {int(time.time() - t0)}s", flush=True)
            if step % 50 == 0:
                print("  val loss", round(evaluate(), 3), flush=True)
            if step >= steps_total:
                done = True
                break
print("val loss after", round(evaluate(), 3))
model = model.merge_and_unload()
model.save_pretrained(args.out, safe_serialization=True)
tok.save_pretrained(args.out)
print("saved", args.out)
