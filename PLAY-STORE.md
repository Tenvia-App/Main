# Getting Tenvia onto Google Play

A plan for later. Nothing here has been submitted yet. Google's rules change often, so re-check each item in the Play Console when we get there.

## Ready now

- **Privacy policy URL:** https://tenvia-app.github.io/Main/privacy.html (live after the next push).
- **Store listing text:** below.
- **Licence and disclaimer:** Apache 2.0 ([LICENSE](LICENSE)), [DISCLAIMER.md](DISCLAIMER.md).

## Store listing (draft)

**App name (30 characters max):** Tenvia: Offline Emergency Help

**Short description (80 characters max):**
Offline safety guides and an on-phone AI for when you're lost or out of signal.

**Full description:**

> Tenvia is a free safety app for when you're outdoors and out of signal.
>
> - **Works with no signal.** Every guide and the AI run on your phone. Nothing is sent anywhere.
> - **Checked guides** for being lost, getting found, cold, heat, water, flooding, wildfire, lightning, strong wind, tornadoes, tides, wild animals, kids and groups, a stranded vehicle and saving phone battery, plus basic first aid (bites and stings, bleeding, burns, broken bones, choking, CPR) and emergencies like earthquakes, house fires, gas leaks, falling through ice and rip currents. Each step is taken from an official government safety page, and the page is linked.
> - **AI chat on your phone.** Describe what's happening in your own words. The AI answers only from the checked guide steps, and a set of safety rules checks every reply before you see it.
> - **Free, no ads, no account, no tracking.** Open source under Apache 2.0.
>
> **Important:** Tenvia is in testing. It is not a medical device or a rescue service and cannot contact anyone for you. If you are in danger, call 911 (or your local emergency number). In the US, call or text 988 for a mental-health crisis.

**Category:** Health & Fitness or Travel & Local (Google has no "Safety" category; Travel & Local fits outdoor use and avoids health-app extra checks).

**Contact email:** Google requires one. Use a dedicated address for Tenvia (for example a new Gmail), not your personal one, because it is shown publicly.

## Data safety form (answers)

- Does the app collect or share user data? **No.**
- Is data encrypted in transit? Not applicable (nothing is sent).
- Can users request deletion? Not applicable (nothing is stored off the phone).

## Things to sort out first

1. **Developer account:** a one-time US$25 fee (after 3 December, when the budget opens up). It needs ID verification.
2. **Testing rule for new personal accounts:** Google currently asks new personal accounts to run a closed test with at least 12 testers for 14 days before the app can go public. Start collecting testers early (friends, family, Discord).
3. **App size:** the AI model is about 1.1 GB. Normal Play uploads have a much smaller limit, so the model must go in a **Play Asset Delivery** pack (or be downloaded inside the app on first start). This is the biggest code change on the list.
4. **Internet permission:** test builds use it only to talk to the AI on the same phone. Plan to run the model in-process and drop the permission before release, as the website and privacy policy promise.
5. **Foreground service permission (`FOREGROUND_SERVICE_SPECIAL_USE`):** the Play Console will ask why the app needs it. Answer: it loads the offline AI model in the background so it can reply quickly in an emergency. A short screen recording may be required.
6. **Starting at boot (`RECEIVE_BOOT_COMPLETED`):** allowed, but be ready to explain it the same way.
7. **Content rating questionnaire:** answer honestly. The app mentions injuries, wildfire and suicide-crisis lines, but has no violence or mature content. Expect a low age rating.
8. **Health/safety claims:** keep the store text free of promises ("will save your life", "rescue"). Keep the not-for-real-emergencies line while the app is in testing.
9. **Screenshots:** at least 2 phone screenshots (Home, a guide, the chat), plus a 512 x 512 icon and a 1024 x 500 feature graphic. [brand/](brand/) has the emblem.
10. **Signing:** use Play App Signing. Keep the upload key in `C:\Users\aries\tenvia-signing` backed up, outside the repo.
11. **Target SDK:** the app targets API 36, which meets the current rule.
