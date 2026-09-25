package com.sleepenergy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime

/** Выгрузка мыслей: минута перед сном, чтобы записать дела на завтра и снять тревожность. */
@Composable
fun BrainDumpScreen(
    bedtime: LocalDateTime,
    initial: List<String>,
    onSave: (items: List<String>, goToBed: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var first by rememberSaveable { mutableStateOf(initial.getOrElse(0) { "" }) }
    var second by rememberSaveable { mutableStateOf(initial.getOrElse(1) { "" }) }
    var third by rememberSaveable { mutableStateOf(initial.getOrElse(2) { "" }) }
    val items = listOf(first, second, third)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Закрыть") }
        Text("Выгрузка мыслей", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Запиши дела на завтра — и голова может отдыхать. Утром список будет ждать на главном экране.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TaskField(first, { first = it }, "Первое дело на завтра", ImeAction.Next)
        TaskField(second, { second = it }, "Второе", ImeAction.Next)
        TaskField(third, { third = it }, "Третье", ImeAction.Done)
        Button(onClick = { onSave(items, true) }, modifier = Modifier.fillMaxWidth()) {
            Text("Записать и лечь спать")
        }
        OutlinedButton(onClick = { onSave(items, false) }, modifier = Modifier.fillMaxWidth()) {
            Text("Только записать")
        }
        Hint("Отбой по плану — в ${hhmm(bedtime)}.")
    }
}

@Composable
private fun TaskField(value: String, onChange: (String) -> Unit, label: String, imeAction: ImeAction) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}
