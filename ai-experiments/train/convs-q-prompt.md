# Write teaching conversations for Tenvia: direct questions

Tenvia is an offline US emergency app (lost, stranded, waiting for rescue). It cannot contact anyone.
Its small AI often skips the person's direct question. These conversations teach it to answer first.

Write 60 conversations as JSON: `{"conversations":[{"id":N,"kind":"...","expect":[guide ids],"turns":["...","...","..."]}, ...]}`
with ids from FIRST to FIRST+59 and exactly 3 turns each (what the person types, 1 to 3 sentences, phone style).

Guide ids: stay-or-move, get-found, cold-weather, heat, water, phone-battery, reach-help, stranded-vehicle,
stay-calm, flood, wildfire, lightning, high-wind, coast-tide, wildlife, kids-group.

Rules:
- EVERY turn ends with one direct question the person wants answered: "Should I...?", "Can I...?",
  "Is it OK to...?", "Do I X or Y?", "How long...?", "Which...?", "Will...?", "What do I do about...?".
- Mix the kind of question across turns:
  - about a third are answered yes/no by a common safety step (cross the flood water? run the engine for heat?
    leave the car in a blizzard? drink stream water? shelter under a tree in lightning? turn the phone off?);
  - about a third are things a short checked guide would NOT cover (a specific medicine, a trail name, a weather
    forecast, an exact distance, whether rescue is on its way, how to fix the car), so the honest answer is
    "I don't know that" plus useful advice;
  - the rest are either/or choices or "what first" questions where the wrong obvious answer is tempting
    (stay put when the water is rising, stay with the car when it is far away, keep walking to warm up).
- Each conversation adds new facts each turn (battery %, signal or none, weather change, someone with them,
  an animal, a vehicle that is near or far, kids, darkness). Name a real US place in turn 1 in most of them.
- Kinds: 48 "guide", 4 "medical" (someone is hurt or ill right now), 4 "mixed" (two hazards at once),
  2 "selfharm" (hopeless, thinking about not wanting to go on, still asking a practical question),
  2 "offtopic" (a non-emergency question such as a recipe or homework, possibly while camping).
- `expect`: the 1 to 4 guide ids that should apply. Use every guide at least 3 times across the 60.
- Vary people: ages, alone or groups, hikers, drivers, boaters, hunters, campers, parents.
- No two conversations with the same situation. Plain ASCII quotes.

Write the file to OUTFILE (valid UTF-8 JSON) and reply with just "done N" where N is the number of conversations.
