# Tenvia guide content: how it's made

The app's guides live in [`android/app/src/main/assets/guides.json`](../android/app/src/main/assets/guides.json). Every step has to follow these rules.

## Rules

1. **Official sources only.** Each step is paraphrased from a page on an official government website: US federal (`.gov`, `.mil`) and state agencies, and since version 4 (23 November 2026) also official Australian (`.gov.au`), New Zealand (`.govt.nz`), UK (`nhs.uk`), EU (`europa.eu`) and Canadian (`canada.ca`) sites. Works by US federal employees are generally in the public domain. Other sites keep their copyright, so their steps are short paraphrases with one short supporting quote each. MedlinePlus encyclopedia pages are licensed from A.D.A.M. and are treated the same way. Images and third-party material are not used.
2. **Every step is traceable.** Each step stores its `source_url` and an exact `quote` from that page that supports it.
3. **Every quote is checked.** Quotes are checked word for word against the live page (ignoring punctuation and capitals). [`verify-sources.ps1`](verify-sources.ps1) does this automatically. A few sites (cdc.gov, 911.gov, fcc.gov) block automated downloads, so those were checked by opening each page in a browser and running the same comparison there.
4. **First aid only from checked steps.** Until version 3, all medical problems were out of scope. On 23 November 2026, after the first real tester chats (a snake bite in Australia), Aries Fulkerson decided Tenvia should give basic first aid. First-aid steps follow the same rules as every other step. The app still says to reach emergency services first, and the AI may give no treatment that isn't a checked step. Where countries' official advice differs, the steps are labelled by country ("US (CDC):", "Australia:"), and the app shows only the one for the country the person names.
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
- **Version 4 (23 November 2026)** added 31 guides with 201 steps, from 30 official pages, all quotes checked word for word ([`drafts/batch4.json`](drafts/batch4.json) to [`drafts/batch8-home.json`](drafts/batch8-home.json)):
  - first aid: snake bite, broken bone and splint, bleeding, burns, choking, CPR, severe allergic reaction, head injury, seizure, heart attack, stroke, heat illness, hypothermia, frostbite, spider bites, ticks and stings
  - natural disasters: earthquake, tsunami, avalanche, hurricane, landslide, volcano, winter storm
  - water and ice: rip current, falling through ice, someone in the water
  - home and town: house fire, carbon monoxide, gas leak, power outage, chemical spill
  - emergency numbers for Australia, the UK, Europe, New Zealand and Canada

  **First aid is the most important part for an expert to check.** Specific questions:
  - **Snake bite:** the US (wash the bite) and Australian (don't wash it; pressure bandage) steps contradict each other on purpose.
  - **Bleeding:** the home-made tourniquet step.
  - **Frostbite:** the "don't thaw if it may refreeze" step.
  - **Stroke:** "call an ambulance rather than drive", in places an ambulance can't reach.
- **Version 5 (26 November 2026):**
  - the 14 disaster, home and water guides were rewritten shorter, one condition per step with the condition first (same sources and quotes), because the AI tended to drop "If ..." conditions from the longer steps ([`rewrite-v5.py`](rewrite-v5.py));
  - new **boat trouble on open water** guide (tester chat 5003), 13 steps from NSW, Victoria and Western Australia boating pages and Transport Canada;
  - **crisis lines outside the US**: Lifeline (Australia), Samaritans (UK) and 1737 (New Zealand), shown with the emergency number in the crisis notice. healthnz.govt.nz blocks automated downloads, so the NZ quote was checked in a browser ([`drafts/batch9-boat-crisis.json`](drafts/batch9-boat-crisis.json)).
- **For reviewers:** [EXPERT-REVIEW.md](EXPERT-REVIEW.md) has a letter to send, the specific questions, and every first-aid, disaster and boat step with its source.
