# Tenvia guide content: how it's made

The app's guides live in [`android/app/src/main/assets/guides.json`](../android/app/src/main/assets/guides.json). Every step has to follow these rules.

## Rules

1. **Official sources only.** Each step is paraphrased from a page on an official US government website (`.gov` or `.mil`). Works by US federal employees are generally in the public domain, so they can be reused freely. Images and third-party material on those pages are not used.
2. **Every step is traceable.** Each step stores its `source_url` and an exact `quote` from that page that supports it.
3. **Every quote is checked.** Quotes are checked word for word against the live page (ignoring punctuation and capitals). [`verify-sources.ps1`](verify-sources.ps1) does this automatically. A few sites (cdc.gov, 911.gov, fcc.gov) block automated downloads, so those were checked by opening each page in a browser and running the same comparison there.
4. **No medical treatment.** Medical problems and crises are detected as out of scope, and the person is told to reach emergency services.
5. **Not final until experts review it.** Every guide stays `"draft": true`, and the app shows "PROTOTYPE: NOT FOR REAL EMERGENCIES" until qualified safety, medical and outdoor professionals review the content (testing track 3 in the [requirements](../REQUIREMENTS.md)).

## History of version 2 (7 November 2026)

- **Drafting:** Hermes Agent drafted 44 steps from 16 official pages ([`drafts/`](drafts/)).
- **Verification:** 43 quotes were confirmed word for word. One step that relied on a state PDF couldn't be checked, so it was replaced with a verified quote from 911.gov.
- **Review by Claude:** each change was approved by Aries Fulkerson.
  - **Stay calm:** cut down to its two genuine steps (the draft duplicated steps from "Lost").
  - **Equipment:** steps that assumed a map, whistle, flashlight, mirror or hat were reworded "If you have…".
  - **Cold weather:** packing advice was reworded for someone already stranded.
  - **Stranded vehicle:** reordered, so the exhaust pipe is checked before the engine is run (carbon-monoxide safety).
  - **Heat:** "use a reflective sunshade" (camping gear) was removed.
- **Result:** 41 steps in 9 guides.

## Known gaps for expert review

- The vehicle guide comes from a winter-storm source; hot-weather vehicle guidance is still missing (the Death Valley safety page, checked 2026-11-20, has no breakdown advice; still looking for an official source).
- Tornado (outdoors or in a vehicle) and dust-storm (driving) steps were added to the wind guide on 2026-11-20 from weather.gov, labelled "Tornado:" and "Dust storm:" like the animal steps. Its first step ("Take shelter in your car") is general wind advice; during a tornado a vehicle is not safe, so an expert should check that the two read clearly together.
- No official on-foot dust-storm advice was found (weather.gov covers driving only).
- "Stay put" in the Lost guide has a very short supporting quote; an expert should confirm the context.
- The guides cover the US only (for example the 911 number); other countries come later.
