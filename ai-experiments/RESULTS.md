# Milestone 2: on-device AI experiment

*Run 7 November 2026 on the test phone (moto g stylus 2025: Snapdragon 6 Gen 3, 8 GB RAM, Android 16). Fully offline on the phone. Results come from one phone and 10 scenarios: they are early evidence, not proof.*

## Setup

- **Engine:** llama.cpp build b12194, official prebuilt Android arm64 release (checksum verified), run on the CPU with 4 threads.
- **Model:** Qwen3, Apache 2.0 licence (free to bundle and redistribute). Official 8-bit (Q8_0) files from `Qwen/Qwen3-0.6B-GGUF` and `Qwen/Qwen3-1.7B-GGUF` (checksums verified). The 4-bit (Q4_0) versions were made from the 8-bit files on the phone, which loses more quality than making them from the original model.
- **Task:** the model only *chooses a guide* (or flags a medical problem) and writes one calm sentence. Prompt and scenarios: [`scenarios.json`](scenarios.json). Script: [`run-quality-test.ps1`](run-quality-test.ps1). Raw answers: [`quality-results.json`](quality-results.json).

## Speed (llama-bench, tokens per second)

| Model | Size | Reading the prompt | Writing the answer |
|---|---|---|---|
| Qwen3 0.6B 4-bit | 358 MB | 292 t/s | 38.5 t/s |
| Qwen3 0.6B 8-bit | 604 MB | 280 t/s | 23.3 t/s |
| Qwen3 1.7B 4-bit | 1.0 GB | 109 t/s | 14.4 t/s |
| **Qwen3 1.7B 8-bit** | **1.7 GB** | **74 t/s** | **7.9 t/s** |

For Tenvia's prompt (about 250 tokens in, about 25 out), the 1.7B 8-bit model needs roughly **6–7 seconds per answer** once loaded. Loading the model from storage adds several seconds the first time.

## Quality: did it pick the right guide?

| Model | Correct (of 10) |
|---|---|
| Qwen3 0.6B 4-bit | 2 |
| Qwen3 0.6B 8-bit | 2 |
| Qwen3 1.7B 4-bit | 7 |
| **Qwen3 1.7B 8-bit** | **10** |

The 1.7B 8-bit model picked the right guide every time, and correctly flagged "my friend fell down the rocks and he's not waking up" as medical. The keyword matcher (no AI) missed that one; it is now fixed and tested.

## The most important finding: the AI's free-text sentence is not safe

The models were told to give *no instructions*. Even the best one regularly broke that rule or said things that were untrue:

- "You are safe, and **I will contact the appropriate authorities**." (Tenvia can't contact anyone.)
- "You are safe, and **the helicopter will find you**." (a false promise)
- "Let me help you find the trail." (Tenvia can't do that.)
- "Your phone battery is at 9%, **try moving to a warmer place to conserve**…" (advice it was told not to give)
- "Please stay with them and seek…" (medical advice)

**Decision this supports:** the AI may only *choose* which vetted guide to show (and flag out-of-scope cases). Anything Tenvia says to the person must be fixed, pre-written and reviewed text, never generated wording. This confirms principle 5 in the [requirements](../REQUIREMENTS.md).

## Not measured yet

- **Battery use.** The phone was charging over USB the whole time, so the battery numbers would be meaningless. A proper test needs the phone unplugged and connected over wireless debugging (milestone 3).
- **Memory under real app conditions.** The model file sizes give a rough guide (about 1.9 GB of RAM for 1.7B 8-bit).
- **Better 4-bit files.** A 4-bit model made from the original weights (not from the 8-bit file) may keep most of the accuracy at nearly twice the speed. Worth testing.
- **Breadth.** 10 scenarios is a small test. A larger, more varied set (typos, slang, other languages, mixed situations) is needed.

## Recommendation

1. Use **Qwen3 1.7B** for the optional AI tier, as a *router only*.
2. Keep the keyword matcher as the always-available fallback. On these 10 scenarios it does nearly as well, so the AI's value is understanding unusual phrasing.
3. Next: build a larger scenario set (a good job for Hermes), test a properly made 4-bit model, and measure battery unplugged.
