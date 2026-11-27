# Expert review of Tenvia's guide steps

Tenvia is a free, open-source, offline Android app that helps people through emergencies when there's no signal. Every step it shows comes from an official government page and stores an exact supporting quote, but **no qualified person has reviewed the steps yet**. This page is what we send to reviewers: a short letter, the specific questions, and the full list of steps.

## The letter (to send)

> **Subject:** Could you review the first-aid steps in a free offline safety app? (about 1-2 hours)
>
> Hello,
>
> My name is Aries Fulkerson. I'm building Tenvia, a free, open-source Android app for people in danger with no phone signal: lost hikers, stranded drivers, people caught in a flood, fire or earthquake, and people helping someone who is hurt. It has no ads, accounts or tracking, and it is still a test version that is clearly labelled "not for real emergencies".
>
> Every instruction in the app is a short paraphrase of an official government source (for example Ready.gov, the CDC, NHS, healthdirect Australia), and each one keeps the exact sentence it came from, so it can be checked. What it doesn't have yet is a review by someone qualified. That's what I'm asking for.
>
> Would you be willing to read through the first-aid steps (about 115 short steps, 16 topics) and mark anything that is wrong, unclear, out of date, in the wrong order, or dangerous if someone follows it alone without training? There are also a few specific questions below. Comments in any form are welcome: a reply email, notes on a printout, or a short call.
>
> This is unpaid volunteer work for now. If you agree, I'd like to name you as a reviewer on the project's page (or keep you anonymous if you prefer). Nothing you send will be published without your OK.
>
> The full list of steps, with links to every source, is here: https://github.com/Tenvia-App/Main/blob/main/content/EXPERT-REVIEW.md
>
> Thank you for considering it.
>
> Aries Fulkerson
> Tenvia, https://tenvia-app.github.io/Main/
> Reply to: tenvia.testchats@gmail.com

**Who to ask:** first-aid instructors (Red Cross, St John Ambulance, St John New Zealand, Royal Life Saving), paramedics, emergency or wilderness-medicine doctors and nurses, search-and-rescue volunteers, and for the disaster and boat steps, emergency-management or coast-guard volunteers. One reviewer per area is a good start.

## Specific questions

**First aid**
1. **Snake bite:** the US (CDC) step says to wash the bite; the Australian step says don't wash it (venom on the skin helps identify the snake) and to use a pressure bandage. The app shows only the one for the country the person names, and the US one when no country is named. Is that the right default, and is the Australian pressure-bandage wording correct?
2. **Bleeding:** the home-made tourniquet step. Should an untrained person be told this at all, and if so, with what limits?
3. **Frostbite:** "don't thaw it if it may freeze again". Is that clear and safe for someone still outdoors?
4. **Stroke:** "call an ambulance rather than drive". What should someone do where no ambulance can reach them?
5. **CPR:** the steps are shown only when the person says someone isn't breathing or isn't responding. Are the compression-first order and the 30:2 wording right for untrained bystanders?

**Disasters, water and boats**
6. **Earthquake:** "If you're inside during the shaking, stay inside" and "After the shaking, if the building is damaged, get out". Do these two read clearly together?
7. **Boat trouble** (new, from Australian state and Transport Canada boating pages): is "stay with your boat, even if it capsizes" and "don't swim to shore unless it's very close" right for most small boats? Anything critical missing (anchoring, out of fuel)?
8. **Someone in the water / through the ice:** the app separates self-rescue steps from steps for someone helping another person. Is anything in the helper steps risky?

**Crisis lines**
9. Outside the US, the crisis notice gives the emergency number plus Lifeline (Australia, 13 11 14), Samaritans (UK, 116 123) or 1737 (New Zealand). Europe has no single line, so it gives 112. Is that right?

## How the app uses these steps

The app never writes its own advice. A small AI on the phone picks the most relevant checked steps for what the person typed and joins them into a short reply, and a set of rules rejects any sentence that isn't from the checked steps. Medical and crisis messages always show a fixed notice first: reach emergency services if there is any way to.

## First-aid steps (115 steps, 16 topics)

### Snake bite

1. Treat every snake bite as dangerous. It can be hard to tell which snakes are venomous, so don't wait to find out. ([healthdirect.gov.au](https://www.healthdirect.gov.au/snake-bites))
2. Get medical help as soon as you can. Don't wait for symptoms to appear. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-snakes.html))
3. Get away from the snake. Don't try to catch or kill it. ([healthdirect.gov.au](https://www.healthdirect.gov.au/snake-bites))
4. Keep as still and calm as you can. Lie or sit down with the bitten part in a comfortable, neutral position. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-snakes.html))
5. Take off rings, watches and anything tight near the bite before it swells. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-snakes.html))
6. Don't cut the bite and don't try to suck the venom out. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-snakes.html))
7. Don't use a tourniquet, ice, or alcohol. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-snakes.html))
8. Don't drive yourself if someone else can, or if help can come to you. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-snakes.html))
9. US (CDC): wash the bite with soap and water and cover it with a clean, dry dressing. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-snakes.html))
10. US (CDC): mark the edge of the swelling and write down the time, so helpers can see if it spreads. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-snakes.html))
11. Australia: don't wash the bite. Venom left on the skin and clothes can help doctors work out which snake it was. ([healthdirect.gov.au](https://www.healthdirect.gov.au/snake-bites))
12. Australia: put a firm pressure bandage over the bite, then bandage the whole limb from the fingers or toes upward, and keep it still with a splint. ([healthdirect.gov.au](https://www.healthdirect.gov.au/snake-bites))
13. If the person stops responding or isn't breathing normally, start CPR if you know how. ([healthdirect.gov.au](https://www.healthdirect.gov.au/snake-bites))
14. Stay with the bitten person. ([healthdirect.gov.au](https://www.healthdirect.gov.au/snake-bites))

### Broken bone or bad sprain

1. Keep the injured person still and calm. ([medlineplus.gov](https://medlineplus.gov/ency/article/000001.htm))
2. Don't move someone who may have hurt their neck or back, unless they're in danger where they are. ([medlineplus.gov](https://medlineplus.gov/ency/article/000001.htm))
3. Don't try to straighten the bone or push it back into place. (The only exception: blood flow past the injury looks cut off and no trained help is near.) ([medlineplus.gov](https://medlineplus.gov/ency/article/000001.htm))
4. If the skin is broken, cover the wound with a clean, dry cloth. If it keeps bleeding, press firmly on it. Don't use a tourniquet unless the bleeding is life-threatening. ([medlineplus.gov](https://medlineplus.gov/ency/article/000001.htm))
5. To make a splint, use something stiff, like a rolled-up newspaper or strips of wood, and tie it so the joints above and below the break can't move. ([medlineplus.gov](https://medlineplus.gov/ency/article/000001.htm))
6. For an arm, a towel can work as a sling. ([nhs.uk](https://www.nhs.uk/conditions/broken-arm-or-wrist/))
7. Take off rings, watches and bracelets on that limb before it swells. ([nhs.uk](https://www.nhs.uk/conditions/broken-arm-or-wrist/))
8. Check the skin past the injury, like fingers or toes. If it's pale, blue, cold or numb, blood flow may be cut off, so get help fast. ([medlineplus.gov](https://medlineplus.gov/ency/article/000001.htm))
9. Try not to move a leg that may be broken, and don't put weight on it. ([nhs.uk](https://www.nhs.uk/conditions/broken-leg/))

### Bad bleeding

1. Press hard right on the wound with a clean cloth, a piece of clothing, or your hand if there's nothing else. ([medlineplus.gov](https://medlineplus.gov/ency/article/000045.htm))
2. Keep pressing until the bleeding stops. Don't lift the cloth to check. ([medlineplus.gov](https://medlineplus.gov/ency/article/000045.htm))
3. If blood soaks through, don't take the cloth off. Put another one on top and keep pressing. ([medlineplus.gov](https://medlineplus.gov/ency/article/000045.htm))
4. Lay the person down, and raise the bleeding part if you can. ([medlineplus.gov](https://medlineplus.gov/ency/article/000045.htm))
5. If something is stuck in the wound, like a stick or knife, leave it in. Pad around it and hold it in place. ([medlineplus.gov](https://medlineplus.gov/ency/article/000045.htm))
6. Only if pressure hasn't stopped life-threatening bleeding from an arm or leg: tie a tourniquet 2 to 3 inches (5 to 7 cm) above the wound, not on a joint. ([medlineplus.gov](https://medlineplus.gov/ency/article/000045.htm))
7. To make a tourniquet: wrap a wide strip of cloth around the limb several times, tie a knot, put a stick on the knot, tie it again, and twist the stick until the bleeding stops. Then fix the stick in place. ([medlineplus.gov](https://medlineplus.gov/ency/article/000045.htm))
8. Write down or remember the time you put the tourniquet on, and tell rescuers. ([medlineplus.gov](https://medlineplus.gov/ency/article/000045.htm))
9. For heavy bleeding, also watch for shock: lay them flat, raise their feet about 12 inches (30 cm), and keep them warm with a coat or blanket. Don't raise the feet if the head, neck, back or legs are hurt. ([medlineplus.gov](https://medlineplus.gov/ency/article/000045.htm))

### Burns

1. If someone's clothes are on fire: stop, drop and roll. Wrap them in a thick coat or blanket to put out the flames. ([medlineplus.gov](https://medlineplus.gov/ency/article/000030.htm))
2. If you're not sure how bad the burn is, treat it as a serious one and get help. ([medlineplus.gov](https://medlineplus.gov/ency/article/000030.htm))
3. For a small burn with unbroken skin, run cool (not icy) water over it for at least 5 minutes, up to 30. ([medlineplus.gov](https://medlineplus.gov/ency/article/000030.htm))
4. Cover the burn loosely with a clean, dry cloth or bandage. ([medlineplus.gov](https://medlineplus.gov/ency/article/000030.htm))
5. Don't put butter, oil, ice, creams or home remedies on a serious burn. ([medlineplus.gov](https://medlineplus.gov/ency/article/000030.htm))
6. Don't pop blisters, and don't pull off clothing that's stuck to the burn. ([medlineplus.gov](https://medlineplus.gov/ency/article/000030.htm))
7. Don't put a large, serious burn in cold water. It can cause shock. ([medlineplus.gov](https://medlineplus.gov/ency/article/000030.htm))
8. If the burn came from electricity, don't touch the person until they're away from the power. Use something that isn't metal to move them off the wire. ([medlineplus.gov](https://medlineplus.gov/ency/article/000030.htm))

### Choking

1. Ask: "Are you choking? Can you speak?" If they're coughing hard and can talk, let them keep coughing. ([medlineplus.gov](https://medlineplus.gov/ency/article/000049.htm))
2. If they can't speak or breathe, give 5 back blows, then 5 abdominal thrusts, and repeat until it comes out. ([medlineplus.gov](https://medlineplus.gov/ency/article/000049.htm))
3. Back blows: stand behind them, lean them forward, and hit firmly between the shoulder blades with the heel of your hand. ([medlineplus.gov](https://medlineplus.gov/ency/article/000049.htm))
4. Abdominal thrusts: wrap your arms around their waist, put your fist just above the belly button, grab it with your other hand, and pull in and up hard. ([medlineplus.gov](https://medlineplus.gov/ency/article/000049.htm))
5. If the person is pregnant or very large, push on the middle of the breastbone instead of the belly. ([medlineplus.gov](https://medlineplus.gov/ency/article/000049.htm))
6. If they pass out, lay them on a firm surface, shout for help, and start CPR. Look in the mouth for the object before each breath. ([medlineplus.gov](https://medlineplus.gov/ency/article/000049.htm))
7. Don't reach into the throat to grab something stuck there. Only take it out if you can see it loose in the mouth. ([medlineplus.gov](https://medlineplus.gov/ency/article/000049.htm))

### Not breathing: CPR

1. Tap or gently shake them and shout "Are you OK?" ([medlineplus.gov](https://medlineplus.gov/ency/article/000013.htm))
2. If they don't respond, shout for help and get someone to call emergency services. If you're alone, call yourself if you have any signal. ([medlineplus.gov](https://medlineplus.gov/ency/article/000013.htm))
3. Lay them on their back on a firm, flat surface. ([medlineplus.gov](https://medlineplus.gov/ency/article/000013.htm))
4. Put the heel of one hand in the center of the chest, between the nipples, with your other hand on top. ([medlineplus.gov](https://medlineplus.gov/ency/article/000013.htm))
5. With straight arms, push down hard and fast about 2 inches (5 cm), 30 times. Let the chest come all the way back up each time. ([medlineplus.gov](https://medlineplus.gov/ency/article/000013.htm))
6. Then tilt the head back, lift the chin, pinch the nose, and give 2 breaths that make the chest rise. ([medlineplus.gov](https://medlineplus.gov/ency/article/000013.htm))
7. Keep going: 30 pushes, then 2 breaths, about 100 to 120 pushes a minute, until they recover or help arrives. ([medlineplus.gov](https://medlineplus.gov/ency/article/000013.htm))
8. If there's an AED (heart defibrillator) nearby, use it as soon as you can. ([medlineplus.gov](https://medlineplus.gov/ency/article/000013.htm))
9. If they start breathing again, roll them onto their side (the recovery position). ([medlineplus.gov](https://medlineplus.gov/ency/article/000013.htm))

### Severe allergic reaction

1. This is an emergency. Get help right away. ([medlineplus.gov](https://medlineplus.gov/ency/article/000844.htm))
2. If they have an epinephrine injector (like an EpiPen), help them use it right away. ([medlineplus.gov](https://medlineplus.gov/ency/article/000844.htm))
3. A very hoarse or whispery voice, or rough noises when breathing in, are warning signs that the throat is swelling. ([medlineplus.gov](https://medlineplus.gov/ency/article/000844.htm))
4. If it was a bee sting, scrape the stinger off with a fingernail or card. Don't squeeze it with tweezers. ([medlineplus.gov](https://medlineplus.gov/ency/article/000844.htm))
5. Don't give anything by mouth if they're having trouble breathing, and don't put a pillow under their head. ([medlineplus.gov](https://medlineplus.gov/ency/article/000844.htm))
6. If they're not hurt and not vomiting, lay them flat, raise their feet about 12 inches (30 cm), and keep them warm. ([medlineplus.gov](https://medlineplus.gov/ency/article/000844.htm))
7. If they stop breathing, start CPR. ([medlineplus.gov](https://medlineplus.gov/ency/article/000844.htm))

### Head injury

1. Get help fast if they're very sleepy, confused, have a bad headache, vomit more than once, have a seizure, can't move an arm or leg, or were knocked out even briefly. ([medlineplus.gov](https://medlineplus.gov/ency/article/000028.htm))
2. If they're unconscious but breathing, treat it as a neck injury too: hold their head still, in line with the body. ([medlineplus.gov](https://medlineplus.gov/ency/article/000028.htm))
3. Stop bleeding by pressing a clean cloth on it, unless the skull might be broken. Then just cover it, without pressing. ([medlineplus.gov](https://medlineplus.gov/ency/article/000028.htm))
4. If they start to vomit, roll their head, neck and body together onto their side, keeping the head still. ([medlineplus.gov](https://medlineplus.gov/ency/article/000028.htm))
5. Don't move them unless you have to, and don't shake someone who seems dazed. ([medlineplus.gov](https://medlineplus.gov/ency/article/000028.htm))
6. Don't take off a helmet if the head injury might be serious. ([medlineplus.gov](https://medlineplus.gov/ency/article/000028.htm))
7. Keep watching them. Signs of a head injury can show up later. ([medlineplus.gov](https://medlineplus.gov/ency/article/000028.htm))

### Seizure

1. Stay calm and stay with them. ([cdc.gov](https://www.cdc.gov/epilepsy/first-aid-for-seizures/index.html))
2. Ease them to the ground and move away anything that could hurt them. ([cdc.gov](https://www.cdc.gov/epilepsy/first-aid-for-seizures/index.html))
3. Turn them gently onto one side, mouth pointing down, to keep the airway clear. ([cdc.gov](https://www.cdc.gov/epilepsy/first-aid-for-seizures/index.html))
4. Put something soft under their head, take off glasses, and loosen anything tight around the neck. ([cdc.gov](https://www.cdc.gov/epilepsy/first-aid-for-seizures/index.html))
5. Don't hold them down, and don't put anything in their mouth. ([cdc.gov](https://www.cdc.gov/epilepsy/first-aid-for-seizures/index.html))
6. Time it. Get emergency help if it lasts more than 5 minutes, another one follows, they're hurt, it happens in water, or they have trouble breathing or waking up. ([cdc.gov](https://www.cdc.gov/epilepsy/first-aid-for-seizures/index.html))
7. Afterward, help them sit somewhere safe, and tell them calmly what happened. ([cdc.gov](https://www.cdc.gov/epilepsy/first-aid-for-seizures/index.html))

### Heart attack signs

1. Signs of a heart attack: chest pain or pressure that lasts more than a few minutes or comes back, pain in the jaw, neck, back, arm or shoulder, shortness of breath, feeling faint, or a cold sweat. ([cdc.gov](https://www.cdc.gov/heart-disease/about/heart-attack.html))
2. Get emergency help right away. The sooner it's treated, the less damage to the heart. ([cdc.gov](https://www.cdc.gov/heart-disease/about/heart-attack.html))
3. If they collapse and stop breathing normally, start CPR, and use an AED if there is one. ([cdc.gov](https://www.cdc.gov/heart-disease/about/heart-attack.html))

### Stroke signs

1. Check quickly. Face: ask them to smile. Does one side droop? Arms: ask them to raise both. Does one drift down? Speech: ask them to repeat a simple phrase. Is it slurred or strange? ([cdc.gov](https://www.cdc.gov/stroke/signs-symptoms/index.html))
2. Other sudden signs: trouble walking or with balance, trouble seeing, numbness on one side, confusion, or a severe headache. ([cdc.gov](https://www.cdc.gov/stroke/signs-symptoms/index.html))
3. If you see any of these, get emergency help right away. Every minute counts. ([cdc.gov](https://www.cdc.gov/stroke/signs-symptoms/index.html))
4. Write down or remember the time the signs started. Doctors need it. ([cdc.gov](https://www.cdc.gov/stroke/signs-symptoms/index.html))
5. Where an ambulance can come, call one rather than driving: crews start treatment on the way. ([cdc.gov](https://www.cdc.gov/stroke/signs-symptoms/index.html))

### Heat stroke and heat exhaustion

1. Heat stroke is the most serious kind: confusion, slurred speech, passing out, a seizure, or very hot skin. It can kill, so get emergency help. ([cdc.gov](https://www.cdc.gov/niosh/heat-stress/about/illnesses.html))
2. Move them to shade or somewhere cool and take off outer clothing. ([cdc.gov](https://www.cdc.gov/niosh/heat-stress/about/illnesses.html))
3. Cool them fast: wet their skin, put cold wet cloths on the head, neck, armpits and groin, soak their clothes with cool water, and fan them. ([cdc.gov](https://www.cdc.gov/niosh/heat-stress/about/illnesses.html))
4. Stay with them until help comes. ([cdc.gov](https://www.cdc.gov/niosh/heat-stress/about/illnesses.html))
5. Heat exhaustion (headache, nausea, dizziness, weakness, heavy sweating, thirst): get out of the heat, take off extra clothing, and take frequent sips of cool water. ([cdc.gov](https://www.cdc.gov/niosh/heat-stress/about/illnesses.html))

### Hypothermia: too cold

1. Warning signs: shivering, exhaustion, confusion, fumbling hands, memory loss, slurred speech, or drowsiness. Someone with hypothermia may not notice it themselves. ([cdc.gov](https://www.cdc.gov/winter-weather/prevention/index.html))
2. It can happen above 40°F (4°C) too, if you're wet from rain, sweat or cold water. ([cdc.gov](https://www.cdc.gov/winter-weather/prevention/index.html))
3. Get them into shelter, out of the wind, and take off wet clothing. ([cdc.gov](https://www.cdc.gov/winter-weather/prevention/index.html))
4. Warm the middle of the body first: chest, neck, head and groin. Skin-to-skin contact under loose, dry blankets or clothes works. ([cdc.gov](https://www.cdc.gov/winter-weather/prevention/index.html))
5. Warm drinks can help if they're awake. No alcohol, and nothing to drink if they're unconscious. ([cdc.gov](https://www.cdc.gov/winter-weather/prevention/index.html))
6. Once they're warmer, keep them dry and wrapped up, including the head and neck. ([cdc.gov](https://www.cdc.gov/winter-weather/prevention/index.html))

### Frostbite

1. Check for hypothermia first and treat that before the frostbite. ([medlineplus.gov](https://medlineplus.gov/ency/article/000057.htm))
2. Get out of the cold. Take off wet clothes and tight rings or jewelry. ([medlineplus.gov](https://medlineplus.gov/ency/article/000057.htm))
3. Don't thaw it if it might freeze again. Refreezing does more damage, so wait until you're somewhere you can stay warm. ([medlineplus.gov](https://medlineplus.gov/ency/article/000057.htm))
4. If help is far away, soak the area in warm (never hot) water, about 104 to 108°F (40 to 42°C), for 15 to 30 minutes. It may burn and swell as it warms. ([medlineplus.gov](https://medlineplus.gov/ency/article/000057.htm))
5. Don't rub it, and don't warm it with a fire, heater or hair dryer. The skin can burn without you feeling it. ([medlineplus.gov](https://medlineplus.gov/ency/article/000057.htm))
6. Afterward, cover it with dry dressings, keep frostbitten fingers or toes apart, and move them as little as you can. ([medlineplus.gov](https://medlineplus.gov/ency/article/000057.htm))

### Spider bites, ticks and stings

1. Spider bite: stay calm, wash it with soap and water, and put a cold, wet cloth on it. Raise it if you can. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-spiders.html))
2. Spider bite: don't try to get the venom out. Get medical help, and if it's safe, note what the spider looked like. ([cdc.gov](https://www.cdc.gov/niosh/outdoor-workers/about/venomous-spiders.html))
3. Tick: take it off as soon as you can. With fine tweezers or your fingers, grip it as close to the skin as you can. ([cdc.gov](https://www.cdc.gov/ticks/after-a-tick-bite/index.html))
4. Tick: pull straight out with steady pressure. Don't twist or jerk it, and don't crush it with your fingers. ([cdc.gov](https://www.cdc.gov/ticks/after-a-tick-bite/index.html))
5. Tick: clean the bite and your hands afterward, and check for more ticks. If a rash or fever comes in the days or weeks after, see a doctor. ([cdc.gov](https://www.cdc.gov/ticks/after-a-tick-bite/index.html))
6. Bee sting: scrape the stinger off with a fingernail or card. Don't squeeze it with tweezers. ([medlineplus.gov](https://medlineplus.gov/ency/article/000844.htm))
7. If swelling spreads to the face or throat, or breathing gets hard after any bite or sting, treat it as a severe allergic reaction. ([medlineplus.gov](https://medlineplus.gov/ency/article/000844.htm))

### Carbon monoxide

1. If a CO alarm goes off, don't look for the cause. Go outside into fresh air right away. ([cpsc.gov](https://www.cpsc.gov/Safety-Education/Safety-Education-Centers/Carbon-Monoxide-Information-Center/Carbon-Monoxide-Questions-and-Answers))
2. Do the same if you have a headache, dizziness, weakness, sick stomach or confusion near something that burns fuel. CO has no smell or color. ([cpsc.gov](https://www.cpsc.gov/Safety-Education/Safety-Education-Centers/Carbon-Monoxide-Information-Center/Carbon-Monoxide-Questions-and-Answers))
3. Call for help from outside. Don't go back in. People can pass out and die if they stay. ([cpsc.gov](https://www.cpsc.gov/Safety-Education/Safety-Education-Centers/Carbon-Monoxide-Information-Center/Carbon-Monoxide-Questions-and-Answers))
4. Never run a generator inside a home, garage, tent or cabin, even with doors and windows open. ([cdc.gov](https://www.cdc.gov/carbon-monoxide/about/index.html))
5. Never burn charcoal or use a camp stove or fuel heater indoors or in a tent. ([cdc.gov](https://www.cdc.gov/carbon-monoxide/about/index.html))
6. Never heat a home with a gas oven or stove. ([cdc.gov](https://www.cdc.gov/carbon-monoxide/about/index.html))

## Disaster, home, water and boat steps (102 steps)

Rewritten shorter on 26 November 2026, one condition each.

### Boat trouble on open water

1. Get everyone into a lifejacket now. ([nsw.gov.au](https://www.nsw.gov.au/driving-boating-and-transport/waterways-safety-and-rules/emergencies/capsizing-and-swamping))
2. If you have a marine radio, make a distress call on VHF channel 16. ([transport.wa.gov.au](https://transport.wa.gov.au/marine/recreational-boating/boating-emergencies/what-to-do))
3. If your radio isn't working, use a phone if you have any signal. ([nsw.gov.au](https://www.nsw.gov.au/driving-boating-and-transport/waterways-safety-and-rules/emergencies/capsizing-and-swamping))
4. When you reach anyone, tell them what's wrong and where you are. ([tc.canada.ca](https://tc.canada.ca/en/marine-transportation/marine-safety/taking-water))
5. If you have an EPIRB or distress beacon, switch it on and leave it on until rescuers tell you to turn it off. ([handbook.safetransport.vic.gov.au](https://handbook.safetransport.vic.gov.au/boating-essentials/emergency-procedures/abandoning-vessel/))
6. If water is coming in, bail or pump it out. ([tc.canada.ca](https://tc.canada.ca/en/marine-transportation/marine-safety/taking-water))
7. Find where the water is coming in, and try to stop or slow the leak. ([tc.canada.ca](https://tc.canada.ca/en/marine-transportation/marine-safety/taking-water))
8. Stay with your boat, even if it capsizes: it's easier for rescuers to find. ([nsw.gov.au](https://www.nsw.gov.au/driving-boating-and-transport/waterways-safety-and-rules/emergencies/capsizing-and-swamping))
9. Don't try to swim to shore unless it's very close and there's a safe place to land. ([handbook.safetransport.vic.gov.au](https://handbook.safetransport.vic.gov.au/boating-essentials/emergency-procedures/abandoning-vessel/))
10. Make yourself as easy to see as you can for boats and aircraft. ([handbook.safetransport.vic.gov.au](https://handbook.safetransport.vic.gov.au/boating-essentials/emergency-procedures/abandoning-vessel/))
11. If you have flares, save them until you can see an aircraft or another boat. ([handbook.safetransport.vic.gov.au](https://handbook.safetransport.vic.gov.au/boating-essentials/before-you-go-boating/raising-the-alarm-calling-for-help/))
12. If you're in the water, keep your lifejacket on and huddle together. ([handbook.safetransport.vic.gov.au](https://handbook.safetransport.vic.gov.au/boating-essentials/emergency-procedures/abandoning-vessel/))
13. Put on more clothes if you can, to help keep warm. ([nsw.gov.au](https://www.nsw.gov.au/driving-boating-and-transport/waterways-safety-and-rules/emergencies/capsizing-and-swamping))

### Earthquake

1. Drop onto your hands and knees where you are. ([ready.gov](https://www.ready.gov/earthquakes))
2. Cover your head and neck with your arm, under a sturdy table or next to an inside wall, away from windows. ([ready.gov](https://www.ready.gov/earthquakes))
3. Hold on until the shaking stops. ([ready.gov](https://www.ready.gov/earthquakes))
4. If you're inside during the shaking, stay inside and away from doorways. ([ready.gov](https://www.ready.gov/earthquakes))
5. If you're outside, move to an open area away from buildings, trees, streetlights and power lines. ([ready.gov](https://www.ready.gov/earthquakes))
6. If you're driving, pull over, stop and set the parking brake. ([ready.gov](https://www.ready.gov/earthquakes))
7. Expect aftershocks, and drop, cover and hold on each time. ([ready.gov](https://www.ready.gov/earthquakes))
8. After the shaking, if the building is damaged, get out and away from it. ([ready.gov](https://www.ready.gov/earthquakes))
9. If you're trapped, text or bang on a pipe or wall instead of shouting. ([ready.gov](https://www.ready.gov/earthquakes))
10. If you're near the coast, go inland or to high ground as soon as the shaking stops. ([ready.gov](https://www.ready.gov/earthquakes))

### Tsunami

1. Signs of a tsunami: an earthquake, a loud roar from the sea, or the sea suddenly rising or draining away. ([ready.gov](https://www.ready.gov/tsunamis))
2. If you see a sign or hear a warning, leave right away. ([ready.gov](https://www.ready.gov/tsunamis))
3. Go to high ground at least 100 feet (30 m) above sea level, or at least a mile (1.6 km) inland. ([ready.gov](https://www.ready.gov/tsunamis))
4. If you feel an earthquake, protect yourself from the shaking first, then go. ([ready.gov](https://www.ready.gov/tsunamis))
5. If you're on a boat, head out to sea. ([ready.gov](https://www.ready.gov/tsunamis))

### Avalanche

1. Stay off slopes steeper than about 30 degrees, and out from under them. ([ready.gov](https://www.ready.gov/avalanche))
2. If someone is buried, call for help, then search only if it's safe for you. ([ready.gov](https://www.ready.gov/avalanche))
3. Act fast: once an avalanche stops, the snow sets hard and buried people can't dig out. ([ready.gov](https://www.ready.gov/avalanche))
4. Once someone is out, warm the middle of their body first and keep them dry. ([ready.gov](https://www.ready.gov/avalanche))

### Hurricane or typhoon

1. If officials tell you to evacuate, go right away. ([ready.gov](https://www.ready.gov/hurricanes))
2. For high winds, shelter in a storm shelter or an inside room. ([ready.gov](https://www.ready.gov/hurricanes))
3. If flooding traps you, go to the highest floor, but not into a closed attic. ([ready.gov](https://www.ready.gov/hurricanes))
4. Don't walk, swim or drive through flood water. ([ready.gov](https://www.ready.gov/hurricanes))
5. After the storm, stay out of flood water: downed power lines can charge it. ([ready.gov](https://www.ready.gov/hurricanes))
6. Use texts instead of calls, and save calls for emergencies. ([ready.gov](https://www.ready.gov/hurricanes))

### Landslide or mudslide

1. If you're in the path of a landslide, move uphill as fast as you can. ([ready.gov](https://www.ready.gov/landslides-debris-flow))
2. Never cross a road with water or mud flowing over it. ([ready.gov](https://www.ready.gov/landslides-debris-flow))
3. When slides are likely, stay out of river valleys and low ground. ([ready.gov](https://www.ready.gov/landslides-debris-flow))
4. If a stream suddenly turns muddy, a landslide may be coming. ([ready.gov](https://www.ready.gov/landslides-debris-flow))
5. During heavy storms, stay awake: many landslide deaths happen while people sleep. ([ready.gov](https://www.ready.gov/landslides-debris-flow))
6. After a slide, stay away from the slide area. ([ready.gov](https://www.ready.gov/landslides-debris-flow))

### Volcano and ash

1. If you're told to evacuate, go early. ([ready.gov](https://www.ready.gov/volcanoes))
2. Stay away from areas downwind of the volcano and valleys downstream of it. ([ready.gov](https://www.ready.gov/volcanoes))
3. If ash is falling, stay inside and seal doors, windows and vents. ([ready.gov](https://www.ready.gov/volcanoes))
4. If you must go outside in ash, cover your nose and mouth with a dust mask or cloth. ([ready.gov](https://www.ready.gov/volcanoes))
5. If you must drive in ash, keep the windows up and the air conditioning off. ([ready.gov](https://www.ready.gov/volcanoes))

### Winter storm or blizzard

1. If a winter storm warning is out, get to shelter right away. ([ready.gov](https://www.ready.gov/winter-weather))
2. If you're stuck in your car, stay inside it. ([ready.gov](https://www.ready.gov/winter-weather))
3. Run generators and grills only outdoors, away from windows. ([ready.gov](https://www.ready.gov/winter-weather))
4. If you must go outside, wear layers of warm clothing. ([ready.gov](https://www.ready.gov/winter-weather))
5. Don't overdo it shoveling or walking in deep snow. ([ready.gov](https://www.ready.gov/winter-weather))
6. Frostbite signs: numbness, and white, grayish-yellow, firm or waxy skin. ([ready.gov](https://www.ready.gov/winter-weather))

### House fire

1. Get low and crawl under the smoke to your way out. ([ready.gov](https://www.ready.gov/home-fires))
2. If a door or its handle is hot, or smoke comes around it, keep it shut and use another way out. ([ready.gov](https://www.ready.gov/home-fires))
3. Open doors slowly. ([ready.gov](https://www.ready.gov/home-fires))
4. If you can't reach someone, get out and tell the emergency operator where they are. ([ready.gov](https://www.ready.gov/home-fires))
5. If you can't get out, signal at a window with a light-colored cloth or a flashlight. ([ready.gov](https://www.ready.gov/home-fires))
6. If your clothes catch fire, stop, drop, cover your face and roll until the fire is out. ([ready.gov](https://www.ready.gov/home-fires))

### Gas leak

1. If you smell gas, stop what you're doing and leave right away. ([kcc.ks.gov](https://www.kcc.ks.gov/natural-gas-pipeline-safety-liquid-pipeline/natural-gas-safety))
2. On the way out, don't touch light switches, phones or garage door openers. ([kcc.ks.gov](https://www.kcc.ks.gov/natural-gas-pipeline-safety-liquid-pipeline/natural-gas-safety))
3. Once you're safely away, call the gas company or emergency services. ([kcc.ks.gov](https://www.kcc.ks.gov/natural-gas-pipeline-safety-liquid-pipeline/natural-gas-safety))

### Chemical spill or toxic gas

1. Some toxic chemicals have no smell. Trouble breathing or stinging eyes can be a sign. ([ready.gov](https://www.ready.gov/hazmat))
2. If you're outside, get away fast, upwind of the source. ([ready.gov](https://www.ready.gov/hazmat))
3. If you're told to evacuate, go right away. ([ready.gov](https://www.ready.gov/hazmat))
4. If you're told to stay inside, turn off air conditioning, fans and vents. ([ready.gov](https://www.ready.gov/hazmat))
5. If you're staying inside, seal gaps around doors and windows with wet towels, plastic or tape. ([ready.gov](https://www.ready.gov/hazmat))
6. If fumes may have got in, take shallow breaths through a cloth. ([ready.gov](https://www.ready.gov/hazmat))
7. Don't eat or drink anything that may be contaminated. ([ready.gov](https://www.ready.gov/hazmat))

### Caught in a rip current

1. Stay calm: a rip current pulls you out, not under. ([weather.gov](https://www.weather.gov/safety/ripcurrent-surviving))
2. Don't swim straight back against the current; it will wear you out. ([weather.gov](https://www.weather.gov/safety/ripcurrent-surviving))
3. Swim along the shoreline until you're out of the pull, then angle back to shore. ([weather.gov](https://www.weather.gov/safety/ripcurrent-surviving))
4. If you can't reach shore, float, face the shore, and call or wave for help. ([weather.gov](https://www.weather.gov/safety/ripcurrent-surviving))

### Fell through ice

1. If you fall through, keep your winter clothes on: they trap air and help you float. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/survival.html))
2. Turn back toward the way you came. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/survival.html))
3. Put your arms on the solid ice, kick your feet, and work your way forward onto it. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/survival.html))
4. Once you're out, lie flat and roll away from the hole. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/survival.html))
5. Then get somewhere warm, dry and sheltered, and warm up right away. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/survival.html))

### Someone is in the water

1. Call for help first, and don't run to the edge. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/saving.html))
2. Shout to them and keep encouraging them. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/saving.html))
3. From a safe spot, hold out a rope, branch or ladder; if they start pulling you in, let go. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/saving.html))
4. Throw them a rope or something that floats. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/saving.html))
5. Don't go out to them yourself unless nothing else is possible. ([dnr.state.mn.us](https://www.dnr.state.mn.us/safety/ice/saving.html))
6. Cold water makes people gasp, so even strong swimmers can drown in it. ([weather.gov](https://www.weather.gov/safety/coldwater))

### Power outage

1. Keep the fridge and freezer closed. ([ready.gov](https://www.ready.gov/power-outages))
2. Run a generator only outdoors, at least 20 feet (6 m) from windows. ([ready.gov](https://www.ready.gov/power-outages))
3. Don't heat your home with a gas stove or oven. ([ready.gov](https://www.ready.gov/power-outages))
4. Unplug appliances and electronics to protect them from a power surge. ([ready.gov](https://www.ready.gov/power-outages))
5. If it's dangerously hot or cold, go to a place in town that has power. ([ready.gov](https://www.ready.gov/power-outages))
6. If medicines need a fridge or a device needs power, make a backup plan early. ([ready.gov](https://www.ready.gov/power-outages))

### Emergency numbers outside the US

1. Australia: call Triple Zero (000). ([infrastructure.gov.au](https://www.infrastructure.gov.au/media-communications/phone/triple-zero))
2. Australia: on a mobile you can also dial 112. It reaches the same 000 service. ([infrastructure.gov.au](https://www.infrastructure.gov.au/media-communications/phone/triple-zero))
3. Australia: you can't text 000, so call when you get any signal. ([infrastructure.gov.au](https://www.infrastructure.gov.au/media-communications/phone/triple-zero))
4. UK: call 999. ([nhs.uk](https://www.nhs.uk/conditions/broken-leg/))
5. Europe: call 112. It works free across the EU, and on most mobile networks worldwide. ([digital-strategy.ec.europa.eu](https://digital-strategy.ec.europa.eu/en/policies/112))
6. New Zealand: call 111 and ask for the service you need. ([police.govt.nz](https://www.police.govt.nz/contact-us/111-police-emergency))
7. Canada: call 911 for police, fire or ambulance. ([parks.canada.ca](https://parks.canada.ca/voyage-travel/securite-safety/urgence-emergency))

### Crisis lines outside the US

1. Australia crisis line: call Lifeline on 13 11 14, any time. ([healthdirect.gov.au](https://www.healthdirect.gov.au/mental-health-helplines))
2. UK crisis line: call Samaritans on 116 123. ([nhs.uk](https://www.nhs.uk/nhs-services/mental-health-services/where-to-get-urgent-help-for-mental-health/))
3. New Zealand crisis line: text 1737, or call 0800 1737 1737, to talk to a counsellor. ([healthnz.govt.nz](https://www.healthnz.govt.nz/health-topics/mental-health/where-to-get-help))

The other guides (lost, getting found, cold, heat, water, phone battery, vehicle, flood, wildfire, lightning, wind, tides, wild animals, kids and groups) are in [guides.json](../android/app/src/main/assets/guides.json).
