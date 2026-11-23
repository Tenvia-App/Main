# Install and test Tenvia (Android)

> **Test software: do not rely on it in a real emergency.** If you are in danger, call 911 (or your local emergency number). In the US, call or text 988 for a mental-health crisis. See [DISCLAIMER.md](DISCLAIMER.md).

## What you need

- An Android phone, Android 8 or newer. So far it has only been tested on one phone (a moto g stylus 2025 with 8 GB of memory). Phones with less memory may be slow or may not run the AI: with 4-5 GB the app warns you, and with 3 GB or less it doesn't start the AI on its own (you can tap **Try anyway**). The guides work on any phone.
- About **2.5 GB of free space**: the download is about 1.2 GB, and the app copies its AI model out once on first start.
- Wi-Fi for the download. After that, nothing needs a connection.

## Install, step by step

The exact wording of the screens below differs a little between phones. Plan about 10 minutes, mostly for the download.

**Step 1. Connect to Wi-Fi.** The download is about 1.2 GB.

**Step 2. Open the download page on your phone.** Go to the [releases page](https://github.com/Tenvia-App/Main/releases). The newest version is at the top.

**Step 3. Download the app.** Under **Assets** for the newest version, tap the file ending in **`.apk`** (for example `Tenvia-0.8.2-ai-test.apk`).
- Chrome may say *"File might be harmful"* or *"Download anyway?"*. Tap **Download anyway**. Chrome says this about every app that isn't from the Play Store.
- Wait for the download to finish.

**Step 4. Open the file.** Tap **Open** in the download message, or open your **Files** app, go to **Downloads** and tap the Tenvia `.apk` file.

**Step 5. Allow the install (first time only).** Android may say *"For your security, your phone isn't allowed to install unknown apps from this source."*
1. Tap **Settings**.
2. Turn on **Allow from this source**.
3. Tap the back arrow. You return to the install screen.

**Step 6. Tap Install.**
- If **Google Play Protect** says it doesn't recognize the app, tap **More details**, then **Install anyway**. It says this because Tenvia isn't on the Play Store yet.
- If it offers to **scan the app**, you can let it scan.

**Step 7. Open Tenvia.** Look for the gold icon with a walking person between two hills. Allow notifications if asked: the app shows one while it gets the AI ready.

**Step 8. Wait for the AI the first time.** The first start copies the AI onto the phone and warms it up. This can take **several minutes**. The guides work right away.

**Step 9 (optional). Turn the "unknown apps" switch back off.** Go to **Settings → Apps → Special app access → Install unknown apps**, pick the app you used (Chrome or Files) and turn it off.

**Updating later:** download the newest `.apk` and install it over the old one. You don't need to uninstall first.

## First start

- The first time, the app copies its AI model and warms it up. This can take **several minutes**. The guides work right away; the AI chat works once it's ready.
- If the chat says **"The AI model file isn't on this phone"**, go to **Settings → Apps → Tenvia → Storage → Clear storage**, then open Tenvia again. This clears an old leftover folder that can block the copy.

## What to try (10 minutes)

1. **Turn on airplane mode.** Everything should still work.
2. Open the chat and describe a situation **in your own words**, as if you were really stuck. Then send 2 or 3 follow-up messages that add details or change things. For example:
   - "lost on a trail and it's getting dark"
   - "my car slid into a ditch, it's snowing, phone at 10%"
   - "creek is rising fast and my kid wants to cross"
   - something messy with typos or slang
3. Check each reply: **is it safe, and is it actually useful?**
4. Open a guide from the home screen. Each step links to the official page it came from.

## Send a chat to help improve Tenvia

After a chat, tap **Send this chat to help improve Tenvia (testers)** above the text box. You'll see the whole chat as text and can edit it first, for example to remove names or places. **Open share menu** then lets you send it. Pick your email app: the address **tenvia.testchats@gmail.com** is filled in for you. The app never sends anything by itself. Please only share practice chats, not a real emergency.

Real practice chats are the most useful thing you can give: they show how people really write, and they help test and teach the AI.

## Report a bad answer

If the AI says something unsafe, wrong or unhelpful, please [report it here](https://github.com/Tenvia-App/Main/issues/new?template=bad-ai-answer.yml). You need a free GitHub account. Screenshots help a lot.

**Don't include your real location, phone number or other personal details.** Reports are public.
