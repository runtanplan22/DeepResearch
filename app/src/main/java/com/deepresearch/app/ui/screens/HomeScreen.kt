package com.deepresearch.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.deepresearch.app.data.model.IterationMode
import com.deepresearch.app.data.model.ReportLength
import com.deepresearch.app.data.model.ResearchStatus
import com.deepresearch.app.viewmodel.ResearchViewModel

/**
 * Home screen - topic input, options, and start.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ResearchViewModel,
    onPlanCreated: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val researchState by viewModel.researchState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "DeepResearch",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "KI-gestützte Tiefenrecherche",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Topic input
        OutlinedTextField(
            value = researchState.topic,
            onValueChange = { viewModel.updateTopic(it) },
            label = { Text("Forschungsthema / Frage") },
            placeholder = { Text("z.B. Auswirkungen von KI auf den Arbeitsmarkt") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            enabled = researchState.status != ResearchStatus.PLANNING
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Report length selector
        Text(
            text = "Berichtslänge",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ReportLength.values().forEach { length ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = researchState.reportLength == length,
                    onClick = { viewModel.updateReportLength(length) },
                    enabled = researchState.status != ResearchStatus.PLANNING
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = length.label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = length.wordCountRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Iteration mode
        Text(
            text = "Iterationen",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = researchState.iterationMode == IterationMode.AUTO,
                onClick = { viewModel.updateIterationMode(IterationMode.AUTO) },
                enabled = researchState.status != ResearchStatus.PLANNING
            )
            Text(
                text = "Automatisch (KI entscheidet)",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = researchState.iterationMode == IterationMode.MANUAL,
                onClick = { viewModel.updateIterationMode(IterationMode.MANUAL) },
                enabled = researchState.status != ResearchStatus.PLANNING
            )
            Text(
                text = "Manuell festlegen",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Manual count dropdown
        AnimatedVisibility(visible = researchState.iterationMode == IterationMode.MANUAL) {
            var expanded by remember { mutableStateOf(false) }
            val options = listOf(3, 5, 7, 10)
            var selectedCount by remember { mutableIntStateOf(5) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.padding(start = 32.dp, top = 8.dp)
            ) {
                OutlinedTextField(
                    value = "$selectedCount Iterationen",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    enabled = researchState.status != ResearchStatus.PLANNING
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { count ->
                        DropdownMenuItem(
                            text = { Text("$count Iterationen") },
                            onClick = {
                                selectedCount = count
                                expanded = false
                                viewModel.updateManualIterationCount(count)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Settings button
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("API")
            }

            // Main action button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.startPlanning()
                },
                modifier = Modifier.weight(2f),
                enabled = researchState.topic.isNotBlank() &&
                        researchState.status != ResearchStatus.PLANNING
            ) {
                if (researchState.status == ResearchStatus.PLANNING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Erstelle Plan...")
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechercheplan erstellen")
                }
            }
        }

        // Error message
        if (researchState.error != null && researchState.status == ResearchStatus.ERROR) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = researchState.error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Restored report from cache
        if (researchState.status == ResearchStatus.COMPLETE && researchState.report.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Letzter Bericht vorhanden",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = researchState.topic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
