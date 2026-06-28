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
        HorizontalDivider()
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
 * Export the report as a PDF using iText.
 */
private fun exportPdf(context: Context, report: String, topic: String) {
    try {
        val pdfFile = File(context.cacheDir, "DeepResearch_Bericht_${topic.take(30).replace(Regex("""[^a-zA-Z0-9]"""), "_")}.pdf")
        val writer = com.itextpdf.kernel.pdf.PdfWriter(pdfFile)
        val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = com.itextpdf.layout.Document(pdf)

        // Title page
        val titleFont = com.itextpdf.kernel.font.PdfFontFactory.createFont()
        val boldFont = com.itextpdf.kernel.font.PdfFontFactory.createFont(
            com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD
        )

        document.add(com.itextpdf.layout.element.Paragraph("Forschungsbericht")
            .setFont(boldFont)
            .setFontSize(24)
            .setMarginBottom(10))
        document.add(com.itextpdf.layout.element.Paragraph(topic)
            .setFont(titleFont)
            .setFontSize(16)
            .setMarginBottom(20))
        document.add(com.itextpdf.layout.element.Paragraph(java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.GERMAN).format(java.util.Date()))
            .setFont(titleFont)
            .setFontSize(12)
            .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
            .setMarginBottom(30))

        // Separator
        document.add(com.itextpdf.layout.element.LineSeparator(com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f)))
        document.add(com.itextpdf.layout.element.Paragraph("").setMarginBottom(10))

        // Process markdown into PDF content
        val lines = report.split("\n")
        for (line in lines) {
            when {
                line.startsWith("### ") -> {
                    document.add(com.itextpdf.layout.element.Paragraph(line.removePrefix("### "))
                        .setFont(boldFont)
                        .setFontSize(13)
                        .setMarginTop(12)
                        .setMarginBottom(6))
                }
                line.startsWith("## ") -> {
                    document.add(com.itextpdf.layout.element.Paragraph(line.removePrefix("## "))
                        .setFont(boldFont)
                        .setFontSize(16)
                        .setMarginTop(16)
                        .setMarginBottom(8))
                }
                line.startsWith("# ") -> {
                    document.add(com.itextpdf.layout.element.Paragraph(line.removePrefix("# "))
                        .setFont(boldFont)
                        .setFontSize(20)
                        .setMarginTop(20)
                        .setMarginBottom(10))
                }
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val content = line.trimStart().removePrefix("- ").removePrefix("* ")
                    document.add(com.itextpdf.layout.element.ListItem(content)
                        .setFont(titleFont)
                        .setFontSize(11)
                        .setMarginLeft(20))
                }
                line.startsWith("---") || line.startsWith("***") -> {
                    document.add(com.itextpdf.layout.element.LineSeparator(com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f)))
                }
                line.isBlank() -> { /* skip */ }
                else -> {
                    document.add(com.itextpdf.layout.element.Paragraph(line)
                        .setFont(titleFont)
                        .setFontSize(11)
                        .setMarginBottom(4))
                }
            }
        }

        document.close()

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
