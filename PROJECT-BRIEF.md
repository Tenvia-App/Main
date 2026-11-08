# Ground: offline AI companion

## Project handoff

This brief is context for discussing and developing the concept with Claude. Treat the product as an early idea, not a validated or deployment-ready system. Separate known requirements from assumptions and proposals.

## Concept

A mobile AI personality intended to help people think through life-threatening situations when they have no network connection. The intended interaction is free-form: people should be able to explain what is happening in their own words, rather than being limited to preset replies.

The intended work is primarily the mobile app. There may also be some video generation for a Twitch channel; that is a separate workstream and is outside this app brief.

## Current status

The attached `index.html` is a visual proof of concept. It shows a free-text interaction, a proposed early-development test plan, and an illustrative terrain graphic.

The page does not connect to an AI model, run offline inference, or give emergency guidance. The terrain graphic is decorative and is not a map. The page is not suitable for real emergencies or field use. It currently loads p5.js from a CDN, so the visual mockup itself also depends on an internet connection.

No safety, reliability, or efficacy results have been established. The test plan below is proposed work, not completed testing.

## Product intent

- Work without a network connection.
- Let users describe a situation in their own words.
- Offer a steady AI personality suited to high-stress moments.
- Be developed and evaluated carefully before any field use is considered.

The personality, supported situations, target users, devices, languages, and safety boundaries are not yet specified. Do not treat details from the visual mockup as final product decisions.

## Proposed early-development test plan

Keep these as four distinct categories. Run all participant work in controlled, clearly labeled simulations, never during real emergencies.

### 1. Verify offline behavior

Test the intended model and core interaction in airplane mode. Check availability, startup and response time, stability, battery use, and whether any feature unexpectedly depends on connectivity.

### 2. Run controlled emergency simulations

Use scripted, facilitator-led scenarios, such as getting disoriented, an injury, or losing shelter. Observe how the system handles incomplete information, asks follow-up questions, communicates uncertainty, and points toward real help when appropriate. Record confusing or unsafe outputs and when a human should take over.

### 3. Probe safety boundaries

Have qualified safety, medical, and outdoor professionals review scenarios and failures. Test ambiguity, conflicting information, overconfidence, and requests the system should not answer. Define escalation rules, stop conditions, and changes required before another round.

### 4. Study comprehension and reliance

Invite participants into non-emergency role-play. Check whether they understand the assistant, can correct it, recognize its limits, and know when not to rely on it. Record accessibility barriers, trust calibration, and points of confusion.

## Evidence for potential testers and public-sector partners

A bounded pilot could produce a documented test protocol, offline-operation logs, scenario outcomes, reviewed failure cases, and go/no-go criteria. The goal is to give partners evidence they can assess, including limitations and failures, rather than claim the system is already safe or effective.

## Safety and communication constraints

- Do not present the concept as proven, reliable, medically approved, or ready for field deployment.
- Do not test during real emergencies. Use supervised simulations with qualified reviewers.
- Make clear when a demo has no model connected and provides no guidance.
- Report failures and uncertainty alongside successful tests.
- Do not invent performance targets or results; define them with relevant experts before testing.

## Open questions for product development

1. Who is the primary user, and in what environments would they use the app?
2. Which situations are in scope for the first prototype, and which must be explicitly out of scope?
3. What mobile platform and minimum hardware constraints apply to offline inference?
4. Which languages must work offline, and how will model updates and safety content be handled?
5. What should the assistant do when it is uncertain, the user cannot provide details, or outside help is unavailable?
6. What data, if any, is stored on the device, and how can the user control it?
7. Which qualified reviewers and test partners should help define scenarios, success criteria, and stop conditions?

## Suggested next task for Claude

Help turn this concept into a product requirements document and a risk-aware prototype plan. First list assumptions and unresolved decisions. Then propose an offline architecture and a staged evaluation plan that respects the safety constraints above. Keep the four test categories separate, distinguish proposals from evidence, and avoid writing operational emergency instructions or making claims of safety or efficacy.