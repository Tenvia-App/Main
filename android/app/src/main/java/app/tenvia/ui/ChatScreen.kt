package app.tenvia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tenvia.data.TenviaAi
import app.tenvia.AiService
import androidx.compose.ui.platform.LocalContext
import app.tenvia.data.ChatEngine
import app.tenvia.data.Guide
import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import app.tenvia.data.AiServer
import app.tenvia.data.ChatExport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bubble = RoundedCornerShape(14.dp)

/** Where testers send chats (filled into the email's To line). Public Tenvia test address. */
const val TESTER_CHAT_EMAIL = "tenvia.testchats@gmail.com"

/** TESTER FEATURE: the chat as plain text, for the tester to send through the share menu if they choose. */
private fun buildShareText(context: Context, state: ChatUiState): String {
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return ChatExport.text(state.lines.map { ChatExport.Turn(it.fromPerson, it.text) }, version, AiServer.MODEL, state.notice, date)
}

data class ChatLine(val fromPerson: Boolean, val text: String, val note: String = "")

/** Chat state kept outside the screen, so opening a guide and coming back keeps the conversation. */
class ChatUiState {
    val lines = mutableStateListOf<ChatLine>()
    var notice by mutableStateOf<String?>(null)
    var guides by mutableStateOf<List<Guide>>(emptyList())
    fun clear() { lines.clear(); notice = null; guides = emptyList() }
}

/** TEST BUILD: free AI conversation, talking to the AI server running on this phone. */
@Composable
fun ChatScreen(engine: ChatEngine, state: ChatUiState, onOpenGuide: (Guide) -> Unit, onBack: () -> Unit) {
    val lines = state.lines
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var shareText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // The AI is loaded and warmed up by AiService (at phone start-up, app update and app open).
    val context = LocalContext.current
    val aiStatus by TenviaAi.status
    var startTry by remember { mutableStateOf(0) }
    LaunchedEffect(startTry) {
        if (aiStatus == TenviaAi.Status.Off || aiStatus == TenviaAi.Status.Failed || startTry > 0) AiService.start(context)
    }
    val aiUp: Boolean? = when (aiStatus) { TenviaAi.Status.Ready, TenviaAi.Status.Warming -> true; TenviaAi.Status.Failed -> false; else -> null }
    val warming = aiStatus == TenviaAi.Status.Warming
    LaunchedEffect(lines.size, busy) { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1 + if (busy) 1 else 0) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.fillMaxWidth().border(1.dp, Warning, Bubble).padding(10.dp)) {
            Text("AI TEST BUILD: NOT FOR REAL EMERGENCIES", color = Warning, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                when (aiUp) {
                    null -> "Starting the on-phone AI. This can take up to a minute..."
                    true -> if (warming) "The AI is getting ready (a few minutes). You can type now; your first reply comes after it is ready. It can be wrong." else "The AI runs on this phone, no internet. Replies take about 1-2 minutes. It can be wrong."
                    false -> "The on-phone AI couldn't start. ${TenviaAi.server.lastProblem} Use the guides instead, or tap Retry."
                },
                style = MaterialTheme.typography.bodyMedium, color = Muted,
            )
        }
        state.notice?.let {
            Column(Modifier.fillMaxWidth().background(ForestLight, Bubble).border(2.dp, Warning, Bubble).padding(12.dp)) {
                Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lines.isEmpty()) item {
                Text("Tell me what's happening, in your own words.", style = MaterialTheme.typography.bodyLarge, color = Muted)
            }
            items(lines) { l ->
                Column(
                    Modifier.fillMaxWidth()
                        .padding(start = if (l.fromPerson) 40.dp else 0.dp, end = if (l.fromPerson) 0.dp else 40.dp)
                        .background(if (l.fromPerson) Forest else ForestLight, Bubble)
                        .padding(12.dp)
                ) {
                    Text(l.text, style = MaterialTheme.typography.bodyLarge)
                    if (l.note.isNotEmpty()) Text(l.note, style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
            }
            if (busy) item { Text("Tenvia is thinking...", style = MaterialTheme.typography.bodyMedium, color = Muted) }
        }
        if (state.guides.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                state.guides.take(2).forEach { g ->
                    OutlinedButton(onClick = { onOpenGuide(g) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = Bubble) {
                        Text(g.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                    }
                }
            }
        }
        if (lines.size >= 2 && !busy) {
            TextButton(onClick = { shareText = buildShareText(context, state) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Send this chat to help improve Tenvia (testers)", style = MaterialTheme.typography.bodyMedium, color = Amber)
            }
        }
        shareText?.let { current ->
            AlertDialog(
                onDismissRequest = { shareText = null },
                title = { Text("Send this chat?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "For testers. This opens your phone's share menu with the chat below. Pick your email app to send it " +
                                "to $TESTER_CHAT_EMAIL. Nothing leaves " +
                                "your phone unless you send it there yourself. Please don't share a real emergency. " +
                                "You can edit the text first, for example to remove names or places.",
                            style = MaterialTheme.typography.bodyMedium, color = Muted,
                        )
                        OutlinedTextField(
                            value = current, onValueChange = { shareText = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 300.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(TESTER_CHAT_EMAIL))
                                putExtra(Intent.EXTRA_SUBJECT, "Tenvia tester chat")
                                putExtra(Intent.EXTRA_TEXT, current)
                            }
                            context.startActivity(Intent.createChooser(send, "Send this chat with"))
                            shareText = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Forest),
                    ) { Text("Open share menu") }
                },
                dismissButton = { TextButton(onClick = { shareText = null }) { Text("Cancel") } },
            )
        }
        OutlinedTextField(
            value = input, onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text("Type here", color = Muted) },
            enabled = !busy,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = Bubble) {
                Text("Back", style = MaterialTheme.typography.labelLarge)
            }
            if (aiUp == false) OutlinedButton(onClick = { startTry++ }, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = Bubble) {
                Text("Retry", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = {
                    val msg = input.trim()
                    input = ""
                    lines += ChatLine(true, msg)
                    busy = true
                    scope.launch {
                        val t0 = System.currentTimeMillis()
                        val r = withContext(Dispatchers.IO) { runCatching { engine.respond(msg) } }
                        val secs = (System.currentTimeMillis() - t0) / 1000
                        r.onSuccess {
                            state.notice = it.notice
                            state.guides = it.guides
                            lines += ChatLine(false, it.text, (if (it.generated) "" else "Checked guide text. ") + "${secs}s")
                        }.onFailure {
                            startTry++
                            lines += ChatLine(false, "The on-phone AI didn't answer. Use the guides from the home screen.", "${secs}s")
                        }
                        busy = false
                    }
                },
                enabled = !busy && input.isNotBlank() && aiUp == true,
                modifier = Modifier.weight(2f).heightIn(min = 56.dp), shape = Bubble,
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Forest),
            ) { Text("Send", style = MaterialTheme.typography.labelLarge) }
        }
    }
}
