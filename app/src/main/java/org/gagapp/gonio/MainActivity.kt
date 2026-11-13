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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            val scheme = if (isSystemInDarkTheme()) darkColorScheme(
                primary = Color(0xFF9CC8F2),
                primaryContainer = Color(0xFF1F4E79),
                onPrimaryContainer = Color(0xFFD6E6F5),
            ) else lightColorScheme(
                primary = primary,
                primaryContainer = Color(0xFFD6E6F5),
                onPrimaryContainer = Color(0xFF0F2F4D),
            )
            MaterialTheme(colorScheme = scheme) { App() }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object History : Screen
    /** [joint] and [movement] are null for a quick measurement with no reference value. */
    data class Measure(val joint: Joint?, val movement: Movement?) : Screen
}

@Composable
private fun App() {
    val context = LocalContext.current
    val store = remember { RecordStore(context) }
    var records by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var side by rememberSaveable { mutableStateOf("R") }

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
            onQuick = { screen = Screen.Measure(null, null) },
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
            side = if (s.movement?.sided == true) side else "",
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
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
    onQuick: () -> Unit,
    onPick: (Joint, Movement) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    Page(
        title = stringResource(R.string.app_name),
        actions = { TextButton(onClick = onHistory) { Text(stringResource(R.string.history_count, historyCount)) } },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Button(
                    onClick = onQuick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2A93B),
                        contentColor = Color(0xFF2B1B00),
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        Text(
                            "⚡ " + stringResource(R.string.quick_measure),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.quick_sub),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.side), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    listOf("L" to R.string.left, "R" to R.string.right).forEach { (key, label) ->
                        FilterChip(
                            selected = side == key,
                            onClick = { onSide(key) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                Text(
                    stringResource(R.string.home_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(JOINTS) { joint ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { open = if (open == joint.key) null else joint.key }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(joint.name),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(if (open == joint.key) "▲" else "▼")
                    }
                    if (open == joint.key) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        ) {
                            joint.movements.forEach { m ->
                                Surface(
                                    onClick = { onPick(joint, m) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                stringResource(m.name),
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Text(
                                                stringResource(R.string.norm_short, m.ref),
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                        Text(
                                            stringResource(R.string.measure_action) + "  ›",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class Phase { READY, RUNNING, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasureScreen(
    joint: Joint?,
    movement: Movement?,
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

    val title = if (joint == null) stringResource(R.string.free_measure)
    else jointWithSide(context.resources, stringResource(joint.name), side)
    Page(title = title, onBack = onBack) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (movement != null) {
                Text(
                    stringResource(movement.name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.norm_long, movement.ref),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (tracker.hasFusion && tracker.hasAccel) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    listOf(true to R.string.sensor_gyro, false to R.string.sensor_accel)
                        .forEachIndexed { i, (value, label) ->
                            SegmentedButton(
                                selected = fusion == value,
                                onClick = { fusion = value },
                                enabled = phase != Phase.RUNNING,
                                shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                                icon = {},
                            ) { Text(stringResource(label), maxLines = 1, softWrap = false) }
                        }
                }
            }

            val shown = if (phase == Phase.DONE) maxAngle else angle
            Gauge(
                angle = shown,
                ref = movement?.ref,
                caption = when (phase) {
                    Phase.READY -> stringResource(R.string.ready)
                    Phase.RUNNING -> stringResource(R.string.max_fmt, maxAngle.roundToInt())
                    Phase.DONE -> stringResource(R.string.max_amplitude)
                },
                modifier = Modifier.weight(1f).padding(24.dp),
            )

            when (phase) {
                Phase.READY -> {
                    Text(
                        stringResource(if (fusion) R.string.hint_fusion else R.string.hint_accel),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    BigButton(stringResource(R.string.zero_start)) {
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
                        stringResource(R.string.hint_running),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    BigButton(stringResource(R.string.finish)) {
                        tracker.clearZero()
                        phase = Phase.DONE
                    }
                }

                Phase.DONE -> {
                    if (movement != null) {
                        val pct = (maxAngle / movement.ref * 100).roundToInt()
                        Text(stringResource(R.string.pct_norm, pct), style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.note_label), maxLines = 1) },
                        singleLine = true,
                        enabled = !saved,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        val tight = PaddingValues(horizontal = 8.dp)
                        OutlinedButton(
                            onClick = { phase = Phase.READY; angle = 0f },
                            contentPadding = tight,
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) { OneLine(stringResource(R.string.new_measure)) }
                        Button(
                            enabled = !saved,
                            onClick = {
                                onSave(
                                    Record(
                                        System.currentTimeMillis(), joint?.key ?: FREE_KEY, movement?.key ?: "",
                                        side, maxAngle.roundToInt(), movement?.ref ?: 0, note.trim(),
                                    )
                                )
                                saved = true
                            },
                            contentPadding = tight,
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) { OneLine(stringResource(if (saved) R.string.saved else R.string.save)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OneLine(text: String) {
    Text(text, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
    ) {
        Text(text, fontSize = 18.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

/** Ring dial: a full turn is 180°. The thin outer arc marks the normal range (if any), the thick one the measure. */
@Composable
private fun Gauge(angle: Float, ref: Int?, caption: String, modifier: Modifier = Modifier) {
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
            if (ref != null) {
                drawArc(
                    normal, -90f, (ref / 180f * 360f).coerceAtMost(360f), false,
                    Offset(thin / 2, thin / 2), Size(size.width - thin, size.height - thin),
                    style = Stroke(thin, cap = StrokeCap.Round),
                )
            }
            if (angle > 0.5f) {
                drawArc(
                    measure, -90f, (angle / 180f * 360f).coerceAtMost(360f), false,
                    topLeft, arcSize, style = Stroke(thick, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${angle.roundToInt()}°", fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HistoryScreen(records: List<Record>, onBack: () -> Unit, onDelete: (Record) -> Unit) {
    val context = LocalContext.current
    val res = context.resources
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    Page(
        title = stringResource(R.string.history),
        onBack = onBack,
        actions = {
            if (records.isNotEmpty()) TextButton(onClick = {
                val text = records.joinToString("\n") { r ->
                    val date = fmt.format(Date(r.time))
                    (if (r.ref > 0) res.getString(R.string.share_line, date, r.title(res), r.angle, r.ref)
                    else res.getString(R.string.share_line_free, date, r.title(res), r.angle)) +
                        if (r.note.isNotEmpty()) " — ${r.note}" else ""
                }
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, res.getString(R.string.share_subject))
                    .putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, res.getString(R.string.share_chooser)))
            }) { Text(stringResource(R.string.share)) }
        },
    ) { pad ->
        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_records))
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
                                Text(r.title(res), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    fmt.format(Date(r.time)) + if (r.note.isNotEmpty()) " · ${r.note}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${r.angle}°", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                if (r.ref > 0) Text(
                                    stringResource(R.string.norm_short, r.ref),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            TextButton(onClick = { onDelete(r) }) { Text("✕") }
                        }
                    }
                }
            }
        }
    }
}
