package app.tenvia.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * TEST BUILD: open AI conversation (high tier), ported from ai-experiments/chat-v4.ps1 (chat round 2:
 * advice must match the ACTIVE guides' steps, claim/guess/invented-fact rules, no unvetted fallback lines).
 *
 * The model runs in a llama-server process on THIS phone (started by the app, see AiServer) and is
 * reached at 127.0.0.1 only. Per message:
 *  1. Safety nets: AI routing + keyword nets for medical / self-harm (fixed notices stay up).
 *  2. Checked steps of the matched guides go to the model with the message.
 *  3. Free reply, then deterministic guard rules; a rejected reply gets a repair pass (4 tries),
 *     then a fixed fallback.
 */
class ChatEngine(private val library: GuideLibrary, private val baseUrl: String = "http://127.0.0.1:8080") {

    data class Reply(val text: String, val generated: Boolean, val notice: String?, val guides: List<Guide>)

    private val guideById = library.guides.associateBy { it.id }
    private val medical = library.outOfScope.firstOrNull { it.id == "medical" }
    private val crisisOos = library.outOfScope.firstOrNull { it.id == "crisis" }
    private val negations = library.outOfScope.flatMap { it.negations }.map { it.lowercase() }
    private val medKeywords = medical?.keywords?.map { it.lowercase() } ?: emptyList()
    private val crisisKeywords = crisisOos?.keywords?.map { it.lowercase() } ?: emptyList()
    private val allSteps = library.guides.flatMap { g -> g.steps.map { it.text } }.joinToString("\n")

    // Conversation state (one conversation per engine).
    private val active = mutableListOf<String>()
    private var isMedical = false
    private var isCrisis = false
    private var hazard = false
    private var hot = false
    private var coSource = false
    private var co = false
    private val history = mutableListOf<Pair<String, String>>()
    private val prevReplies = mutableListOf<String>()
    private var curMsg = ""
    private var convUser = ""
    private var hasSignalNow = false
    private var curSteps = ""
    private var inVehicle = false

    fun reset() {
        active.clear(); isMedical = false; isCrisis = false; hazard = false; hot = false; coSource = false; co = false
        history.clear(); prevReplies.clear(); convUser = ""; hasSignalNow = false; curSteps = ""
    }

    /** True when the on-phone AI server answers. */
    fun isAvailable(): Boolean = runCatching {
        val c = URL("$baseUrl/health").openConnection() as HttpURLConnection
        c.connectTimeout = 1500; c.readTimeout = 1500
        c.responseCode == 200
    }.getOrDefault(false)

    // ---------------- keyword helper (same as Find-Keyword in the test scripts) ----------------
    private fun findKeyword(text: String, keywords: List<String>): String? {
        val tokens0 = text.lowercase().replace("'", "").split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        var phrase = " " + tokens0.joinToString(" ") + " "
        negations.forEach { phrase = phrase.replace(" $it ", " ") }
        val tokens = phrase.trim().split(' ').filter { it.isNotBlank() }
        for (kw in keywords) {
            if (' ' in kw) { if (phrase.contains(" $kw ")) return kw }
            else if (tokens.contains(kw)) return kw
            else if (kw.length >= 4 && tokens.any { it.startsWith(kw) }) return kw
        }
        return null
    }

    // ---------------- model calls ----------------
    private fun complete(prompt: String, grammar: String?, temp: Double, n: Int, seed: Int = 42): String {
        val body = JSONObject().apply {
            put("prompt", prompt); put("n_predict", n); put("temperature", temp); put("seed", seed)
            put("stop", JSONArray().put("<|im_end|>")); put("cache_prompt", true)
            if (grammar != null) put("grammar", grammar)
        }
        val c = URL("$baseUrl/completion").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 3000; c.readTimeout = 300_000
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val text = c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return JSONObject(text).getString("content")
            .replace(Regex("[‘’]"), "'").replace(Regex("[“”]"), "\"").trim()
    }

    private fun chat(sys: String, shots: List<Pair<String, String>>, user: String): String {
        val sb = StringBuilder("<|im_start|>system\n$sys<|im_end|>\n")
        shots.forEach { sb.append("<|im_start|>user\n${it.first}<|im_end|>\n<|im_start|>assistant\n${it.second}<|im_end|>\n") }
        sb.append("<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun askBool(sys: String, shots: List<Pair<String, String>>, text: String): Boolean =
        runCatching { JSONObject(complete(chat(sys, shots, text), BOOL_GRAMMAR, 0.0, 12)).getBoolean("answer") }.getOrDefault(false)

    /**
     * Reads the fixed instructions (about 3,000 tokens) before the person sends anything, so the first
     * reply doesn't wait for them. Measured on a moto g stylus 2025: about 4 minutes of the first
     * reply was this reading. The server keeps these prefixes cached and reuses them.
     */
    fun warmUp() {
        val prefixes = listOf(
            chat(HARM_SYS, HARM_SHOTS, "").substringBeforeLast("<|im_start|>user") + "<|im_start|>user\n",
            chat(HURT_SYS, HURT_SHOTS, "").substringBeforeLast("<|im_start|>user") + "<|im_start|>user\n",
            chat(ROUTE_SYS, ROUTE_SHOTS, "").substringBeforeLast("<|im_start|>user") + "<|im_start|>user\n",
            buildPrompt("", "").substringBeforeLast("<|im_start|>user") + "<|im_start|>user\n",
        )
        prefixes.forEach { p -> runCatching { complete(p, null, 0.0, 1) } }
    }

    // ---------------- one turn ----------------
    fun respond(msg: String): Reply {
        curMsg = msg
        convUser += " $msg"
        // 1. routing + safety nets
        val route = runCatching { JSONObject(complete(chat(ROUTE_SYS, ROUTE_SHOTS, msg), routeGrammar(), 0.0, 80)) }.getOrNull()
        val rGuide = route?.optString("guide") ?: "none"
        val rUrgent = route?.optBoolean("urgent") ?: false
        if (Regex("\\p{L}{2,}").containsMatchIn(msg)) {
            val ck = findKeyword(msg, crisisKeywords + CRISIS_TRIGGERS)
            val decisive = findKeyword(msg, crisisKeywords.filter { it !in listOf("hurt myself", "a burden", "self harm") })
            val med = findKeyword(msg, medKeywords)
            var thisMed = false
            if (med != null) {
                thisMed = (rGuide == "none" && rUrgent) || askBool(HURT_SYS, HURT_SHOTS, msg)
            }
            val runHarm = if (thisMed) ck != null else ck != null || rGuide == "none" || rGuide == "stay-calm"
            if (decisive != null) { isCrisis = true; thisMed = false }
            else if (runHarm && askBool(HARM_SYS, HARM_SHOTS, msg)) { isCrisis = true; thisMed = false }
            if (thisMed) isMedical = true
            library.guides.filter { g -> findKeyword(msg, g.keywords.map { it.lowercase() }) != null }.forEach { g ->
                if (g.id !in active) active.add(0, g.id)
            }
            if (rGuide != "none" && guideById.containsKey(rGuide)) { active.remove(rGuide); active.add(0, rGuide) }
        }
        val use = active.take(4)
        inVehicle = "stranded-vehicle" in use
        val steps = grounding(use)
        curSteps = steps

        // situation notes (fixed word rules, not AI). v4: lightning has its own checked guide now.
        val hazardNow = Regex("\\b(fire|wildfire|smoke|flames?|burning|evacuat\\w*|flood\\w*|water (is )?(rising|coming in|up to)|rising water|tornado|tide (is )?(coming|rising|up)\\w*|tide coming|high tide|incoming tide)\\b", RegexOption.IGNORE_CASE).containsMatchIn(msg)
        if (hazardNow) hazard = true
        // CDC carbon monoxide signs near a fuel-burning source = medical (sealed set 1, 301: "the heater is making us sleepy").
        if (Regex("\\b(heater|engine|exhaust|tailpipe|generator|stove|grill|charcoal|propane|kerosene|fumes|idling|car (is )?running|running the car)\\b", RegexOption.IGNORE_CASE).containsMatchIn(msg)) coSource = true
        if (coSource && Regex("\\b(sleepy|drowsy|so tired|can't stay awake|hard to (stay awake|wake)|won't wake|headaches?|dizz\\w*|lightheaded|weak|nause\\w*|upset stomach|throw(ing)? up|threw up|vomit\\w*|chest pain|confus\\w*|passed out|pass(ing)? out)\\b", RegexOption.IGNORE_CASE).containsMatchIn(msg)) { co = true; isMedical = true }
        if (Regex("\\b(hot|heat wave|the heat (is|was)|in (this|the) heat|overheat\\w*|desert|scorching|sun is (brutal|hot|beating)|9\\d ?(f|degrees)|1\\d\\d ?(f|degrees))\\b", RegexOption.IGNORE_CASE).containsMatchIn(msg)) hot = true
        val hasSignal = SIGNAL_RE.containsMatchIn(msg)
        hasSignalNow = hasSignal
        val ctx = mutableListOf<String>()
        if (isCrisis) ctx += "NOTE: ${crisisNote()}" else if (isMedical) ctx += "NOTE: ${medicalNote()}"
        ctx += if (steps.isNotEmpty()) "CHECKED STEPS:\n$steps" else "CHECKED STEPS: none match yet. Do not give practical advice. Kindly ask what is happening."
        if (hazard) ctx += "NOTE: There is fire, smoke, flood or a rising tide. Use the checked flood, wildfire or beach steps if they are given. Otherwise say honestly that you do not have checked advice for this, and to call 911 as soon as they have any signal. Do not tell them to stay put or to move unless a checked step says so."
        if (co) ctx += "NOTE: These can be carbon monoxide warning signs (CDC: headache, dizziness, weakness, upset stomach, vomiting, chest pain, confusion; people who are sleeping can die before they have symptoms). Say that reaching 911 is the most important thing. Do not suggest running the engine, heater or generator."
        if (hasSignal) ctx += "NOTE: The person has some signal right now. The most important thing is to call 911 now (call if you can, text if you cannot)."
        val context = ctx.joinToString("\n\n")

        // 2-4. reply + guard + repair
        val base = buildPrompt(context, msg)
        var prompt = base
        var reply: String? = null
        val med = isMedical || isCrisis
        for (attempt in 1..4) {
            var cand = runCatching { complete(prompt, null, if (attempt == 1) 0.3 else 0.6, 140, 100 + attempt) }.getOrNull() ?: break
            cand = cand.replace(Regex("(?i)\\bstep \\d+\\s*[:.)-]\\s*"), "").replace(Regex("(?m)^\\s*(\\d+[.)]|[-*])\\s+"), "").trim()
            // v4 (dev2 202.3): the model drops the altitude limit even when told 4 times; add the step's own words.
            if (r("\\bboil").containsMatchIn(cand) && !r("6,?500|3 minutes|three minutes").containsMatchIn(cand))
                cand = r("\\b(1|one) minute\\b").replaceFirst(cand, "$0 (3 minutes above 6,500 feet)")
            val bad = RULES.filter { (_, test) -> test(cand, med) }.map { it.first }
            if (bad.isEmpty()) { reply = cand; break }
            val hint = bad.joinToString(" ") { if (it == "advice not in steps") adviceHint() else FIX_HINTS[it] ?: "" }
            prompt = base + cand + "<|im_end|>\n<|im_start|>user\nRewrite your reply. Problem: $hint Keep everything else, 2 to 4 short sentences.<|im_end|>\n<|im_start|>assistant\n"
        }
        val notice = when { isCrisis -> crisisOos?.message; isMedical -> medical?.message; else -> null }
        val shownGuides = use.mapNotNull { guideById[it] }
        if (reply == null) {
            // v4: no unvetted lines. Hazard: the active hazard guide's own quoted steps; else the top guide's.
            val hz = use.firstOrNull { it in HAZARD_GUIDES && (it == use.firstOrNull() || hazardNow) }
            val first = when {
                hz != null -> guideById[hz]?.steps?.take(2)?.map { it.text } ?: emptyList()
                hazard -> emptyList()
                // Water: steps 1 and 3 ("streams can be sources" alone, without "must be treated", misleads; dev2 202.3).
                else -> use.take(1).flatMap { id ->
                    val st = guideById[id]?.steps ?: emptyList()
                    (if (id == "water" && st.size >= 3) listOf(st[0], st[2]) else st.take(2)).map { it.text }
                }
            }
            // v5 (sealed 3: 524.3, 533.3): a crisis or medical fallback is the notice's own text, never
            // another guide's steps (lost-hiker steps to someone in crisis, shade to a low blood sugar).
            var fb = when {
                isCrisis && crisisOos != null -> "I hear you, and I'm here with you. " + crisisOos.message
                isMedical && medical != null -> "I hear you. " + medical.message +
                    (if (hz != null && first.isNotEmpty()) " The checked guide says: " + first.joinToString(" ") else "")
                first.isNotEmpty() -> "I hear you. Here is what the checked guide says: " + first.joinToString(" ")
                hazard -> "I don't have checked advice for this situation. As soon as you have signal, call 911."
                else -> "I'm here. Tell me what is happening, in your own words."
            }
            if (hasSignal && r("\\b(bars?|signal|service|reception)\\b").containsMatchIn(msg) && !fb.contains("911")) fb += " You have some signal: call 911 now (call if you can, text if you cannot)."
            prevReplies += fb
            return Reply(fb, false, notice, shownGuides)
        }
        history += ("$context\n\nPerson: $msg") to reply
        prevReplies += reply
        return Reply(reply, true, notice, shownGuides)
    }

    private fun grounding(ids: List<String>): String = ids.mapNotNull { guideById[it] }.joinToString("\n") { g ->
        "${g.title}:\n" + g.steps.mapIndexed { i, s -> "  ${i + 1}. ${s.text}" }.joinToString("\n")
    }

    private fun ex(ids: List<String>, person: String, note: String = ""): String {
        val c = mutableListOf<String>()
        if (note.isNotEmpty()) c += "NOTE: $note"
        c += if (ids.isNotEmpty()) "CHECKED STEPS:\n${grounding(ids)}" else "CHECKED STEPS: none match yet."
        return c.joinToString("\n\n") + "\n\nPerson: $person"
    }

    private val examples by lazy {
        listOf(
            ex(listOf("stranded-vehicle", "cold-weather"), "stuck in snow on a back road, can i just run the heater all night?") to
                "Not all night. Before running the engine, make sure the exhaust pipe isn't blocked by snow. Then run it about 10 minutes each hour for warmth, with a window open a little to keep out carbon monoxide. Is your exhaust pipe clear right now?",
            ex(listOf("water", "heat"), "found a creek, can i drink straight from it? so thirsty") to
                "Not straight from the creek. Water from outdoor sources must be treated to kill germs first: if it's cloudy, filter it through a clean cloth, then bring it to a rolling boil for 1 minute (3 minutes above 6,500 feet). Until then, don't ration the water you have: drink what you need today. Do you have a way to boil water?",
            ex(listOf("stay-or-move"), "i can see a road way down the hill, should i just walk to it?") to
                "I don't have checked advice saying it's safe to go for it. What I can tell you is to retrace your steps only if it's safe, and if you can't safely find your way back, stay where you are. Can you still see the way you came in?",
            ex(listOf("stay-or-move", "get-found"), "theres a fast river between me and the trail, should i cross it?") to
                "I don't have checked advice on crossing rivers, so I can't tell you it's safe. If you can't safely find your way back, staying where you are is the checked advice, and if you have a whistle, blow it often so people can find you. If you get any signal, 911 can advise you.",
            ex(emptyList(), "my friend slipped and his leg is bent wrong", "Someone is hurt or ill. Do not give any treatment instructions.") to
                "I'm so sorry, that sounds frightening and painful. The most important thing now is emergency help: if there is any way to call or text 911, do it right away. I don't have checked first-aid steps for injuries. Is he awake and talking to you?",
        )
    }

    private fun buildPrompt(context: String, msg: String): String {
        val sb = StringBuilder("<|im_start|>system\n$CHAT_SYS<|im_end|>\n")
        examples.forEach { sb.append("<|im_start|>user\n${it.first}<|im_end|>\n<|im_start|>assistant\n${it.second}<|im_end|>\n") }
        sb.append("<|im_start|>user\n(New conversation with a different person starts now.)<|im_end|>\n<|im_start|>assistant\nUnderstood.<|im_end|>\n")
        history.takeLast(5).forEach { sb.append("<|im_start|>user\n${it.first}<|im_end|>\n<|im_start|>assistant\n${it.second}<|im_end|>\n") }
        sb.append("<|im_start|>user\n$context\n\nPerson: $msg<|im_end|>\n<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun medicalNote() = "Someone is hurt or ill. The app is showing this notice: \"${medical?.message}\" Acknowledge the injury or illness first. Say that reaching 911 is the most important thing whenever there is any way to. Do not give ANY treatment or first-aid instructions (no pressure, bandages, splints, ice, keeping a limb still, medicine, drinking, sleeping, moving or positioning the person)."
    private fun crisisNote() = "This person may be thinking about ending their life. The app is showing this notice: \"${crisisOos?.message}\" Be warm and present. Tell them you are glad they told you and that they matter. Gently encourage calling or texting 988, or 911, as soon as they have any signal. You may ask if they are safe right now. Do not lecture, argue, promise things will get better, or talk about methods."

    private fun routeGrammar(): String {
        val q = "\""
        val picked = library.guides.joinToString(" | ") { "$q${it.id}\\$q$q" }
        return """root ::= "{\"about\":\"" about "\",\"guide\":\"" choice "}"
about ::= [a-zA-Z ,']{3,60}
choice ::= picked ",\"urgent\":false" | "none\",\"urgent\":" bool
picked ::= $picked
bool ::= "true" | "false""""
    }

    // ---------------- guard rules (same as chat-v2.ps1) ----------------
    private fun r(p: String) = Regex(p, RegexOption.IGNORE_CASE)
    private fun sentences(s: String) = s.split(Regex("(?<=[.!?])\\s+"))
    private val NEGATION = r("\\b(don't|do not|not|never|isn't|aren't|avoid|can't tell you|no checked|don't have checked|without checked|rather than|instead of)\\b")
    private fun anySentence(s: String, p: Regex) = sentences(s).any { p.containsMatchIn(it) && !NEGATION.containsMatchIn(it) && !stepCopy(it) }
    private fun stem5(w: String) = if (w.length > 5) w.substring(0, 5) else w
    // v4 (dev2): a sentence that is a near-copy of one ACTIVE step ("if you leave your vehicle, you will become
    // disoriented quickly") is the step itself, so movement / claim / going rules don't apply to it.
    private fun stepCopy(sent: String): Boolean {
        val ws = sent.lowercase().replace("'", "").split(Regex("[^a-z0-9]+")).filter { it.length >= 4 && it !in PLAIN_WORDS }.map { stem5(it) }.toSet()
        if (ws.size < 2) return false
        return curSteps.lowercase().replace("'", "").split("\n").any { line ->
            val lw = line.split(Regex("[^a-z0-9]+")).filter { it.length >= 4 }.map { stem5(it) }.toSet()
            lw.isNotEmpty() && ws.count { it in lw }.toDouble() / ws.size >= 0.75
        }
    }

    private fun engineAdvice(s: String): Boolean = sentences(s).any { sent ->
        if (r("\\b(don't|do not|never|not)\\s+(run|keep|leave|start|idle|use|turn)\\b").containsMatchIn(sent)) false
        // v4 (dev2 203.1): "leave the engine off" / "I don't have checked advice on running the engine".
        else if (r("\\b(leave|keep|turn|switch|shut)\\b[^.!?]{0,25}\\boff\\b|\\b(engine|it|motor|heater) off\\b|checked advice|only for the cold|only in the cold").containsMatchIn(sent)) false
        // "Keep it on and ready to call" is about the phone.
        else if (r("\\b(phone|call|calls|battery|signal)\\b").containsMatchIn(sent) && !r("\\b(engine|motor|heater|car|truck|vehicle|van|suv|idl\\w*)\\b").containsMatchIn(sent)) false
        else if (r("^\\W*before (running|starting|you run|you start|turning on)\\b").containsMatchIn(sent) && !r("\\b(then|and) (run|start|keep)\\b").containsMatchIn(sent)) false
        else r("\\b(run(ning)?|keep|leave|turn(ing)? on|use)\\b[^.!?]{0,25}\\b(engine|heater|motor|car running)\\b|\\b(engine|heater)\\b[^.!?]{0,15}\\b(running|on|run)\\b|\\b(keep|leave|have) (it|the car|the truck|the vehicle|the van|the suv|your car|your truck) (running|on)\\b|\\b(car|truck|van|suv|vehicle|motor) (running|idling)\\b|\\bidl(e|ing)\\b").containsMatchIn(sent)
    }

    private fun phoneOff(s: String): Boolean = sentences(s).any { sent ->
        if (r("\\b(don't|do not|never)\\s+(turn|switch|keep|power)\\s+(it|your phone|the phone)\\s+off\\b|\\b(don't|do not|never)\\b[^.!?]{0,30}\\b(turn (it |your phone |the phone )?off|airplane mode)\\b").containsMatchIn(sent)) false
        else r("\\b(turn(ing)?|switch(ing)?|power(ing)?) (it|your phone|the phone) off\\b|\\b(turn(ing)?|switch(ing)?) off (your|the) phone\\b|\\b(keep|leave) (it|your phone|the phone) off\\b|\\bdon't (text|call|use (it|your phone))\\b|\\bairplane mode\\b").containsMatchIn(sent)
    }

    // v4 (dev2 205.1, 207.3): the no-signal-bars condition must be in the same sentence as "turn the phone off".
    private fun phoneOffSentenceNoCond(s: String): Boolean = sentences(s).any { sent ->
        if (r("\\b(don't|do not|never)\\s+(turn|switch|keep|power)\\s+(it|your phone|the phone)\\s+off\\b|\\b(don't|do not|never)\\b[^.!?]{0,30}\\b(turn (it |your phone |the phone )?off|airplane mode)\\b").containsMatchIn(sent)) false
        else r("\\b(turn(ing)?|switch(ing)?|power(ing)?) (it|your phone|the phone) off\\b|\\b(turn(ing)?|switch(ing)?|power(ing)?) off (your|the) phone\\b|\\b(keep|leave) (it|your phone|the phone) off\\b|\\bturn it off\\b").containsMatchIn(sent) &&
            !r("\\bno (signal|bars|service)\\b|\\bno signal bars\\b").containsMatchIn(sent)
    }

    private fun words(s: String) = s.lowercase().split(Regex("[^a-z]+")).filter { it.length >= 3 }.toSet()
    private fun repeats(s: String): Boolean {
        val w = words(s)
        return w.isNotEmpty() && prevReplies.any { p -> val pw = words(p); w.count { it in pw }.toDouble() / w.size >= 0.9 }
    }

    private val ADVICE_VERBS = "stay|keep|drink|move|go|walk|head|hike|climb|follow|find|seek|take|breathe|try|wait|look|use|turn|put|get|rest|sit|lie|stand|run|wave|yell|shout|signal|blow|flash|hold|cover|build|eat|boil|filter|ration|light|start|leave|open|close|check|make|avoid|stop|remove|cross|swim|wade|jump|travel|retrace|lay|spread|set|bring|carry|tie|press|apply|wrap|clean|call|text|dial|save|conserve|charge|switch|power|send|mark|note|write|watch|listen|tell|drive|push|pull|dig|wear"
    private val ALWAYS_OK = setOf("call", "text", "dial", "tell", "breathe", "let", "share", "answer", "check", "look", "see", "notice", "think")
    private val SYNONYMS = mapOf("start" to "run", "idle" to "run", "sip" to "drink", "switch" to "turn", "shelter" to "find", "protect" to "shield")
    // v4 (chat round 2): advice is checked against the ACTIVE guides' steps for this turn: the clause's
    // verb AND its content words must be in those steps (round 1: "any guide" let improvisations pass).
    private class Clause(val verb: String, val text: String)
    private fun adviceClauses(s: String): List<Clause> {
        val out = mutableListOf<Clause>()
        for (sent in sentences(s)) {
            if (Regex("\\?\\s*$").containsMatchIn(sent)) continue
            for (cl in sent.split(Regex(",\\s*|;\\s*|:\\s*|(?<=[a-z]{3})-(?=[a-z]{2,} )|\\s+-\\s+|\\s+(?:and|then|or|but)\\s+"))) {
                var c = cl.trim().lowercase().replace(Regex("[.!?\"]+$"), "")
                c = c.replace(Regex("^(first|next|then|now|also|so|for now|right now|if you can|if it's safe|if possible|please|just|instead|until then|meanwhile|in the meantime)\\s*,?\\s*"), "")
                c = c.replace(Regex("^(go|switch|put it) (in)?to (airplane|low power)"), "turn on $3")
                c = c.replace(Regex("^(?:i'd|i would) (?:suggest|recommend)(?: that you)?\\s+|^(?:the best thing (?:to do )?is to|it's best to|it's important to|it is important to|it's a good idea to|it helps to)\\s+"), "")
                Regex("^(?:don't|do not|never)\\s+(\\w+)").find(c)?.let { out += Clause(it.groupValues[1], c); continue }
                Regex("^(?:you should|you could|you can|you need to|you must|you'll want to|try to|make sure to|make sure you|be sure to)\\s+(?:not\\s+)?(\\w+)").find(c)?.let { out += Clause(it.groupValues[1], c); continue }
                Regex("^(?:your \\w+|he|she|they|everyone|the \\w+) (?:should|must|needs? to|has to|have to)\\s+(?:not\\s+)?(\\w+)").find(c)?.let { out += Clause(it.groupValues[1], c); continue }
                Regex("^($ADVICE_VERBS)\\b").find(c)?.let { out += Clause(it.groupValues[1], c) }
            }
        }
        return out.filter { it.verb !in ALWAYS_OK }
    }
    private fun verbIn(v0: String, st: String): Boolean {
        val v = SYNONYMS[v0] ?: v0
        val stem = if (v.length > 4) v.substring(0, 4) else v
        return Regex("\\b${Regex.escape(stem)}").containsMatchIn(st)
    }
    private var badClauses: List<String> = emptyList()
    private fun unsupportedClauses(s: String, steps: String): List<String> {
        val st = steps.lowercase().replace("'", "")
        return adviceClauses(s).filter { a ->
            if (st.isEmpty() || !verbIn(a.verb, st)) return@filter true
            val ws = a.text.replace("'", "").split(Regex("[^a-z0-9]+")).filter { it.length >= 4 && it != a.verb && it !in PLAIN_WORDS }
            var sup = 0; var uns = 0
            for (w0 in ws) {
                val w = WORD_SYNONYMS[w0] ?: w0
                val stem = if (w.length > 5) w.substring(0, 5) else w
                if (Regex("\\b${Regex.escape(stem)}").containsMatchIn(st)) sup++ else uns++
            }
            uns >= 2 || (uns == 1 && sup == 0)
        }.map { it.text }
    }
    private fun adviceHint(): String {
        val q = badClauses.take(3).joinToString(", ") { "\"$it\"" }
        return "It gave advice that is not in the CHECKED STEPS for this message: $q. Remove that advice. Give only advice that is in the checked steps, in their words, or say you don't have checked advice on that."
    }
    private fun claim(s: String, p: Regex) = sentences(s).any { p.containsMatchIn(it) && !r("checked advice|checked step").containsMatchIn(it) && !stepCopy(it) }
    // "You don't have a map" / "since you have signal": only when the person said so (round 1: 417.2, 419.1, 315.1).
    private fun inventedAbsence(s: String): Boolean {
        val u = convUser.lowercase().replace("'", "")
        for (m in r("(?<!\\bif )\\byou (?:don't|do not|dont) have (?:a |an |any )?(\\w+)|(?<!\\bif )\\byou have no (\\w+)|\\bwith no (signal|bars|service|map)\\b|\\bno signal bars means\\b").findAll(s)) {
            val n = (m.groupValues[1] + m.groupValues[2] + m.groupValues[3]).lowercase()
            if (n in setOf("to", "enough", "time", "much")) continue
            if (n == "checked" || n.isEmpty()) {
                if (m.value.contains("means") && !Regex("\\bno (signal|bars|service)|\\b(zero|0) bars|\\bno bars|without signal|dont have (any )?(signal|service)|lost (signal|service)|signal (is )?gone").containsMatchIn(u)) return true
                continue
            }
            val stem = Regex.escape(if (n.length > 5) n.substring(0, 5) else n)
            if (!Regex("\\b(no|dont have|do not have|without|lost|left|forgot|none|zero|0|dead|ran out of)\\b[^.!?]{0,25}\\b$stem|\\b$stem\\w*\\b[^.!?]{0,15}\\b(died|dead|is gone|gone|lost|broke|none|isnt|is not|wont)").containsMatchIn(u)) return true
        }
        return r("\\b(since|now that|because) you (have|got|'ve got) (some |a )?(signal|service|bars?|reception)\\b").containsMatchIn(s) && !hasSignalNow
    }

    private val TREATMENT = "tourniquet|cpr|splint\\w*|elevat\\w*|bandag\\w*|pressure|wrap\\w*|ice pack|ibuprofen|aspirin|tylenol|acetaminophen|medicine|medication|painkiller\\w*|antiseptic|disinfect\\w*|stitch\\w*|compress\\w*|sling|clean (the|your|it)|tie (it|the|a)|rinse"

    private val RULES: List<Pair<String, (String, Boolean) -> Boolean>> = listOf(
        "promise" to { s, _ -> r("\\b(will|going to|gonna) (find|rescue|save|come for|reach|get to|see|spot|hear) you\\b|\\bhelp is (coming|on (the|its|it's) way)|\\b(you're|you are|you'll be|you will be|everything will be|it will be|it'll be) (safe|fine|ok|okay|alright|all right)\\b|rescuers? (is|are) (coming|on)|\\b(they|someone|rescuers|searchers|people) (are|is) (looking|searching|coming) for you\\b|\\b(will|can|could) get better\\b|\\buntil (help|rescue|rescuers|someone|they) (arrives?|comes?|gets? there|finds? you)\\b|\\bwhen (help|rescue|rescuers|someone|they) (arrives?|comes?|gets? there|finds? you)\\b|\\bwhile (you )?wait(ing)? for (help|rescue|rescuers)\\b|\\bhelp (will|should) (come|arrive)\\b").containsMatchIn(s) },
        "claims to act" to { s, _ -> r("\\b(i|we)('ll| will| am going to| are going to| can)\\b.{0,20}\\b(call|contact|send|alert|notify|text|reach out to|get help|track)\\b|\\bi('ve| have) (called|contacted|alerted|notified|sent)\\b|\\bi('m| am) (calling|contacting|alerting|sending|notifying|tracking)\\b").containsMatchIn(s) },
        "treatment" to { s, _ -> r("\\b($TREATMENT)\\b").findAll(s).any { !allSteps.lowercase().contains(it.value.lowercase()) } },
        "first aid while hurt" to { s, med -> med && anySentence(s, r("\\b(rest\\w*|hydrat\\w*|cool (place|spot|down|off)|stay cool|shade|drink\\w*|sleep\\w*|lie down|sit down|fluids?)\\b|\\b(keep|hold|put|raise|rest|move|cool|ice|press|clean|sleep|lie|sit|drink|stop|slow)\\b[^.!?]{0,40}\\b(ankle|leg|arm|foot|feet|knee|wrist|head|neck|wound|cut|bleed\\w*|injur\\w*|swell\\w*|it still|still|body|yourself cool|it off)\\b")) },
        "engine without limits" to { s, _ -> engineAdvice(s) && !(r("10 minutes").containsMatchIn(s) && r("exhaust|tailpipe").containsMatchIn(sentences(s).filterNot { Regex("\\?\\s*$").containsMatchIn(it) }.joinToString(" ")) && r("window").containsMatchIn(s) && r("\\bcold\\b").containsMatchIn(s)) },
        "engine advice in heat" to { s, _ -> hot && engineAdvice(s) },
        "repeats an earlier reply" to { s, _ -> repeats(s) },
        "stay put in fire or flood" to { s, _ -> hazard && r("\\bstay (with|in|inside) (your|the) (vehicle|car|truck|van|suv|pickup)\\b|\\bstay (with|in|inside) it\\b|\\bstay inside\\b|\\bstay where you are\\b|\\bstay put\\b|\\bremain (in|with|where)\\b").containsMatchIn(s) && !r("rapidly moving water|rising inside|on the roof").containsMatchIn(s) },
        "engine with CO signs" to { s, _ -> co && engineAdvice(s) },
        "alarming speculation" to { s, _ -> r("\\b(may|might|could|will)\\b[^.!?]{0,20}\\b(rise|rising|spread|get worse|worsen|come back|flood|collapse)\\b").containsMatchIn(s) },
        "untreated water" to { s, _ -> anySentence(s, r("\\bdrink\\w*\\b[^.!?]{0,40}\\b(stream|creek|river|puddle|lake|pond|wash|spring|swamp|it|that|this water|water here)\\b")) && !r("\\b(treat\\w*|boil\\w*)\\b").containsMatchIn(s) },
        "rationing" to { s, _ -> anySentence(s, r("\\bwhen (you're|you are|you feel|you get) thirsty\\b|\\bration\\w*|\\bsips?\\b|\\bsave (it|your water|the water|the drinking water|your drinking water)\\b|\\bwhen (your |their )?mouths? (is |are |gets? |feels? )?(very |really )?dry|\\bonly when (your )?mouth|\\bwait (until|till) (you're |you are )?thirsty|\\bonly drink\\b|\\bdrink\\w* (water |it )?only (if|when)\\b|\\bshare (it|the (bottle|water))[^.!?]{0,20}carefully|\\bmake (it|the water) last\\b|\\bdrink (it |them )?(all )?(right )?now\\b|\\bdrink (all|everything)\\b")) },
        "phone off with signal" to { s, _ -> SIGNAL_RE.containsMatchIn(curMsg) && phoneOff(s) },
        "phone off without condition" to { s, _ -> (phoneOff(s) && !r("no (signal|bars)|no signal bars").containsMatchIn(s)) || phoneOffSentenceNoCond(s) },
        // v4 (dev2 judge): boiling without the altitude limit drops a condition of the step.
        "boil without altitude" to { s, _ -> r("\\bboil").containsMatchIn(s) && r("\\b(1|one) minute").containsMatchIn(s) && !r("6,?500|3 minutes|three minutes").containsMatchIn(s) },
        "movement advice" to { s, _ -> anySentence(s, r("\\b(walk\\w*|hik\\w*|head(ing)?|driv\\w*|paddl\\w*|cross\\w*|climb\\w*|swim\\w*|wad\\w*|go(ing)?|mov\\w*)\\b\\s+(to|toward|towards|down|up|across|over|for|back to|out to|along|around|downstream|upstream|higher)\\b(?!\\s+(airplane|low power|sleep))|\\bleav\\w* (the|your) (car|vehicle|truck|van|suv|shelter|island|kayak)\\b|\\bfollow\\w* (the|that) (river|stream|creek|road|lights?|dog|sound)\\b|\\bstand in the road\\b|\\bflag (it|them|the \\w+) down\\b")) },
        "phone capability claim" to { s, _ -> r("\\b(phone|it|gps)\\b[^.!?]{0,15}\\b(can't|cannot|can not|won't|will not|can|will)\\b[^.!?]{0,10}\\b(send|share|show|track|transmit|find)\\b[^.!?]{0,20}\\b(location|position|where you are)").containsMatchIn(s) },
        "safety claim" to { s, _ -> sentences(s).any { sent -> !r("checked advice|hard-topped|can't (tell|say)|not (safe|sure it's safe)|isn't safe|no way to know").containsMatchIn(sent) && r("(?<!\\bif )\\b(staying|waiting|being|it's|that's|this is|you're|you are|is) (perfectly |completely |totally )?(safe|safer|the safest)\\b(?! to)").containsMatchIn(sent) } },
        "entertains going" to { s, _ -> claim(s, r("\\bif you (do )?(go|walk|leave|head out|decide to (go|walk|leave)|try to (walk|go|reach))\\b|\\bbefore you (go|walk|leave|head out)\\b|\\bon your way\\b")) },
        "wrong speaker" to { s, _ -> r("\\byou (don't|do not) have (any )?checked advice").containsMatchIn(s) },
        // v4 (round 1 judge reasons): claims and guesses that no step makes.
        "danger claim" to { s, _ -> claim(s, r("\\b(is|are|it's|that's|could be|may be|might be|can be|would be|isn't|is not|not)\\s+(very |too |really )?(dangerous|unsafe|risky|safe enough)\\b|\\b(could|may|might|will|would) (make (it|things|this) worse|hurt you|be dangerous)\\b|\\b(won't|will not|doesn't|does not) (warm|help|work|mean)\\b|\\bincreases? (the )?risk\\b|(?<!\\bif )\\byou (can't|cannot|can not) safely\\b|\\b(can't|cannot) (cross|reach|get to|walk|go|climb)\\b[^.!?]{0,20}\\bsafely\\b|\\bno matter how\\b")) },
        "guessing" to { s, _ -> r("\\b(it|that|this) (sounds like|could be|might be|may be|is probably|is likely)\\b(?! (dangerous|unsafe|risky))|\\bcould mean\\b|\\b(probably|likely) (from|because|means)\\b|\\bit could be from\\b|\\b(don't|do not|doesn't|does not) apply\\b").containsMatchIn(s) },
        "invents a fact" to { s, _ -> inventedAbsence(s) },
        // v4 (dev2 208.1, 214.4): the stay-put step has a condition; staying put is never called okay.
        "stay put without condition" to { s, _ -> !inVehicle && sentences(s).any { r("\\b(stay where you are|stay put|staying (where you are|put|here))\\b").containsMatchIn(it) && !r("\\bif you (can't|cannot|can not)\\b|\\bunless\\b|\\bchanging location\\b").containsMatchIn(it) } },
        "calls it normal" to { s, _ -> r("\\b(is|are|that's|it's|feels?) (completely |totally |perfectly |very )?(normal|natural|expected)\\b").containsMatchIn(s) },
        "too long" to { s, _ -> s.split(Regex("\\s+")).count { it.isNotBlank() } > 100 },
        "advice not in steps" to { s, _ -> badClauses = unsupportedClauses(s, curSteps); badClauses.isNotEmpty() },
        "empty" to { s, _ -> s.isBlank() },
    )

    companion object {
        private val SIGNAL_RE = Regex("\\b(one|1|a|two|2|some|few) bars?\\b|\\bbar (just )?(popped|appeared|showed)|\\b(have|got|getting) (a |some )?(signal|service|reception)\\b|\\b(signal|service) (is |came |just came )?back\\b|\\b(buzzing|getting) (with )?alerts\\b", RegexOption.IGNORE_CASE)
        private val HAZARD_GUIDES = setOf("flood", "wildfire", "coast-tide", "lightning", "high-wind")
        // Words that carry no advice of their own (so they need not appear in a step). Same as chat-v4.ps1.
        private val PLAIN_WORDS = setOf("this", "that", "these", "those", "with", "your", "yours", "them", "they", "their", "there", "here", "what", "when", "where", "which", "while",
            "will", "would", "could", "should", "from", "into", "onto", "about", "just", "still", "very", "really", "some", "more", "most", "much", "many", "even", "also",
            "than", "then", "each", "every", "back", "like", "sure", "best", "right", "okay", "until", "before", "after", "because", "have", "been", "being", "were",
            "other", "others", "thing", "things", "something", "anything", "possible", "able", "dont", "its", "youre", "please", "help", "yourself", "yourselves",
            "everyone", "anyone", "someone", "people", "person", "phone", "signal", "call", "text", "time", "times", "little", "long", "first", "once",
            "least", "well", "good", "away", "only", "around", "close", "area", "place", "spot", "current", "currently", "already", "keep", "stay", "make", "calm",
            "calmly", "safely", "instead", "both", "again", "directly", "fully", "soon", "need", "want", "tell", "told", "tells", "often", "down", "family", "group")
        private val WORD_SYNONYMS = mapOf("truck" to "vehicle", "car" to "vehicle", "suv" to "vehicle", "van" to "vehicle", "kids" to "children", "child" to "children",
            "sun" to "shade", "jacket" to "clothing", "coat" to "clothing", "figure" to "work", "visible" to "see", "tying" to "tie",
            "toddler" to "children", "baby" to "children", "son" to "children", "daughter" to "children", "untreated" to "treated", "clothes" to "clothing")
        private const val BOOL_GRAMMAR = "root ::= \"{\\\"answer\\\":\" (\"true\" | \"false\") \"}\""

        private val FIX_HINTS = mapOf(
            "promise" to "It promised or predicted an outcome (for example \"until help arrives\" or \"things will get better\"). Say nothing about what will happen.",
            "claims to act" to "It said the app would contact someone. The app cannot contact anyone.",
            "treatment" to "It gave medical treatment advice. Give none.",
            "first aid while hurt" to "It gave first-aid advice about the injury or illness. Give none: only say reaching 911 is the most important thing.",
            "engine without limits" to "It talked about running the engine without all the limits. Either leave the engine out, or say all of: only in the cold, check the exhaust pipe is not blocked, run it about 10 minutes each hour, with a window open a little.",
            "engine with CO signs" to "They have carbon monoxide warning signs. Do not suggest running the engine, heater or generator; say reaching 911 is the most important thing.",
            "engine advice in heat" to "It gave the engine-for-warmth step, which is only for the cold. It is hot, so leave the engine out.",
            "repeats an earlier reply" to "It repeated an earlier reply. Respond to what they just said, with something new.",
            "stay put in fire or flood" to "There is fire, smoke, flood or a rising tide. Do not tell them to stay with the vehicle or stay where they are unless a checked step says so. Use the checked steps, or say you do not have checked advice for this, and to call 911 as soon as they have any signal.",
            "alarming speculation" to "It guessed that things could get worse. Do not speculate.",
            "untreated water" to "It talked about drinking outdoor water without saying it must be treated (boiled) first.",
            "rationing" to "It suggested rationing or saving water. The checked step says: do not ration your drinking water; drink what you need today.",
            "phone off with signal" to "They have some signal, so turning the phone off or airplane mode is wrong: as soon as you have signal, call 911 (call if you can, text if you cannot).",
            "phone off without condition" to "It said to turn the phone off or use airplane mode without the condition: only when there are no signal bars.",
            "movement advice" to "It suggested walking, driving, crossing or going somewhere. Do not. Say you do not have checked advice saying that is safe, and give the step that fits (stay with your vehicle, or stay where you are if you cannot safely find your way back).",
            "phone capability claim" to "It claimed what the phone can or cannot do with location. Do not; just say to tell 911 where they are as exactly as they can when they have signal.",
            "safety claim" to "It said something is safe. The app cannot know that; do not call anything safe.",
            "entertains going" to "It talked as if they might go or walk somewhere. Do not; give the step that fits (stay with your vehicle, or stay where you are if you cannot safely find your way back).",
            "wrong speaker" to "Say \"I don't have checked advice\" (you, the app), not \"you don't have checked advice\".",
            "stay put without condition" to "Keep the condition of the step: say \"If you can't safely find your way back, stay where you are.\" Do not say to stay put without it, and do not call staying put okay or safe.",
            "calls it normal" to "It called their symptoms or feelings normal. Do not.",
            "too long" to "It was too long. Use 2 to 4 short sentences.",
            "advice not in steps" to "It gave advice that is not in the checked steps. Use only the checked steps.",
            "danger claim" to "It claimed something is dangerous, unsafe, or will or will not work. The app cannot judge that. Say you do not have checked advice on it, and give only the checked steps.",
            "boil without altitude" to "It said to boil water for 1 minute without the limit in the checked step: above 6,500 feet, boil it for 3 minutes.",
            "guessing" to "It guessed what something means or what is causing it. Do not guess; say only what the checked steps say.",
            "invents a fact" to "It stated something about their situation they did not say (for example that they have or do not have a map, signal or landmarks). Only use what they told you, or ask.",
            "empty" to "It was empty.",
        )

        private val CRISIS_TRIGGERS = listOf("trapped", "no way out", "no solution", "unbearable", "cant take", "cant go on", "gave away", "giving away", "give away",
            "goodbye", "say bye", "my will", "affairs", "death", "dying", "die", "a plan", "my plan", "stockpil", "pills", "burden", "better off", "better without",
            "hopeless", "no reason", "no point", "pointless", "guilt", "shame", "disappear", "never come back", "not come back", "come back from", "tired of", "end it", "over with",
            "keep myself safe", "seguir", "morir", "wake up tomorrow", "not wake up", "make it stop", "make this stop", "all of this stop", "easier if", "easiest")

        private const val CHAT_SYS = """You are Tenvia, a calm helper inside an emergency app on a phone with NO internet. You cannot call, text, contact, alert, send or track anyone, and nobody else can see this conversation.
Talk like a calm, kind expert sitting next to the person. Write 2 to 4 short sentences (under 70 words) in plain words, like a text message. No lists or headings.
How to answer:
- If they asked a question, answer it directly first (for example "Not all night." or "I don't have checked advice on that.").
- Practical advice must come ONLY from the CHECKED STEPS. Do not add your own do's and don'ts or general survival tips.
- When you use a step, keep ALL of its conditions and limits (if..., before..., about 10 minutes each hour, a window open a little, must be treated first, only when there are no signal bars). Never shorten a step in a way that drops a condition.
- Never suggest walking, hiking, driving, paddling, crossing water, or leaving a vehicle or shelter to go somewhere. If they ask about going somewhere, say you don't have checked advice saying it is safe, then give the step that fits (stay with your vehicle; if you can't safely find your way back, stay where you are).
- Use only the CHECKED STEPS given for THIS message. If they don't cover something (for example crossing a river, building something, tents, food), say honestly that you don't have checked advice on it, and that if they get any signal, 911 can advise them.
- You may end with ONE short new question. Never repeat a question.
- Never promise or predict anything: not "help is coming", "until help arrives", "you'll be okay", "things will get better", "they will see you".
- Never say you are calling, contacting or alerting anyone.
- Never give medical or first-aid advice of any kind. If someone is hurt or ill, say reaching 911 is the most important thing, any way they can.
- Do not invent facts they did not say, and do not call their symptoms or feelings normal.
- Suggesting they call or text 911 (or 988) as soon as they have signal is always okay."""

        private const val ROUTE_SYS = """You sort messages for Tenvia, an emergency app on a phone with no internet. Read the message, write a short summary, then choose.

Guides:
stay-or-move: lost or unsure of the way back (trail, path, direction, shore, campground)
get-found: searchers, rescuers, helicopters, boats or patrol are near and the person wants to be seen or heard
cold-weather: cold, wet, wind, rain, snow, sleet, shivering, numb or soaked
heat: heat, sun, no shade, overheating, sunburn
water: drinking water: running out, finding it, making it safe
phone-battery: phone battery low or dying
reach-help: signal, service or SOS just appeared, or asking how to call or text 911 / emergency services / responders
stranded-vehicle: car, truck, van, bus, RV or other vehicle stuck, broken down or stalled
stay-calm: fear, panic, overwhelmed, racing thoughts, can't think
flood: flood water, rising water, water coming into the car or over the road
wildfire: wildfire, smoke, flames, orange glow, evacuation
lightning: thunder, lightning, thunderstorm
high-wind: strong wind, gusts, tent blowing or bending, falling trees or branches, power lines down
coast-tide: beach, ocean, tide, waves, cliffs, coast
wildlife: bear, mountain lion, snake, coyote, moose or another wild animal nearby
kids-group: children, older people or a group the person is responsible for

urgent = true ONLY if someone is injured, ill, bleeding, unconscious, not breathing, choking, having chest pain, a stroke, a seizure, an allergic reaction or asthma attack, swallowed something harmful, has heat stroke signs (hot dry skin, confusion, cannot walk straight), has carbon monoxide signs (headache, nausea or dizziness near a generator, heater, stove or engine), or wants to harm or kill themselves. When urgent is true, guide is none.
Having signal, wanting to call 911, being lost, cold, hot, thirsty, stuck or scared is NOT urgent on its own.
If the message is not about an emergency (greetings, jokes, questions, tests, nonsense), guide is none and urgent is false.
If two guides fit, choose the most pressing one."""
        private val ROUTE_SHOTS = listOf(
            "cant find the way back to my car, all the trees look the same" to """{"about":"lost, cannot find way back","guide":"stay-or-move","urgent":false}""",
            "i see a drone flying over the valley, how do i get it to notice me" to """{"about":"searchers nearby, wants to be seen","guide":"get-found","urgent":false}""",
            "got 2 bars finally!!! who do i call" to """{"about":"signal appeared, wants to call for help","guide":"reach-help","urgent":false}""",
            "my brother twisted his knee badly and cant walk, we are lost" to """{"about":"brother injured, cannot walk","guide":"none","urgent":true}""",
            "phone at 3 percent and no service" to """{"about":"phone battery almost dead","guide":"phone-battery","urgent":false}""",
            "whats a good pizza place" to """{"about":"not an emergency","guide":"none","urgent":false}""",
            "i just want it all to end, i might do something to myself" to """{"about":"thinking of self harm","guide":"none","urgent":true}""",
        )
        private const val HURT_SYS = """Answer one question about the message: is a PERSON (not an animal, object, phone or vehicle) injured, ill, poisoned, unconscious, struggling to breathe, or having a medical problem right now? Reply {"answer":true} or {"answer":false}."""
        private val HURT_SHOTS = listOf(
            "the fence post is broken and a branch fell on the trail" to """{"answer":false}""",
            "my wrist is swollen and bent after I fell off the rocks" to """{"answer":true}""",
            "my phone battery is swelling and getting hot" to """{"answer":false}""",
            "we saw a wounded deer, now we cannot find the path" to """{"answer":false}""",
            "my son is breathing weird and his lips look blue" to """{"answer":true}""",
            "I am not hurt, just shaking and scared after the storm" to """{"answer":false}""",
            "out in the desert, my skin is hot and dry and I feel confused and wobbly" to """{"answer":true}""",
            "we all have headaches and feel sick, the heater has been running in the tent" to """{"answer":true}""",
            "I am confused which trail goes back to the parking lot" to """{"answer":false}""",
            "the generator ran out of gas and now the cabin is cold" to """{"answer":false}""",
        )
        private const val HARM_SYS = """Answer one question: does this message suggest the person may be thinking about suicide or harming themselves, directly or indirectly?
Warning signs (NIMH, SAMHSA): talking about wanting to die or kill themselves; feeling trapped or that there are no solutions; unbearable emotional pain; giving away important possessions; saying goodbye; putting affairs in order; talking or thinking about death often; making a plan or stockpiling pills; feeling like a burden; feeling hopeless or having no reason to live; withdrawing.
These are NOT self-harm: being physically trapped (in a car, under debris, by water); panic, fear or feeling overwhelmed during a disaster; physical medical emergencies (injury, poisoning by accident, breathing problems, illness); worrying about dying from a physical danger.
Reply {"answer":true} or {"answer":false}."""
        private val HARM_SHOTS = listOf(
            "trapped in my truck in the snow and the door wont open" to """{"answer":false}""",
            "everyone would honestly be better off if I stayed out here" to """{"answer":true}""",
            "I already said goodbye to my sister, none of it matters now" to """{"answer":true}""",
            "no way out of this canyon until the water goes down, im scared" to """{"answer":false}""",
            "whats the weather like tomorrow" to """{"answer":false}""",
            "I wrote letters for my kids and left them on the table" to """{"answer":true}""",
            "im panicking so bad, everyone is screaming and I cant think straight" to """{"answer":false}""",
            "my friend breathed in fumes and now he is dizzy and coughing" to """{"answer":false}""",
            "the toddler swallowed something from under the sink and is drowsy" to """{"answer":false}""",
            "I keep jumping at every sound in this dark house, I am so on edge" to """{"answer":false}""",
            "my oxygen machine stopped when the power went out and I cant get enough air" to """{"answer":false}""",
            "I threw up blood twice and feel faint" to """{"answer":false}""",
        )
    }
}
