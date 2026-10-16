package io.github.zakaria5729.phonetics.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.zakaria5729.phonetics.PhoneticLanguages
import io.github.zakaria5729.phonetics.compose.rememberPhoneticMatches
import io.github.zakaria5729.phonetics.compose.rememberPhonetics

private enum class Choice(val label: String, val sample: String) {
    Bangla("Bangla", "আসাদ"),
    English("English", "Catherine"),
}

@Composable
public fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var choice by remember { mutableStateOf(Choice.Bangla) }
            var input by remember { mutableStateOf(Choice.Bangla.sample) }

            val language = if (choice == Choice.Bangla) PhoneticLanguages.bangla else PhoneticLanguages.english
            val phonetics = rememberPhonetics(language)
            val result by rememberPhoneticMatches(phonetics, input, maxResults = 30, debounceMillis = 120)

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice.entries.forEach { option ->
                        FilterChip(
                            selected = choice == option,
                            onClick = {
                                choice = option
                                input = option.sample
                            },
                            label = { Text(option.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Type a name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = result?.error?.message ?: "Running on ${phonetics.workerName}",
                    style = MaterialTheme.typography.labelMedium,
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(result?.matches.orEmpty()) { match ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(match.word, style = MaterialTheme.typography.bodyLarge)
                            Text("${match.score}%", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
