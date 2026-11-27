package app.tenvia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import app.tenvia.data.Guide
import app.tenvia.data.MatchResult
import app.tenvia.data.UpdateCheck

private val Corner = RoundedCornerShape(14.dp)

/** Shown on every screen until guides are vetted. Honest about what this build is. */
@Composable
fun PrototypeBanner() {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Warning, Corner)
            .padding(14.dp)
    ) {
        Text("PROTOTYPE: NOT FOR REAL EMERGENCIES", color = Warning,
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("Guides come from official government safety pages but haven't been reviewed by experts yet. If you can call or text emergency services, do that first.",
            style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

/** TEST BUILD: shown on the home screen when a newer test version is on GitHub (see UpdateCheck). */
@Composable
private fun UpdateNote() {
    val update = UpdateCheck.available ?: return
    if (UpdateCheck.dismissed) return
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(ForestLight, Corner)
            .border(1.dp, Amber, Corner)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("New test version: ${update.version}", color = Amber,
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text("Download it and install it over this one. No need to uninstall, and your app stays set up. It's a big download (about 1 GB), so use Wi-Fi.",
            style = MaterialTheme.typography.bodyMedium, color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { UpdateCheck.dismissed = true },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = Corner,
            ) { Text("Not now", style = MaterialTheme.typography.labelLarge) }
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.pageUrl)))
                    }
                },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = Corner,
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Forest),
            ) { Text("Download", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
fun HomeScreen(
    guides: List<Guide>,
    onSearch: (String) -> Unit,
    onOpenGuide: (Guide) -> Unit,
    onChat: () -> Unit = {},
) {
    var text by rememberSaveable { mutableStateOf("") }
    ScreenColumn {
        PrototypeBanner()
        UpdateNote()
        Text("Tenvia", style = MaterialTheme.typography.headlineLarge, color = Amber)
        // TEST BUILD: free AI conversation (needs the on-phone AI server started over USB).
        BigButton("Talk to Tenvia (AI test)") { onChat() }
        Text("Or search the checked guides (no AI): tell me what's happening, in your own words.",
            style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text("For example: I'm lost and it's getting dark", color = Muted) },
        )
        BigButton("Find help", enabled = text.isNotBlank()) { onSearch(text) }
        Spacer(Modifier.height(8.dp))
        Text("Or choose a topic", style = MaterialTheme.typography.titleLarge)
        guides.filter { it.id != "crisis-lines" }.forEach { g -> GuideButton(g) { onOpenGuide(g) } }
    }
}

@Composable
fun ResultsScreen(
    result: MatchResult,
    allGuides: List<Guide>,
    onOpenGuide: (Guide) -> Unit,
    onBack: () -> Unit,
) {
    ScreenColumn {
        result.outOfScope.forEach { oos ->
            Column(
                Modifier.fillMaxWidth()
                    .background(ForestLight, Corner)
                    .border(2.dp, Warning, Corner)
                    .padding(16.dp)
            ) {
                Text(oos.message, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        if (result.guides.isNotEmpty()) {
            Text("This may help", style = MaterialTheme.typography.headlineSmall)
            result.guides.forEach { g -> GuideButton(g) { onOpenGuide(g) } }
        } else {
            Text("I couldn't match that to a guide. Choose the closest topic:",
                style = MaterialTheme.typography.bodyLarge)
            allGuides.forEach { g -> GuideButton(g) { onOpenGuide(g) } }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = Corner) {
            Text("Back", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GuideScreen(guide: Guide, onDone: () -> Unit) {
    var step by rememberSaveable(guide.id) { mutableIntStateOf(0) }
    val last = guide.steps.lastIndex
    ScreenColumn {
        if (guide.draft) PrototypeBanner()
        Text(guide.title, style = MaterialTheme.typography.headlineSmall, color = Amber)
        Text("Step ${step + 1} of ${guide.steps.size}", style = MaterialTheme.typography.bodyMedium, color = Muted)
        val current = guide.steps[step]
        Column(
            Modifier.fillMaxWidth()
                .background(ForestLight, Corner)
                .padding(20.dp)
                .heightIn(min = 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(current.text, style = MaterialTheme.typography.bodyLarge)
            if (current.sourceName.isNotEmpty()) {
                Text("Source: ${current.sourceName}", style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { if (step > 0) step-- else onDone() },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp), shape = Corner,
            ) { Text(if (step > 0) "Back" else "Close", style = MaterialTheme.typography.labelLarge) }
            Button(
                onClick = { if (step < last) step++ else onDone() },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp), shape = Corner,
            ) { Text(if (step < last) "Next" else "Done", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun BigButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = Corner,
        colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Forest),
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun GuideButton(guide: Guide, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        shape = Corner,
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(guide.title, style = MaterialTheme.typography.titleLarge, color = Paper)
            Text(guide.summary, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}
