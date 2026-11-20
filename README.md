# Tenvia

**Hold on, there's a way through.** Tenvia is a free, fully offline Android app that helps people through dangerous situations when there's no signal: lost, stranded, or waiting for rescue. It's free like 911: no ads, accounts, limits or tracking, and no internet permission at all.

Website: **https://tenvia-app.github.io/Main/**

Created by **Aries Fulkerson**. Open source under the [Apache License 2.0](LICENSE); see [NOTICE.md](NOTICE.md).

> ⚠ **Prototype test software. Not for real emergencies: if you are in danger, call 911.** Nothing here has been reviewed by medical or rescue experts yet, and the AI can be wrong. Provided as is, with no warranty and no liability. Read the full [DISCLAIMER.md](DISCLAIMER.md).

## What's here

| Folder / file | What it is |
|---|---|
| [REQUIREMENTS.md](REQUIREMENTS.md) | Principles, scope, architecture, milestones |
| [android/](android/) | The app: Kotlin and Jetpack Compose, minSdk 26, no internet permission. It contains the guide library, the keyword matcher and the medical and self-harm warning-word safety net, with unit tests |
| [content/](content/) | How guide content is made. Every step comes from an official US government page with a verified quote |
| [ai-experiments/PIPELINE.md](ai-experiments/PIPELINE.md) | **The on-device AI pipeline: routing, both safety nets, and semi-free sentences** |
| [ai-experiments/RESULTS.md](ai-experiments/RESULTS.md) | Every AI test round, with honest scores, failures and fixes |
| [docs/](docs/) | The website, served by GitHub Pages |
| [brand/](brand/) | The Tenvia emblem and its design history |

## Key safety decisions

1. **Guides are vetted text, never AI-written.** The AI only chooses which guide to show.
2. **Two safety nets:** medical warning signs (CDC, NIH, NIMH, Ready.gov) and self-harm warning signs (NIMH, SAMHSA), each sign verified word for word on the source page.
3. **Self-harm messages get a fixed crisis message**, never AI text.
4. **Every AI sentence is checked twice** before anyone sees it, and replaced with fixed text if it fails.
