@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.zakaria5729.phonetics.sample

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.zakaria5729.phonetics.BanglaPhonetics
import io.github.zakaria5729.phonetics.EnglishPhonetics
import io.github.zakaria5729.phonetics.PhoneticCancelToken
import io.github.zakaria5729.phonetics.PhoneticCancelledException
import io.github.zakaria5729.phonetics.PhoneticExecutionMode
import io.github.zakaria5729.phonetics.PhoneticMatch
import io.github.zakaria5729.phonetics.Phonetics
import kotlin.math.roundToInt
import kotlin.time.TimeSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Everything a demo needs for one language: sample data and how to build instances. */
private enum class LanguageChoice(
    val label: String,
    val className: String,
    val defaultSearch: String,
    val inputLabel: String,
    val heavyQuery: String,
    val sampleWords: List<String>,
    val nameParts: List<String>,
    val defaultPair: Pair<String, String>,
    val samplePairs: List<Pair<String, String>>,
) {
    Bangla(
        label = "Bangla (বাংলা)",
        className = "BanglaPhonetics",
        defaultSearch = "মাসুদ",
        inputLabel = "বাংলা শব্দ (Bangla word)",
        heavyQuery = "মাছুদ শরিফ রহীম",
        sampleWords = listOf("মাসুদ", "শরীফ", "রহিম", "নাজনীন", "সৈয়দ", "করিম"),
        nameParts = listOf("মাসুদ", "শরীফ", "রহিম", "করিম", "নাজনীন", "জসীম", "সৈয়দ"),
        defaultPair = "মাসুদ" to "মাছুদ",
        samplePairs = listOf("মাসুদ" to "মাছুদ", "শরীফ" to "শরিফ", "রহিম" to "রহীম"),
    ),
    English(
        label = "English",
        className = "EnglishPhonetics",
        defaultSearch = "Philip",
        inputLabel = "English word",
        heavyQuery = "Stefen Smyth Filip",
        sampleWords = listOf("Philip", "Stephen", "Jackson", "Smith", "Sean", "Catherine"),
        nameParts = listOf("Stephen", "Smith", "Katherine", "Philip", "Jackson", "Sean", "Bryan"),
        defaultPair = "Stephen" to "Stefen",
        samplePairs = listOf(
            "Stephen" to "Stefen",
            "Philip" to "Filip",
            "Smith" to "Smyth",
            "Jackson" to "Jaxon",
            "Catherine" to "Katherine",
        ),
    ),
    ;

    /** One shared instance per language, for the search box. */
    fun sharedInstance(): Phonetics = when (this) {
        Bangla -> BanglaPhonetics.instance
        English -> EnglishPhonetics.instance
    }

    /** A fresh instance, for sections that need their own lifecycle. */
    fun createInstance(executionMode: PhoneticExecutionMode = PhoneticExecutionMode.CancelPrevious): Phonetics =
        when (this) {
            Bangla -> BanglaPhonetics(executionMode = executionMode)
            English -> EnglishPhonetics(executionMode = executionMode)
        }

    fun buildCustomerNames(count: Int): List<String> = List(count) { i ->
        "${nameParts[i % nameParts.size]} " +
            "${nameParts[(i * 3) % nameParts.size]} " +
            "${nameParts[(i * 5) % nameParts.size]} $i"
    }
}

/** `score`, without decimals when it's a whole number, otherwise with two. */
private fun formatScore(score: Double): String {
    val hundredths = kotlin.math.round(score * 100).toLong()
    val whole = hundredths / 100
    val frac = hundredths % 100
    return if (frac == 0L) "$whole" else "$whole.${frac.toString().padStart(2, '0')}"
}

@Composable
public fun App() {
    MaterialTheme {
        Surface {
            var language by remember { mutableStateOf(LanguageChoice.Bangla) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("kotlin-phonetics (${language.label})") },
                        actions = {
                            AssistChip(
                                onClick = {},
                                label = { Text(language.sharedInstance().workerName) },
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                    )
                },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Language:", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LanguageChoice.entries.forEach { option ->
                                        FilterChip(
                                            selected = language == option,
                                            onClick = { language = option },
                                            label = { Text(option.label) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { SearchSection(language) }
                    item { PairwiseSection(language) }
                    item { HeavyJobSection(language) }
                    item { ThreeCallsSection(language) }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SearchSection(language: LanguageChoice) {
    val phonetics = remember(language) { language.sharedInstance() }
    var input by remember(language) { mutableStateOf(language.defaultSearch) }
    var minScore by remember(language) { mutableStateOf(70f) }
    var hasLimit by remember(language) { mutableStateOf(true) }
    var maxResults by remember(language) { mutableStateOf(50) }
    var usesQueue by remember(language) { mutableStateOf(false) }
    var matches by remember(language) { mutableStateOf<List<PhoneticMatch>>(emptyList()) }
    var errorMessage by remember(language) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun search() {
        scope.launch {
            val result = phonetics.generate(
                input,
                minScore = minScore.toDouble(),
                maxResults = if (hasLimit) maxResults else null,
            )
            if (result.error is PhoneticCancelledException) return@launch
            matches = result.matches
            errorMessage = result.error?.message
        }
    }

    LaunchedEffect(language) { search() }

    DisposableEffect(language) {
        onDispose {
            phonetics.cancelAll()
            phonetics.setExecutionMode(PhoneticExecutionMode.CancelPrevious)
        }
    }

    SectionCard(
        title = "1. Search box: ${language.className}.instance",
        description = "One shared instance for quick calls. By default it uses cancelPrevious, " +
            "so each new keystroke cancels the search still running for the one before it.",
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; search() },
            label = { Text(language.inputLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Quick test:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            language.sampleWords.forEach { word ->
                AssistChip(onClick = { input = word; search() }, label = { Text(word) })
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Minimum match: ${minScore.roundToInt()}%",
                modifier = Modifier.width(150.dp),
                fontWeight = FontWeight.Medium,
            )
            Slider(
                value = minScore,
                onValueChange = { minScore = it; search() },
                valueRange = 0f..100f,
                steps = 19,
                modifier = Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.width(150.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasLimit, onCheckedChange = { hasLimit = it; search() })
                Text(
                    if (hasLimit) "Limit: $maxResults" else "No limit",
                    fontWeight = FontWeight.Medium,
                    color = if (hasLimit) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (hasLimit) {
                Slider(
                    value = maxResults.toFloat().coerceIn(5f, 200f),
                    onValueChange = { maxResults = it.roundToInt(); search() },
                    valueRange = 5f..200f,
                    steps = 38,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(20, 50, 100).forEach { preset ->
                        FilterChip(
                            selected = maxResults == preset,
                            onClick = { maxResults = preset; search() },
                            label = { Text("$preset", fontSize = 12.sp) },
                        )
                    }
                }
            } else {
                Text(
                    "Unlimited (all matching phonetics)",
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Queue instead of cancelPrevious", fontWeight = FontWeight.Medium)
                Text(
                    "${language.className}.instance.setExecutionMode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = usesQueue,
                onCheckedChange = { checked ->
                    usesQueue = checked
                    phonetics.setExecutionMode(
                        if (checked) PhoneticExecutionMode.Queue else PhoneticExecutionMode.CancelPrevious,
                    )
                },
            )
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${matches.size} variants" +
                    if (hasLimit && matches.size >= maxResults) " (capped at $maxResults)" else "",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                if (hasLimit) "Limit: $maxResults max" else "Unlimited results",
                style = MaterialTheme.typography.bodySmall,
                color = if (hasLimit) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (errorMessage != null) {
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        }
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(matches) { match ->
                    ListItem(
                        headlineContent = { Text(match.word, fontSize = 20.sp) },
                        supportingContent = {
                            Text(if (match.changes.isEmpty()) "original" else match.changes.joinToString("  "))
                        },
                        trailingContent = {
                            Text("${formatScore(match.score)}%", style = MaterialTheme.typography.titleMedium)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PairwiseSection(language: LanguageChoice) {
    val phonetics = remember(language) { language.createInstance() }
    var word1 by remember(language) { mutableStateOf(language.defaultPair.first) }
    var word2 by remember(language) { mutableStateOf(language.defaultPair.second) }
    var score by remember(language) { mutableStateOf<Double?>(null) }
    var isMatch by remember(language) { mutableStateOf<Boolean?>(null) }
    var errorMessage by remember(language) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun compare() {
        val w1 = word1.trim()
        val w2 = word2.trim()
        if (w1.isEmpty() || w2.isEmpty()) {
            score = 0.0
            isMatch = false
            errorMessage = null
            return
        }
        scope.launch {
            val similarityResult = phonetics.similarity(w1, w2)
            val matchResult = phonetics.matches(w1, w2, minScore = 80.0)
            score = similarityResult.score
            isMatch = matchResult.isMatch
            errorMessage = similarityResult.error?.message ?: matchResult.error?.message
        }
    }

    LaunchedEffect(language) { compare() }
    DisposableEffect(language) { onDispose { phonetics.dispose() } }

    SectionCard(
        title = "2. Pairwise similarity: similarity() & matches()",
        description = "Compare two ${language.label} words to see their phonetic similarity " +
            "percentage and whether they meet the match threshold (≥ 80%).",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = word1,
                onValueChange = { word1 = it; compare() },
                label = { Text("Word 1") },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = word2,
                onValueChange = { word2 = it; compare() },
                label = { Text("Word 2") },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Sample pairs:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            language.samplePairs.forEach { (a, b) ->
                AssistChip(onClick = { word1 = a; word2 = b; compare() }, label = { Text("$a ↔ $b") })
            }
        }
        Spacer(Modifier.height(12.dp))
        val message = errorMessage
        val currentScore = score
        if (message != null) {
            Text(message, color = MaterialTheme.colorScheme.error)
        } else if (currentScore != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Similarity: ${formatScore(currentScore)}%", style = MaterialTheme.typography.titleMedium)
                    val matched = isMatch == true
                    Text(
                        if (matched) "Match (≥80%)" else "No Match (<80%)",
                        color = if (matched) Color(0xFF2E7D32) else Color(0xFFE65100),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeavyJobSection(language: LanguageChoice) {
    var isRunning by remember(language) { mutableStateOf(false) }
    var status by remember(language) { mutableStateOf("") }
    var activeToken by remember(language) { mutableStateOf<PhoneticCancelToken?>(null) }
    var activePhonetics by remember(language) { mutableStateOf<Phonetics?>(null) }
    val scope = rememberCoroutineScope()

    fun run() {
        val phonetics = language.createInstance()
        val token = PhoneticCancelToken()
        activePhonetics = phonetics
        activeToken = token
        isRunning = true
        status = "Ranking 100,000 ${language.label} names…"
        scope.launch {
            val names = language.buildCustomerNames(100_000)
            val start = TimeSource.Monotonic.markNow()
            val result = phonetics.rank(language.heavyQuery, names, limit = 3, cancelToken = token)
            phonetics.dispose()
            if (activePhonetics === phonetics) activePhonetics = null
            val elapsed = start.elapsedNow().inWholeMilliseconds
            status = when {
                result.error == null ->
                    "$elapsed ms. Best: " +
                        result.matches.joinToString(", ") { "${it.word} ${formatScore(it.score)}%" }
                result.error is PhoneticCancelledException -> "Cancelled after $elapsed ms"
                else -> result.error?.message ?: "Unknown error"
            }
            isRunning = false
        }
    }

    DisposableEffect(language) {
        onDispose {
            activeToken?.cancel()
            activePhonetics?.dispose()
        }
    }

    SectionCard(
        title = "3. Heavy job: its own ${language.className}()",
        description = "A big job gets its own instance, created when it starts and disposed " +
            "when it ends. Typing in the search box above does not cancel it, and the work keeps " +
            "running because it is off the main thread.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isRunning) {
                OutlinedButton(onClick = { activeToken?.cancel() }) { Text("Cancel") }
                Spacer(Modifier.width(12.dp))
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
            } else {
                Button(onClick = { run() }) { Text("Stress test") }
            }
            Spacer(Modifier.width(12.dp))
            Text(status, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ThreeCallsSection(language: LanguageChoice) {
    var mode by remember(language) { mutableStateOf(PhoneticExecutionMode.CancelPrevious) }
    var isRunning by remember(language) { mutableStateOf(false) }
    var lines by remember(language) { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun run() {
        isRunning = true
        lines = emptyList()
        scope.launch {
            val phonetics = language.createInstance(executionMode = mode)
            val names = language.buildCustomerNames(30_000)
            val start = TimeSource.Monotonic.markNow()
            val results = coroutineScope {
                (0 until 3).map { index ->
                    async {
                        val result = phonetics.rank(language.heavyQuery, names, limit = 1)
                        val elapsed = start.elapsedNow().inWholeMilliseconds
                        if (result.error is PhoneticCancelledException) {
                            "Call ${index + 1}: cancelled after $elapsed ms"
                        } else {
                            "Call ${index + 1}: done in $elapsed ms"
                        }
                    }
                }.awaitAll()
            }
            phonetics.dispose()
            lines = results
            isRunning = false
        }
    }

    SectionCard(
        title = "4. Three calls at once: cancelPrevious or queue",
        description = "The three calls are made together on a new ${language.className}" +
            "(executionMode = ...). cancelPrevious: each new call cancels the one before, so only " +
            "the last finishes. queue: each call waits for the one before it.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == PhoneticExecutionMode.CancelPrevious,
                onClick = { if (!isRunning) mode = PhoneticExecutionMode.CancelPrevious },
                label = { Text("cancelPrevious") },
            )
            FilterChip(
                selected = mode == PhoneticExecutionMode.Queue,
                onClick = { if (!isRunning) mode = PhoneticExecutionMode.Queue },
                label = { Text("queue") },
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { run() }, enabled = !isRunning) { Text("Run 3 calls") }
        Spacer(Modifier.height(8.dp))
        lines.forEach { Text(it) }
    }
}
