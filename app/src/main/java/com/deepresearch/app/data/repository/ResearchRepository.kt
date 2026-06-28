package com.deepresearch.app.data.repository

import com.deepresearch.app.data.api.*
import com.deepresearch.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repository handling all API communication.
 * Manages Retrofit instances for DeepSeek and SearXNG with dynamic base URLs.
 */
class ResearchRepository(
    private val settingsProvider: SettingsProvider
) {
    open class SettingsProvider(
        val deepSeekBaseUrl: () -> String,
        val deepSeekApiKey: () -> String,
        val searxngBaseUrl: () -> String,
        val searxngApiKey: () -> String
    )

    private var deepSeekService: DeepSeekApiService? = null
    private var searxngService: SearXngApiService? = null

    // System prompt for DeepSeek to generate research plan
    private val planSystemPrompt = """
Du bist ein professioneller Recherche-Assistent. Erstelle einen detaillierten Rechercheplan für die folgende Frage/ein Thema.

Formuliere deine Antwort als JSON mit folgenden Feldern:
- "topic": Das übergeordnete Thema
- "sub_questions": Ein Array mit 3-6 Unterfragen, die die Recherche strukturieren
- "search_directions": Ein Array mit 2-4 Suchrichtungen (Stichworte/Suchstrategien)
- "estimated_iterations": Geschätzte Anzahl Iterationen (Zahl zwischen 3 und 7)

Gib NUR das JSON-Objekt aus, keine zusätzlichen Erklärungen.
    """.trimIndent()

    // System prompt for DeepSeek to execute research
    private fun researchSystemPrompt(length: ReportLength): String {
        val wordTarget = when (length) {
            ReportLength.SHORT -> "300-500 Wörter"
            ReportLength.MEDIUM -> "800-1200 Wörter"
            ReportLength.LONG -> "2000-4000 Wörter"
        }
        return """
Du bist ein professioneller Recherche-Assistent. Du führst eine mehrstufige Recherche durch.

Aufgabe: Analysiere die Suchergebnisse gründlich und erstelle eine fundierte Analyse.
Nach jeder Analyse gibst du eine neue Suchrichtung vor, um tiefer zu graben.

Formuliere deine Antwort als JSON mit folgenden Feldern:
- "analysis": Deine detaillierte Zwischenanalyse der bisherigen Funde
- "next_direction": Die nächste Suchrichtung oder Fragestellung
- "sufficient": true/false - ob du genug Informationen hast (nach mindestens 3 Iterationen)
- "key_findings": Array von Schlüsselerkenntnissen

Am Ende der Recherche erstellst du einen vollständigen Bericht in folgendem Umfang: $wordTarget.
Der finale Bericht soll als gut formatierter Markdown-Text mit Überschriften, Aufzählungen und Quellenverweisen sein.
Struktur den Bericht mit: Einleitung, Hauptteil (mit Zwischenüberschriften), Fazit und Quellenverzeichnis.

Falls du Bilder in den Suchergebnissen gefunden hast, verweise darauf im Text mit [Bild: URL].
        """.trimIndent()
    }

    /**
     * Builds or returns cached DeepSeek API service with current base URL.
     */
    private fun getDeepSeekService(): DeepSeekApiService {
        val baseUrl = settingsProvider.deepSeekBaseUrl().trimEnd('/') + "/"
        if (deepSeekService != null) return deepSeekService!!
        return createDeepSeekService(baseUrl)
    }

    private fun createDeepSeekService(baseUrl: String): DeepSeekApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(DeepSeekApiService::class.java).also {
            deepSeekService = it
        }
    }

    /**
     * Reset service cache (called when settings change).
     */
    fun resetDeepSeekService() {
        deepSeekService = null
    }

    /**
     * Builds or returns cached SearXNG API service.
     */
    private fun getSearXngService(): SearXngApiService {
        val baseUrl = settingsProvider.searxngBaseUrl().trimEnd('/') + "/"
        if (searxngService != null) return searxngService!!
        return createSearXngService(baseUrl)
    }

    private fun createSearXngService(baseUrl: String): SearXngApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(SearXngApiService::class.java).also {
            searxngService = it
        }
    }

    /**
     * Generate a research plan using DeepSeek.
     */
    suspend fun generatePlan(topic: String): ResearchPlan = withContext(Dispatchers.IO) {
        val request = DeepSeekChatRequest(
            model = settingsProvider.getSelectedModel(),
            messages = listOf(
                ChatMessageDto("system", planSystemPrompt),
                ChatMessageDto("user", topic)
            ),
            temperature = 0.3
        )
        val apiKey = settingsProvider.deepSeekApiKey()
        val authHeader = "Bearer $apiKey"
        val response = getDeepSeekService().chatCompletion(authHeader, request)
        val body = response.body()
        val error = body?.error

        if (!response.isSuccessful || error != null) {
            throw Exception(error?.message ?: "HTTP ${response.code()}: ${response.message()}")
        }

        val content = body?.choices?.firstOrNull()?.message?.content
            ?: throw Exception("Leere Antwort von der API")

        // Parse JSON from the response (may contain markdown code fences)
        val jsonStr = content.trimStart()
            .removePrefix("```json")
            .removePrefix("```")
            .trimEnd()
            .removeSuffix("```")
            .trim()

        // Use Gson to parse into a map
        val gson = com.google.gson.Gson()
        val map = gson.fromJson(jsonStr, Map::class.java) as? Map<*, *>
            ?: throw Exception("Konnte Antwort nicht als JSON parsen")

        ResearchPlan(
            topic = map["topic"]?.toString() ?: topic,
            subQuestions = (map["sub_questions"] as? List<*>)?.mapNotNull { it?.toString() }
                ?: emptyList(),
            searchDirections = (map["search_directions"] as? List<*>)?.mapNotNull { it?.toString() }
                ?: emptyList(),
            estimatedIterations = (map["estimated_iterations"] as? Double)?.toInt()
                ?: (map["estimated_iterations"] as? String)?.toIntOrNull() ?: 3
        )
    }

    /**
     * Execute one research iteration: search + analyze.
     * Returns the iteration result including sources and next direction.
     */
    suspend fun executeIteration(
        query: String,
        iterationNumber: Int,
        totalIterations: Int,
        context: List<IterationResult> = emptyList(),
        mode: IterationMode,
        reportLength: ReportLength
    ): IterationResult = withContext(Dispatchers.IO) {
        // Step 1: Search via SearXNG
        val searchResponse = getSearXngService().search(query = query)
        val searchBody = searchResponse.body()
        val searchResults = searchBody?.results?.mapNotNull { result ->
            if (result.url != null) {
                SearchResult(
                    title = result.title ?: "Kein Titel",
                    url = result.url,
                    snippet = result.content ?: ""
                )
            } else null
        } ?: emptyList()

        // Step 2: Also search for images
        val imageResponse = getSearXngService().searchWithImages(query = query)
        val imageBody = imageResponse.body()
        val images = (imageBody?.results ?: emptyList()).mapNotNull { r ->
            if (r.img_src != null) {
                SearchImage(
                    imgUrl = r.img_src,
                    sourceUrl = r.url ?: "",
                    title = r.title ?: "Bild",
                    thumbnailUrl = r.thumbnail_src ?: r.img_src
                )
            } else null
        }

        // Step 3: Send to DeepSeek for analysis
        val analysis = analyzeResults(query, searchResults, iterationNumber, totalIterations, context, mode, reportLength)

        IterationResult(
            iterationNumber = iterationNumber,
            totalIterations = totalIterations,
            searchQuery = query,
            sources = searchResults,
            analysis = analysis.first,
            nextDirection = analysis.second,
            intermediateImages = images
        )
    }

    private suspend fun analyzeResults(
        query: String,
        results: List<SearchResult>,
        iterationNumber: Int,
        totalIterations: Int,
        context: List<IterationResult>,
        mode: IterationMode,
        reportLength: ReportLength
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val sourcesText = results.take(10).joinToString("\n\n") { r ->
            "Titel: ${r.title}\nURL: ${r.url}\nInhalt: ${r.snippet.take(500)}"
        }
        val contextText = context.takeLast(2).joinToString("\n\n") { ctx ->
            "Frühere Analyse (Iteration ${ctx.iterationNumber}): ${ctx.analysis.take(300)}"
        }

        val prompt = buildString {
            appendLine("Iteration $iterationNumber von $totalIterations")
            appendLine()
            appendLine("Suchanfrage: $query")
            appendLine()
            appendLine("Suchergebnisse:")
            appendLine(sourcesText)
            if (contextText.isNotBlank()) {
                appendLine()
                appendLine("Kontext aus vorherigen Iterationen:")
                appendLine(contextText)
            }
            appendLine()
            if (iterationNumber < totalIterations || mode == IterationMode.AUTO) {
                appendLine("Analysiere diese Ergebnisse gründlich und gib die nächste Suchrichtung vor.")
                appendLine("Formatiere die Antwort als JSON mit \"analysis\" und \"next_direction\" und \"sufficient\" (true/false) und \"key_findings\".")
            } else {
                appendLine("Dies ist die letzte Iteration. Analysiere die Ergebnisse und fasse zusammen.")
                appendLine("Formatiere die Antwort als JSON mit \"analysis\" (detailliert), \"next_direction\": \"FINAL\", \"sufficient\": true, \"key_findings\".")
            }
        }

        val request = DeepSeekChatRequest(
            model = settingsProvider.getSelectedModel(),
            messages = listOf(
                ChatMessageDto("system", researchSystemPrompt(reportLength)),
                ChatMessageDto("user", prompt)
            ),
            temperature = 0.5
        )
        val apiKey = settingsProvider.deepSeekApiKey()
        val authHeader = "Bearer $apiKey"
        val response = getDeepSeekService().chatCompletion(authHeader, request)
        val body = response.body()
        val error = body?.error

        if (!response.isSuccessful || error != null) {
            throw Exception(error?.message ?: "HTTP ${response.code()}: ${response.message()}")
        }

        val content = body?.choices?.firstOrNull()?.message?.content
            ?: throw Exception("Leere Antwort von der API")

        // Parse JSON
        val jsonStr = content.trimStart()
            .removePrefix("```json")
            .removePrefix("```")
            .trimEnd()
            .removeSuffix("```")
            .trim()

        val gson = com.google.gson.Gson()
        val map = try {
            gson.fromJson(jsonStr, Map::class.java) as? Map<*, *>
        } catch (e: Exception) {
            null
        }

        val analysis = map?.get("analysis")?.toString() ?: content
        val nextDirection = map?.get("next_direction")?.toString() ?: "Weiter mit vertiefender Suche"

        Pair(analysis, nextDirection)
    }

    /**
     * Generate the final report from all iterations.
     */
    suspend fun generateReport(
        topic: String,
        iterations: List<IterationResult>,
        reportLength: ReportLength,
        selectedImages: List<SearchImage>
    ): String = withContext(Dispatchers.IO) {
        val iterationsSummary = iterations.joinToString("\n\n---\n\n") { it ->
            "**Iteration ${it.iterationNumber}:** Suchanfrage: ${it.searchQuery}\n\n" +
                    "Analyse: ${it.analysis}\n\n" +
                    "Quellen:\n${it.sources.take(5).joinToString("\n") { "- ${it.title}: ${it.url}" }}"
        }

        val imagesSection = if (selectedImages.isNotEmpty()) {
            "\n\n## Gefundene Bilder\n\n" + selectedImages.joinToString("\n\n") { img ->
                "![${img.title}](${img.imgUrl})\n*Quelle: ${img.sourceUrl}*"
            }
        } else ""

        val wordTarget = when (reportLength) {
            ReportLength.SHORT -> "300-500 Wörter"
            ReportLength.MEDIUM -> "800-1200 Wörter"
            ReportLength.LONG -> "2000-4000 Wörter"
        }

        val prompt = """
Erstelle einen professionellen, gut strukturierten Forschungsbericht zum Thema: "$topic"

Nutze die folgenden Recherche-Ergebnisse als Grundlage:

$iterationsSummary

Anforderungen:
- Umfang: $wordTarget
- Format: Markdown mit Überschriften (##, ###), Aufzählungen, und ggf. Tabellen
- Struktur: Titel, Einleitung, Hauptteil mit Zwischenüberschriften, Fazit, Quellenverzeichnis
- Alle Quellen aus den Iterationen am Ende im Quellenverzeichnis aufführen

$imagesSection

Erstelle einen wissenschaftlich fundierten, gut lesbaren Bericht.
        """.trimIndent()

        val request = DeepSeekChatRequest(
            model = settingsProvider.getSelectedModel(),
            messages = listOf(
                ChatMessageDto("system", "Du bist ein professioneller wissenschaftlicher Autor. Erstelle einen fundierten Bericht."),
                ChatMessageDto("user", prompt)
            ),
            temperature = 0.3,
            maxTokens = 8192
        )
        val apiKey = settingsProvider.deepSeekApiKey()
        val authHeader = "Bearer $apiKey"
        val response = getDeepSeekService().chatCompletion(authHeader, request)
        val body = response.body()
        val error = body?.error

        if (!response.isSuccessful || error != null) {
            throw Exception(error?.message ?: "HTTP ${response.code()}: ${response.message()}")
        }

        body?.choices?.firstOrNull()?.message?.content
            ?: throw Exception("Konnte keinen Bericht generieren")
    }

    /**
     * Generate a follow-up answer for discussion mode.
     */
    suspend fun generateDiscussionAnswer(
        report: String,
        question: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val historyText = history.takeLast(10).joinToString("\n") { msg ->
            "${if (msg.role == ChatRole.USER) "User" else "Assistent"}: ${msg.content.take(300)}"
        }

        val prompt = """
Beziehe dich auf den folgenden Forschungsbericht, um die Frage zu beantworten.

BERICHT:
$report

VERLAUF:
$historyText

FRAGE:
$question

Antworte ausführlich und fundiert, basierend auf dem Bericht.
        """.trimIndent()

        val request = DeepSeekChatRequest(
            model = settingsProvider.getSelectedModel(),
            messages = listOf(
                ChatMessageDto("system", "Du bist ein hilfreicher Assistent, der Fragen zu einem Forschungsbericht beantwortet."),
                ChatMessageDto("user", prompt)
            ),
            temperature = 0.5
        )
        val apiKey = settingsProvider.deepSeekApiKey()
        val authHeader = "Bearer $apiKey"
        val response = getDeepSeekService().chatCompletion(authHeader, request)
        val body = response.body()
        val error = body?.error

        if (!response.isSuccessful || error != null) {
            throw Exception(error?.message ?: "HTTP ${response.code()}: ${response.message()}")
        }

        body?.choices?.firstOrNull()?.message?.content
            ?: throw Exception("Konnte keine Antwort generieren")
    }

    /**
     * Test DeepSeek connection with a simple request.
     */
    suspend fun testDeepSeekConnection(): String = withContext(Dispatchers.IO) {
        val request = DeepSeekChatRequest(
            model = settingsProvider.getSelectedModel(),
            messages = listOf(
                ChatMessageDto("user", "Antworte mit 'OK' wenn du erreichbar bist.")
            ),
            maxTokens = 10,
            temperature = 0.0
        )
        val apiKey = settingsProvider.deepSeekApiKey()
        val authHeader = "Bearer $apiKey"
        val response = getDeepSeekService().chatCompletion(authHeader, request)
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.error != null) {
                "Fehler: ${body.error.message}"
            } else {
                "✅ Verbindung erfolgreich! Modell antwortet."
            }
        } else {
            "❌ Fehler ${response.code()}: ${response.message()}"
        }
    }

    /**
     * Load available models from DeepSeek.
     */
    suspend fun loadModels(): List<String> = withContext(Dispatchers.IO) {
        val apiKey = settingsProvider.deepSeekApiKey()
        val authHeader = "Bearer $apiKey"
        val response = getDeepSeekService().listModels(authHeader)
        val body = response.body()
        if (response.isSuccessful && body?.data != null) {
            body.data.map { it.id }
        } else {
            throw Exception(body?.error?.message ?: "Konnte Modelle nicht laden")
        }
    }

    /**
     * Test SearXNG connection.
     */
    suspend fun testSearXNGConnection(): String = withContext(Dispatchers.IO) {
        try {
            val response = getSearXngService().getConfig()
            if (response.isSuccessful) {
                val body = response.body()
                "✅ Verbindung erfolgreich! ${body?.instance_name ?: "SearXNG"} (v${body?.version ?: "?"})"
            } else {
                // Fallback: try a search
                val searchResp = getSearXngService().search(q = "test", format = "json")
                if (searchResp.isSuccessful) {
                    "✅ Verbindung erfolgreich! (SearXNG)"
                } else {
                    "❌ Fehler ${response.code()}: ${response.message()}"
                }
            }
        } catch (e: Exception) {
            "❌ Fehler: ${e.localizedMessage ?: "Verbindung fehlgeschlagen"}"
        }
    }
}
