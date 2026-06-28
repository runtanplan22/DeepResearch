package com.deepresearch.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.deepresearch.app.data.model.SearchImage
import com.deepresearch.app.data.model.ResearchStatus
import com.deepresearch.app.ui.components.MarkdownText
import com.deepresearch.app.viewmodel.ResearchViewModel
import java.io.File

/**
 * Report screen with tabs: View Report, Discussion, and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ResearchViewModel,
    onNewResearch: () -> Unit
) {
    val researchState by viewModel.researchState.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Bericht", "Bilder", "Diskutieren")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Forschungsbericht",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = researchState.topic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    // PDF Export
                    if (researchState.report.isNotBlank()) {
                        IconButton(onClick = { exportPdf(context, researchState.report, researchState.topic) }) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Als PDF speichern")
                        }
                    }
                    // New research
                    IconButton(onClick = onNewResearch) {
                        Icon(Icons.Filled.Add, contentDescription = "Neue Recherche")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ReportContentTab(viewModel, context)
                1 -> ImagesTab(viewModel)
                2 -> DiscussionTab(viewModel)
            }
        }
    }
}

@Composable
private fun ReportContentTab(viewModel: ResearchViewModel, context: Context) {
    val researchState by viewModel.researchState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Error state
        if (researchState.status == ResearchStatus.ERROR) {
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
                        text = researchState.error ?: "Fehler beim Erstellen des Berichts.",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            return
        }

        if (researchState.report.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Kein Bericht verfügbar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        // Report content as rendered Markdown
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            MarkdownText(
                markdown = researchState.report,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { exportPdf(context, researchState.report, researchState.topic) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PDF speichern")
            }

            OutlinedButton(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, researchState.report)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Bericht teilen"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Teilen")
            }
        }
    }
}

@Composable
private fun ImagesTab(viewModel: ResearchViewModel) {
    val researchState by viewModel.researchState.collectAsState()
    val images = researchState.collectedImages

    if (images.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Keine Bilder gefunden.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(images) { image ->
            ImageCard(image = image, viewModel = viewModel)
        }
    }
}

@Composable
private fun ImageCard(image: SearchImage, viewModel: ResearchViewModel) {
    val researchState by viewModel.researchState.collectAsState()
    val isSelected = researchState.selectedImageUrls.contains(image.imgUrl)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.imgUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = image.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp),
                contentScale = ContentScale.Fit
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = image.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = image.sourceUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { viewModel.toggleImageSelection(image.imgUrl) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "In Bericht einfügen",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscussionTab(viewModel: ResearchViewModel) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()

    var inputText by remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "💬 Diskussion",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Stelle Fragen zum Bericht. Die KI antwortet basierend auf den Recherche-Ergebnissen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(chatMessages) { msg ->
                val isUser = msg.role == com.deepresearch.app.data.model.ChatRole.USER
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isUser) "Du" else "Assistent",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isUser)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        MarkdownText(
                            markdown = msg.content,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (isChatLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Denkt nach...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Input
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Frage zum Bericht...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isChatLoading
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.text.isNotBlank()) {
                        viewModel.sendChatMessage(inputText.text)
                        inputText = TextFieldValue("")
                    }
                },
                enabled = inputText.text.isNotBlank() && !isChatLoading
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Senden",
                    tint = if (inputText.text.isNotBlank() && !isChatLoading)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * Export the report as a PDF using Android's built-in PdfDocument API.
 * Available since API 19, no external library needed.
 */
private fun exportPdf(context: Context, report: String, topic: String) {
    try {
        val safeTopic = topic.take(30).replace(Regex("""[^a-zA-Z0-9]"""), "_")
        val pdfFile = File(context.cacheDir, "DeepResearch_Bericht_$safeTopic.pdf")
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val paint = android.graphics.Paint()
        val titlePaint = android.graphics.Paint().apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 32f
            isAntiAlias = true
        }
        val headingPaint = android.graphics.Paint().apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 22f
            isAntiAlias = true
        }
        val subheadingPaint = android.graphics.Paint().apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 18f
            isAntiAlias = true
        }
        val bodyPaint = android.graphics.Paint().apply {
            textSize = 14f
            isAntiAlias = true
        }
        val datePaint = android.graphics.Paint().apply {
            textSize = 12f
            color = android.graphics.Color.GRAY
            isAntiAlias = true
        }

        // Page info: A4 = 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50
        val contentWidth = pageWidth - 2 * margin

        var pageCount = 1
        var y = margin + 30f

        // Title page
        val titlePageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCount).create()
        var page = pdfDocument.startPage(titlePageInfo)
        val canvas = page.canvas

        // Title
        canvas.drawText("Forschungsbericht", margin.toFloat(), y, titlePaint)
        y += 50f
        canvas.drawText(topic, margin.toFloat(), y, headingPaint)
        y += 40f
        val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.GERMAN).format(java.util.Date())
        canvas.drawText(dateStr, margin.toFloat(), y, datePaint)
        y += 30f

        // Separator line
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, paint)
        paint.style = android.graphics.Paint.Style.FILL
        y += 30f

        // Process markdown content
        val lines = report.split("\n")
        for (line in lines) {
            // Check if we need a new page
            if (y > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageCount++
                val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCount).create()
                page = pdfDocument.startPage(newPageInfo)
                y = margin.toFloat()
            }

            val displayLine = line.trimStart()
            when {
                displayLine.startsWith("### ") -> {
                    canvas.drawText(displayLine.removePrefix("### "), margin.toFloat(), y, subheadingPaint)
                    y += 30f
                }
                displayLine.startsWith("## ") -> {
                    canvas.drawText(displayLine.removePrefix("## "), margin.toFloat(), y, headingPaint)
                    y += 35f
                }
                displayLine.startsWith("# ") -> {
                    canvas.drawText(displayLine.removePrefix("# "), margin.toFloat(), y, titlePaint)
                    y += 40f
                }
                displayLine.startsWith("- ") || displayLine.startsWith("* ") -> {
                    val content = displayLine.removePrefix("- ").removePrefix("* ")
                    canvas.drawText("  \u2022  $content", margin.toFloat(), y, bodyPaint)
                    y += 22f
                }
                displayLine.matches(Regex("""^\d+\..*""")) -> {
                    canvas.drawText(displayLine, margin.toFloat(), y, bodyPaint)
                    y += 22f
                }
                displayLine.startsWith("---") || displayLine.startsWith("***") -> {
                    paint.style = android.graphics.Paint.Style.STROKE
                    canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, paint)
                    paint.style = android.graphics.Paint.Style.FILL
                    y += 20f
                }
                displayLine.startsWith("> ") -> {
                    paint.color = android.graphics.Color.GRAY
                    canvas.drawText(displayLine.removePrefix("> "), margin.toFloat() + 20f, y, bodyPaint)
                    paint.color = android.graphics.Color.BLACK
                    y += 22f
                }
                displayLine.isBlank() -> {
                    y += 10f
                }
                else -> {
                    // Word wrap long lines
                    val words = displayLine.split(" ")
                    val wrapped = mutableListOf<String>()
                    var currentLine = StringBuilder()
                    for (word in words) {
                        if (currentLine.length + word.length > 80) {
                            wrapped.add(currentLine.toString())
                            currentLine = StringBuilder(word)
                        } else {
                            if (currentLine.isNotEmpty()) currentLine.append(" ")
                            currentLine.append(word)
                        }
                    }
                    if (currentLine.isNotEmpty()) wrapped.add(currentLine.toString())
                    for (wl in wrapped) {
                        if (y > pageHeight - margin) {
                            pdfDocument.finishPage(page)
                            pageCount++
                            val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCount).create()
                            page = pdfDocument.startPage(newPageInfo)
                            y = margin.toFloat()
                        }
                        canvas.drawText(wl, margin.toFloat(), y, bodyPaint)
                        y += 20f
                    }
                }
            }
        }

        pdfDocument.finishPage(page)

        // Write to file
        pdfDocument.writeTo(java.io.FileOutputStream(pdfFile))
        pdfDocument.close()

        // Share via Android share intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pdfFile))
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "PDF teilen"))

    } catch (e: Exception) {
        // Fallback: share as text
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, report)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Bericht teilen"))
    }
}
