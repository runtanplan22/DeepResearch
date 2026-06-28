# DeepResearch – KI-gestützte Tiefenrecherche

Native Android-App für mehrstufige, KI-gestützte Recherche mit DeepSeek + SearXNG.

## Features

- **Rechercheplan**: KI erstellt strukturierten Plan (Unterfragen, Suchrichtungen)
- **Mehrstufige Recherche**: Automatische oder manuelle Iterationen (3/5/7/10)
- **Live-Ansicht**: Echtzeit-Fortschritt mit einklappbaren Iterationsdetails
- **Bericht**: Formatierter Markdown-Bericht mit Bildern aus Suchergebnissen
- **PDF-Export**: Formatierter PDF-Bericht mit Teilen-Funktion
- **Diskussion**: Chat-basierte Rückfragen zum fertigen Bericht
- **API-Konfiguration**: DeepSeek + SearXNG Endpunkte mit Verbindungstest
- **Sichere Speicherung**: API-Keys via EncryptedSharedPreferences

## Voraussetzungen

- **Android Studio** Hedgehog (2023.1.1) oder neuer
- **Android SDK**: API 26+ (Android 8.0 Oreo)
- **Gradle**: 8.5 (Wrapper inkludiert)
- **JDK**: 17

## Benötigte externe Dienste

1. **DeepSeek API** – KI-Modell für Planung, Analyse und Berichterstellung
   - Account unter https://platform.deepseek.com
   - API-Key generieren
   
2. **SearXNG Instanz** – Metasuchmaschine für Recherche-Quellen
   - Eigenes Hosting: `docker run -d -p 8888:8080 searxng/searxng`
   - Oder öffentliche Instanz (Achtung: Datenschutz)

## Projekt in Android Studio öffnen

1. **Repository klonen oder Ordner öffnen:**
   - `File → Open → DeepResearch/deepresearch/` auswählen

2. **Sync gradle:**
   - Bei erstem Öffnen fragt Android Studio nach Sync → **"Sync Now"**
   - Falls nicht: `File → Sync Project with Gradle Files`

3. **SDK-Pfad prüfen:**
   - `File → Project Structure → SDK Location`
   - Pfad zum Android SDK muss korrekt sein

4. **App bauen:**
   - `Build → Make Project` (Strg+F9 / Cmd+F9)

5. **Starten:**
   - Emulator (API 26+) oder physisches Gerät wählen
   - `Run → Run 'app'`

## Erste Schritte in der App

1. **API-Konfiguration** über das Zahnrad-Icon ⚙️ auf der Startseite:
   
   **DeepSeek:**
   - Base URL: `https://api.deepseek.com`
   - API-Key eintragen
   - "Speichern" klicken
   - "Modelle laden" → verfügbare Modelle erscheinen im Dropdown
   
   **SearXNG:**
   - Base URL: z. B. `http://10.0.2.2:8888` (Emulator → localhost)
   - "Speichern" klicken
   - "Verbindung testen" → bei Erfolg erscheint Bestätigung

2. **Recherche starten:**
   - Thema eingeben (z. B. "Auswirkungen von KI auf den Arbeitsmarkt")
   - Berichtslänge und Iterationsmodus wählen
   - "Rechercheplan erstellen"
   - Plan prüfen → "Research starten"

3. **Ergebnisse:**
   - Live-Verfolgung der Iterationen
   - Fertigen Bericht ansehen, Bilder auswählen
   - Als PDF exportieren
   - Diskussion starten

## Projektstruktur

```
app/src/main/java/com/deepresearch/app/
├── data/
│   ├── api/           # Retrofit Services + DTOs
│   ├── local/         # Room DB + EncryptedSettings
│   ├── model/         # Domain-Modelle
│   └── repository/    # ResearchRepository (API-Orchestrierung)
├── ui/
│   ├── components/    # MarkdownText Renderer
│   ├── navigation/    # Routes
│   ├── screens/       # 5 Screens (Home, Plan, Research, Report, Settings)
│   └── theme/         # Material3 Theme (Light + Dark)
└── viewmodel/         # ResearchViewModel
```

## Abhängigkeiten

- Jetpack Compose + Material3 (UI)
- Retrofit + OkHttp (Netzwerk)
- Room (Cache)
- EncryptedSharedPreferences (API-Keys)
- Coil (Bilder)
- iText 7 (PDF)

## Mindestanforderungen

- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34
- **Sprache**: Kotlin 1.9.22
- **Portrait-Modus** (fest eingestellt)
