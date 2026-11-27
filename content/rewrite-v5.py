# Guide library v5 (26 Nov 2026): disaster and home steps rewritten shorter, one condition each, condition first.
# The source page and checked quote of every step stay the same; only the paraphrase changes.
# Why: on sealed sets 14-15 most unsafe replies came from these guides (the AI dropped an "If ..." or mixed steps).
import io, json
P = 'android/app/src/main/assets/guides.json'
NEW = {
 'earthquake': [
  "Drop onto your hands and knees where you are.",
  "Cover your head and neck with your arm, under a sturdy table or next to an inside wall, away from windows.",
  "Hold on until the shaking stops.",
  "If you're inside during the shaking, stay inside and away from doorways.",
  "If you're outside, move to an open area away from buildings, trees, streetlights and power lines.",
  "If you're driving, pull over, stop and set the parking brake.",
  "Expect aftershocks, and drop, cover and hold on each time.",
  "After the shaking, if the building is damaged, get out and away from it.",
  "If you're trapped, text or bang on a pipe or wall instead of shouting.",
  "If you're near the coast, go inland or to high ground as soon as the shaking stops."],
 'tsunami': [
  "Signs of a tsunami: an earthquake, a loud roar from the sea, or the sea suddenly rising or draining away.",
  "If you see a sign or hear a warning, leave right away.",
  "Go to high ground at least 100 feet (30 m) above sea level, or at least a mile (1.6 km) inland.",
  "If you feel an earthquake, protect yourself from the shaking first, then go.",
  "If you're on a boat, head out to sea."],
 'avalanche': [
  "Stay off slopes steeper than about 30 degrees, and out from under them.",
  "If someone is buried, call for help, then search only if it's safe for you.",
  "Act fast: once an avalanche stops, the snow sets hard and buried people can't dig out.",
  "Once someone is out, warm the middle of their body first and keep them dry."],
 'hurricane': [
  "If officials tell you to evacuate, go right away.",
  "For high winds, shelter in a storm shelter or an inside room.",
  "If flooding traps you, go to the highest floor, but not into a closed attic.",
  "Don't walk, swim or drive through flood water.",
  "After the storm, stay out of flood water: downed power lines can charge it.",
  "Use texts instead of calls, and save calls for emergencies."],
 'landslide': [
  "If you're in the path of a landslide, move uphill as fast as you can.",
  "Never cross a road with water or mud flowing over it.",
  "When slides are likely, stay out of river valleys and low ground.",
  "If a stream suddenly turns muddy, a landslide may be coming.",
  "During heavy storms, stay awake: many landslide deaths happen while people sleep.",
  "After a slide, stay away from the slide area."],
 'volcano': [
  "If you're told to evacuate, go early.",
  "Stay away from areas downwind of the volcano and valleys downstream of it.",
  "If ash is falling, stay inside and seal doors, windows and vents.",
  "If you must go outside in ash, cover your nose and mouth with a dust mask or cloth.",
  "If you must drive in ash, keep the windows up and the air conditioning off."],
 'winter-storm': [
  "If a winter storm warning is out, get to shelter right away.",
  "If you're stuck in your car, stay inside it.",
  "Run generators and grills only outdoors, away from windows.",
  "If you must go outside, wear layers of warm clothing.",
  "Don't overdo it shoveling or walking in deep snow.",
  "Frostbite signs: numbness, and white, grayish-yellow, firm or waxy skin."],
 'rip-current': [
  "Stay calm: a rip current pulls you out, not under.",
  "Don't swim straight back against the current; it will wear you out.",
  "Swim along the shoreline until you're out of the pull, then angle back to shore.",
  "If you can't reach shore, float, face the shore, and call or wave for help."],
 'fell-through-ice': [
  "If you fall through, keep your winter clothes on: they trap air and help you float.",
  "Turn back toward the way you came.",
  "Put your arms on the solid ice, kick your feet, and work your way forward onto it.",
  "Once you're out, lie flat and roll away from the hole.",
  "Then get somewhere warm, dry and sheltered, and warm up right away."],
 'someone-in-water': [
  "Call for help first, and don't run to the edge.",
  "Shout to them and keep encouraging them.",
  "From a safe spot, hold out a rope, branch or ladder; if they start pulling you in, let go.",
  "Throw them a rope or something that floats.",
  "Don't go out to them yourself unless nothing else is possible.",
  "Cold water makes people gasp, so even strong swimmers can drown in it."],
 'house-fire': [
  "Get low and crawl under the smoke to your way out.",
  "If a door or its handle is hot, or smoke comes around it, keep it shut and use another way out.",
  "Open doors slowly.",
  "If you can't reach someone, get out and tell the emergency operator where they are.",
  "If you can't get out, signal at a window with a light-colored cloth or a flashlight.",
  "If your clothes catch fire, stop, drop, cover your face and roll until the fire is out."],
 'gas-leak': [
  "If you smell gas, stop what you're doing and leave right away.",
  "On the way out, don't touch light switches, phones or garage door openers.",
  "Once you're safely away, call the gas company or emergency services."],
 'power-outage': [
  "Keep the fridge and freezer closed.",
  "Run a generator only outdoors, at least 20 feet (6 m) from windows.",
  "Don't heat your home with a gas stove or oven.",
  "Unplug appliances and electronics to protect them from a power surge.",
  "If it's dangerously hot or cold, go to a place in town that has power.",
  "If medicines need a fridge or a device needs power, make a backup plan early."],
 'chemical-spill': [
  "Some toxic chemicals have no smell. Trouble breathing or stinging eyes can be a sign.",
  "If you're outside, get away fast, upwind of the source.",
  "If you're told to evacuate, go right away.",
  "If you're told to stay inside, turn off air conditioning, fans and vents.",
  "If you're staying inside, seal gaps around doors and windows with wet towels, plastic or tape.",
  "If fumes may have got in, take shallow breaths through a cloth.",
  "Don't eat or drink anything that may be contaminated."],
}
raw = io.open(P, encoding='utf-8', newline='').read()
g = json.loads(raw)
n = 0
for x in g['guides']:
    if x['id'] in NEW:
        assert len(NEW[x['id']]) == len(x['steps']), x['id']
        for s, t in zip(x['steps'], NEW[x['id']]):
            s['text'] = t; n += 1
g['version'] = 5
nl = '\r\n' if '\r\n' in raw else '\n'
out = json.dumps(g, ensure_ascii=False, indent=4).replace('\n', nl) + nl
io.open(P, 'w', encoding='utf-8', newline='').write(out)
print('rewrote', n, 'steps; version', g['version'])
