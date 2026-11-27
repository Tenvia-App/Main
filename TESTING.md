# Testing Tenvia (test version 0.9.2-ai-test)

Thanks for helping test **Tenvia**, a free offline app meant to help people through dangerous situations when there's no signal. This version has an AI chat that runs fully on your phone.

> **WARNING: This is test software. Do not rely on it in a real emergency.** If you are in danger, call or text 911 (or your local emergency number). In the US, call or text 988 for a mental-health crisis. Nothing has been reviewed by medical or rescue experts yet, and the AI can be wrong. See [DISCLAIMER.md](DISCLAIMER.md).

## Android: install and try it

1. **Install** with the step-by-step guide: [INSTALL.md](INSTALL.md). You need Android 8 or newer, about 2.5 GB free, and Wi-Fi for the download.
2. **Turn on airplane mode.** Everything should still work.
3. **Pretend you're in trouble.** Open the chat and describe a made-up situation in your own words. Then send 2 or 3 follow-up messages that add details or change things. For example: lost on a trail at dusk, car stuck in snow, a creek rising, phone almost dead. New in 0.9.0: first aid (a snake bite, a broken leg, a burn, someone choking) and disasters at home or in town (earthquake, house fire, gas smell, flood, ice). New in 0.9.2: boat trouble at sea or on a lake. Please try those too, and tell us which country you are in. Typos and slang are welcome; write the way you really would.
4. **Send the chat.** Tap **Send this chat to help improve Tenvia (testers)**. Check the text, remove anything personal, then tap **Open share menu** and pick your email app. The address **tenvia.testchats@gmail.com** is filled in for you. The app never sends anything by itself.

Several short chats about different situations help more than one long one.

## iPhone: send a made-up first message

Tenvia only runs on Android for now. You can still help: email **tenvia.testchats@gmail.com** with the subject **iPhone tester**, and write the first message you would type if you were really stuck (a few lines, in your own words). If you like, add one or two follow-up messages you would send next. We run them through the AI and use them to test it.

## What to tell us

Please add a short note to your email about anything that went wrong on your phone:

- **Install:** did any step not match what your phone showed? Where did you get stuck?
- **First start:** did the AI get ready? About how long did it take? Did you see an error such as "The AI model file isn't on this phone"?
- **Speed:** about how long did a reply take?
- **Send this chat:** did the button work, and did your email app open with the address filled in?
- **Answers:** was any reply unsafe, wrong, or not useful? Which one, and why?
- **Your phone:** the model (for example "Pixel 7") and Android version, if you know them.

Screenshots help a lot. To report a bad answer in public instead, use the [bad AI answer form](https://github.com/Tenvia-App/Main/issues/new?template=bad-ai-answer.yml) (needs a free GitHub account; reports are public).

**Please don't include your real location, phone number or other personal details.** Only share practice chats, never a real emergency.

## Not in this version yet

- Only English. Emergency numbers for the US and Canada (911), Australia (000), New Zealand (111), the UK (999) and Europe (112) when you name the country; crisis lines are US and Canada only (988).
- Tested on only one phone so far (8 GB of memory). Phones with less memory may be slow or may not run the AI: the app warns on 4-5 GB phones and asks before starting the AI on 3 GB or less. If you see that message, please tell us your phone model.
- The test build asks for internet permission only to talk to the AI running on the same phone and, when the phone is online, to check GitHub for a newer test version. The finished app is planned to drop it.

## Getting new versions

From 0.8.3 on, when your phone is online, the home screen shows **New test version** when there's a newer build. Tap **Download**, then install the new file over the old one. You don't need to uninstall first, and your app stays set up. It's about 1 GB, so use Wi-Fi. The app never downloads or installs anything by itself.
