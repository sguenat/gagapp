package org.gagapp.gonio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val primary = Color(0xFF1F4E79)
            val scheme = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF9CC8F2))
            else lightColorScheme(primary = primary)
            MaterialTheme(colorScheme = scheme) { App() }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object History : Screen
    data class Measure(val joint: Joint, val movement: Movement) : Screen
}

@Composable
private fun App() {
    val context = LocalContext.current
    val store = remember { RecordStore(context) }
    var records by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var side by rememberSaveable { mutableStateOf("Droite") }

    fun update(list: List<Record>) {
        records = list
        store.save(list)
    }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            side = side,
            onSide = { side = it },
            historyCount = records.size,
            onHistory = { screen = Screen.History },
            onPick = { j, m -> screen = Screen.Measure(j, m) },
        )

        Screen.History -> HistoryScreen(
            records = records,
            onBack = { screen = Screen.Home },
            onDelete = { r -> update(records - r) },
        )

        is Screen.Measure -> MeasureScreen(
            joint = s.joint,
            movement = s.movement,
            side = if (s.movement.sided) side else "",
            onBack = { screen = Screen.Home },
            onSave = { r -> update(listOf(r) + records) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { if (onBack != null) TextButton(onClick = onBack) { Text("←", fontSize = 24.sp) } },
                actions = { actions() },
            )
        },
        content = content,
    )
}

@Composable
private fun HomeScreen(
    side: String,
    onSide: (String) -> Unit,
    historyCount: Int,
    onHistory: () -> Unit,
    onPick: (Joint, Movement) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    Page(
        title = "Goniomètre",
        actions = { TextButton(onClick = onHistory) { Text("Historique ($historyCount)") } },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Côté", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    listOf("Gauche", "Droite").forEach {
                        FilterChip(selected = side == it, onClick = { onSide(it) }, label = { Text(it) })
                    }
                }
                Text(
                    "Choisissez une articulation, puis le mouvement à mesurer.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(JOINTS) { joint ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { open = if (open == joint.name) null else joint.name }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(joint.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(if (open == joint.name) "▲" else "▼")
                    }
                    if (open == joint.name) {
                        joint.movements.forEach { m ->
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(joint, m) }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                            ) {
                                Text(m.name, modifier = Modifier.weight(1f))
                                Text("norme ${m.ref}°", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class Phase { READY, RUNNING, DONE }

@Composable
private fun MeasureScreen(
    joint: Joint,
    movement: Movement,
    side: String,
    onBack: () -> Unit,
    onSave: (Record) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val tracker = remember { AngleTracker(context) }
    var fusion by rememberSaveable { mutableStateOf(tracker.hasFusion) }
    var phase by remember { mutableStateOf(Phase.READY) }
    var angle by remember { mutableFloatStateOf(0f) }
    var maxAngle by remember { mutableFloatStateOf(0f) }
    var note by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    DisposableEffect(fusion) {
        tracker.onAngle = {
            if (phase == Phase.RUNNING) {
                angle = it
                maxAngle = max(maxAngle, it)
            }
        }
        tracker.start(fusion)
        phase = Phase.READY
        view.keepScreenOn = true
        onDispose {
            tracker.stop()
            view.keepScreenOn = false
        }
    }

    val title = jointWithSide(joint.name, side)
    Page(title = title, onBack = onBack) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(movement.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Norme : ${movement.ref}°", color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (tracker.hasFusion && tracker.hasAccel) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    FilterChip(
                        selected = fusion,
                        enabled = phase != Phase.RUNNING,
                        onClick = { fusion = true },
                        label = { Text("Gyro + accéléromètre") },
                    )
                    FilterChip(
                        selected = !fusion,
                        enabled = phase != Phase.RUNNING,
                        onClick = { fusion = false },
                        label = { Text("Accéléromètre seul") },
                    )
                }
            }

            val shown = if (phase == Phase.DONE) maxAngle else angle
            Gauge(
                angle = shown,
                ref = movement.ref,
                caption = when (phase) {
                    Phase.READY -> "Prêt"
                    Phase.RUNNING -> "Max : ${maxAngle.roundToInt()}°"
                    Phase.DONE -> "Amplitude maximale"
                },
                modifier = Modifier.weight(1f).padding(24.dp),
            )

            when (phase) {
                Phase.READY -> {
                    Text(
                        if (fusion) "Fixez ou tenez le téléphone sur le segment mobile, placez l'articulation " +
                            "en position de départ, puis mettez à zéro."
                        else "Mode accéléromètre : le mouvement doit incliner le téléphone par rapport à la " +
                            "verticale. Bougez lentement. Placez l'articulation en position de départ, puis mettez à zéro.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    BigButton("Mettre à zéro et démarrer") {
                        angle = 0f
                        maxAngle = 0f
                        saved = false
                        note = ""
                        tracker.zero()
                        phase = Phase.RUNNING
                    }
                }

                Phase.RUNNING -> {
                    Text(
                        "Effectuez le mouvement. Le maximum atteint est conservé.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    BigButton("Terminer") {
                        tracker.clearZero()
                        phase = Phase.DONE
                    }
                }

                Phase.DONE -> {
                    val pct = (maxAngle / movement.ref * 100).roundToInt()
                    Text("$pct % de la norme", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (patient, remarque…)") },
                        singleLine = true,
                        enabled = !saved,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        OutlinedButton(
                            onClick = { phase = Phase.READY; angle = 0f },
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) { Text("Nouvelle mesure") }
                        Button(
                            enabled = !saved,
                            onClick = {
                                onSave(
                                    Record(
                                        System.currentTimeMillis(), joint.name, movement.name, side,
                                        maxAngle.roundToInt(), movement.ref, note.trim(),
                                    )
                                )
                                saved = true
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) { Text(if (saved) "Enregistré ✓" else "Enregistrer") }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)) {
        Text(text, fontSize = 18.sp)
    }
}

/** Ring dial: a full turn is 180°. The thin outer arc marks the normal range, the thick one the measure. */
@Composable
private fun Gauge(angle: Float, ref: Int, caption: String, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val measure = MaterialTheme.colorScheme.primary
    val normal = Color(0xFF3FA66B)
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val thick = size.minDimension * 0.07f
            val thin = thick * 0.35f
            val inset = thin + thick / 2 + thin
            val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
            val topLeft = Offset(inset, inset)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(thick))
            drawArc(
                normal, -90f, (ref / 180f * 360f).coerceAtMost(360f), false,
                Offset(thin / 2, thin / 2), Size(size.width - thin, size.height - thin),
                style = Stroke(thin, cap = StrokeCap.Round),
            )
            if (angle > 0.5f) {
                drawArc(
                    measure, -90f, (angle / 180f * 360f).coerceAtMost(360f), false,
                    topLeft, arcSize, style = Stroke(thick, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${angle.roundToInt()}°", fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun HistoryScreen(records: List<Record>, onBack: () -> Unit, onDelete: (Record) -> Unit) {
    val context = LocalContext.current
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    Page(
        title = "Historique",
        onBack = onBack,
        actions = {
            if (records.isNotEmpty()) TextButton(onClick = {
                val text = records.joinToString("\n") { r ->
                    "${fmt.format(Date(r.time))} — ${r.title} : ${r.angle}° (norme ${r.ref}°)" +
                        if (r.note.isNotEmpty()) " — ${r.note}" else ""
                }
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Mesures d'amplitude articulaire")
                    .putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, "Partager les mesures"))
            }) { Text("Partager") }
        },
    ) { pad ->
        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Aucune mesure enregistrée.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(records, key = { it.time }) { r ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    fmt.format(Date(r.time)) + if (r.note.isNotEmpty()) " · ${r.note}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${r.angle}°", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("norme ${r.ref}°", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onDelete(r) }) { Text("✕") }
                        }
                    }
                }
            }
        }
    }
}
