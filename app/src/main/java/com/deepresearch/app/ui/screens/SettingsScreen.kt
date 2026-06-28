package com.deepresearch.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.deepresearch.app.viewmodel.ResearchViewModel

/**
 * API Settings screen with connection test capabilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ResearchViewModel,
    onBack: () -> Unit
) {
    val settings = remember { com.deepresearch.app.DeepResearchApp.instance.settingsDataStore }
    val deepSeekModels by viewModel.deepSeekModels.collectAsState()
    val deepSeekTestResult by viewModel.deepSeekTestResult.collectAsState()
    val searxngTestResult by viewModel.searxngTestResult.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()

    // Local state for form fields
    var deepSeekUrl by remember { mutableStateOf(settings.getDeepSeekBaseUrl()) }
    var deepSeekKey by remember { mutableStateOf(settings.getDeepSeekApiKey()) }
    var showDeepSeekKey by remember { mutableStateOf(false) }
    var searxngUrl by remember { mutableStateOf(settings.getSearxngBaseUrl()) }
    var searxngKey by remember { mutableStateOf(settings.getSearxngApiKey()) }
    var showSearxngKey by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(settings.getSelectedModel()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API-Konfiguration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ====== DeepSeek Section ======
            Text(
                text = "DeepSeek API",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = deepSeekUrl,
                onValueChange = { deepSeekUrl = it },
                label = { Text("Base URL") },
                placeholder = { Text("https://api.deepseek.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = deepSeekKey,
                onValueChange = { deepSeekKey = it },
                label = { Text("API-Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showDeepSeekKey)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showDeepSeekKey = !showDeepSeekKey }) {
                        Icon(
                            imageVector = if (showDeepSeekKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showDeepSeekKey) "Verstecken" else "Anzeigen"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Save DeepSeek
            Button(
                onClick = {
                    viewModel.updateDeepSeekBaseUrl(deepSeekUrl)
                    viewModel.updateDeepSeekApiKey(deepSeekKey)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("DeepSeek Einstellungen speichern")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Model dropdown
            Text(
                text = "Ausgewähltes Modell",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var modelExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = !modelExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Modell") }
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        if (deepSeekModels.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Keine Modelle geladen") },
                                onClick = { modelExpanded = false }
                            )
                        } else {
                            deepSeekModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        selectedModel = model
                                        viewModel.updateSelectedModel(model)
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        // Save current URLs first
                        viewModel.updateDeepSeekBaseUrl(deepSeekUrl)
                        viewModel.updateDeepSeekApiKey(deepSeekKey)
                        viewModel.loadDeepSeekModels()
                    },
                    enabled = !isLoadingModels
                ) {
                    if (isLoadingModels) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Laden")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test connection
            OutlinedButton(
                onClick = {
                    viewModel.updateDeepSeekBaseUrl(deepSeekUrl)
                    viewModel.updateDeepSeekApiKey(deepSeekKey)
                    viewModel.testDeepSeekConnection()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Verbindung testen")
            }

            // Test result
            if (deepSeekTestResult != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (deepSeekTestResult!!.startsWith("✅"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = deepSeekTestResult!!,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // ====== SearXNG Section ======
            Text(
                text = "SearXNG API",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searxngUrl,
                onValueChange = { searxngUrl = it },
                label = { Text("Base URL") },
                placeholder = { Text("https://searxng.example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searxngKey,
                onValueChange = { searxngKey = it },
                label = { Text("API-Key (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showSearxngKey)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSearxngKey = !showSearxngKey }) {
                        Icon(
                            imageVector = if (showSearxngKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showSearxngKey) "Verstecken" else "Anzeigen"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.updateSearxngBaseUrl(searxngUrl)
                    viewModel.updateSearxngApiKey(searxngKey)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SearXNG Einstellungen speichern")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    viewModel.updateSearxngBaseUrl(searxngUrl)
                    viewModel.updateSearxngApiKey(searxngKey)
                    viewModel.testSearXNGConnection()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Verbindung testen")
            }

            // Test result
            if (searxngTestResult != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (searxngTestResult!!.startsWith("✅"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = searxngTestResult!!,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // App info
            Text(
                text = "Über DeepResearch",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "KI-gestützte Tiefenrecherche mit DeepSeek + SearXNG",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
