# Tenvia: product requirements

*Version 0.1, 7 November 2026. Owner: Aries Fulkerson. Status: early concept. Nothing here has been built or tested yet.*

> Tenvia is not a replacement for emergency services. It is an unproven concept. Nothing in this document claims that Tenvia is safe, reliable or effective, and no testing has been done.

## 1. What Tenvia is

Tenvia is a free Android app that helps a person through a dangerous situation when there is no phone signal or internet. The person describes what is happening in their own words. Tenvia responds calmly and steadily, and guides them through vetted, step-by-step help.

The name joins Latin *tenē* ("hold on") and *via* ("the way"): **hold on, there's a way through.**

## 2. Principles (these do not change)

1. **Fully offline.** Every feature works in airplane mode, with no network at any point after install. The app should not even request internet permission, so anyone can verify it never connects.
2. **Free like 911.** No ads, paid tiers, subscriptions, usage limits, accounts or sign-ins, ever.
3. **Private.** Nothing a person types or says leaves the phone. No analytics, tracking or data collection.
4. **Works without the AI.** The core guides work on any supported phone, even one too weak to run a model or with the AI switched off. The AI is an upgrade, never a requirement.
5. **The AI never invents life-critical steps.** Instructions come only from vetted content. The AI's job is to understand the situation, pick the right guide, ask follow-up questions and keep the person calm, not to make up advice.
6. **Battery comes first.** In an emergency, the phone's battery is a survival resource. Tenvia should use as little of it as possible and help the person save the rest.
7. **Honest about its limits.** Tenvia says when it is unsure, encourages contacting real help whenever that's possible, and never presents itself as proven, approved or a substitute for professionals.

## 3. Who it's for

Anyone, with no training needed: people who are stressed or scared, who may be cold, injured, in the dark, using one hand, or reading poorly. Design for the worst moment, not the best:

- big text and buttons, high contrast, readable in sunlight and in the dark
- short sentences, one step at a time
- voice input and spoken replies (on the phone), as hands may be busy
- English first; more languages later

## 4. First release: lost, stranded or waiting for rescue

**Why this first:** losing your connection is part of the situation itself, the established guidance is stable and well known, and it carries less risk than medical treatment. Medical first aid is the next category and needs expert review first.

**In scope (topics, not instructions):**
- deciding whether to stay put or move
- helping rescuers find you (signaling)
- keeping safe from cold, heat and weather, and the order of priorities (shelter, water, food)
- making the phone battery last
- contacting help as soon as any signal or connection is available
- staying calm and thinking clearly

**Out of scope for the first release:** medical treatment instructions (bleeding, CPR, fractures and so on), poisoning, mental-health crises and anything else not listed above. When a person describes one of these, Tenvia says plainly that it can't guide them yet and encourages them to reach emergency services if possible.

**Content rule:** all guide content will be drafted from reputable public sources (for example US government emergency-preparedness and park-safety material, which is generally free to reuse), with every source and licence recorded. Before anyone relies on it, qualified people must review it. Until then, every build is clearly labelled "not for real emergencies."

## 5. How it will be built (proposal)

| Part | What it does | Proposal |
|---|---|---|
| App | Screens, voice, settings | Native Android: Kotlin with Jetpack Compose |
| Guide library | Vetted step-by-step guides, stored as structured data inside the app | Works with no AI; opens instantly |
| Simple matcher | Finds the right guide from the person's words without AI, using keywords | The fallback for every phone |
| On-device AI (optional) | Understands free-form descriptions, asks follow-up questions, walks through a guide calmly | A small, openly licensed model (around 1–3 billion parameters, compressed) run with llama.cpp. Licence must allow free redistribution |
| Safety layer | Keeps the AI to vetted guide content and blocks out-of-scope advice | Checks every AI reply before it's shown |
| Power modes | Normal, low-battery, critical | Lower modes turn off the AI and voice, darken the screen, and remove background work |

**Delivery:** the model and guides arrive with the app at install (for example through Google Play's install-time asset delivery), so the app never needs to download anything later. A built-in self-check confirms everything is present before it's needed.

**Phones:** support older and cheaper Android phones for the guide library. The AI tier will run only on phones that can handle it; the exact minimum is to be measured, not guessed.

## 6. Data and privacy

- Stored on the phone only: settings (language, text size, voice on or off). Conversations are kept only for the current session unless the person chooses otherwise.
- No accounts, analytics, crash reporting to servers, or ads, and no internet permission.

## 7. Testing (from the project brief; kept as four separate tracks)

1. **Offline behaviour:** airplane-mode checks of startup, speed, stability, battery use, and any hidden dependence on a connection.
2. **Controlled simulations:** scripted, supervised scenarios, never real emergencies.
3. **Safety boundaries:** qualified safety, medical and outdoor experts review failures, ambiguity and overconfidence, and set stop conditions.
4. **Understanding and trust:** non-emergency role-play to check people understand Tenvia, can correct it and know when not to rely on it.

Targets (battery use, response time and so on) will be set with experts and by measuring, not made up in advance. Failures are reported alongside successes.

## 8. Milestones

| # | Milestone | Needs funding? |
|---|---|---|
| 0 | Name, emblem, ownership record, this document | Done |
| 1 | App skeleton: guide library and simple matcher, no AI, draft content clearly labelled not for real use | No |
| 2 | On-device AI experiment: does a small model run acceptably on a mid-range phone? | No |
| 3 | Offline and battery testing (testing track 1) | No |
| 4 | Expert review and supervised simulations (testing tracks 2–4) | Likely |
| 5 | Medical first aid category, with expert-reviewed content | Likely |

## 9. Open decisions

1. Minimum Android version and phone specs (to be measured in milestone 2).
2. Which openly licensed model, and its exact licence terms.
3. Which sources the first guides draw on, and who reviews them.
4. Open-source licence (leaning AGPL, decided before going public).
5. Which languages come after English.
6. How guide updates reach people who are offline most of the time (for example through normal app updates).
