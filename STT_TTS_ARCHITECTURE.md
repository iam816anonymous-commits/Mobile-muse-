# STT_TTS_ARCHITECTURE.md — Speech Input & Output Subsystem Architecture

## 1. Executive Summary & Multilingual Scope

The **Speech Subsystem** enables hands-free voice interaction, translating spoken user utterances into normalized commands and providing synthesized audio feedback.

### Key Architectural Constraints
1. **Pluggable Provider Architecture:** Speech-to-Text (STT) and Text-to-Speech (TTS) are wrapped behind provider interfaces. Core command execution has zero direct coupling to speech engines.
2. **API 27 Compatibility Baseline:** Speech recognition on Android 8.1 relies on Android's `SpeechRecognizer` platform API. On-device offline STT API (`SpeechRecognizer.isOnDeviceRecognitionAvailable()`) is used when running on API 31+; online/system fallback is used on API 27–30.
3. **Multilingual Scope:**
   - **English (EN)**
   - **Telugu (TE)**
   - **Kannada (KN)**
   - **Hindi (HI)**
4. **On-Demand Lifecycle:** Speech engines consume significant RAM when active. Speech recognizers and TTS engines are **initialized on-demand and destroyed immediately when idle**.

---

## 2. Pluggable Provider Interfaces

```kotlin
interface SpeechInputProvider {
    fun initialize(context: Context, languageCode: String): Boolean
    fun startListening(listener: SpeechInputListener)
    fun stopListening()
    fun destroy()
    fun isLanguageSupported(languageCode: String): Boolean
    fun isOfflineRecognitionSupported(): Boolean
}

interface SpeechInputListener {
    fun onSpeechReady()
    fun onSpeechStart()
    fun onPartialResults(partialText: String)
    fun onFinalResults(spokenText: String, confidence: Float)
    fun onError(errorCode: Int, errorMessage: String)
}

interface SpeechOutputProvider {
    fun initialize(context: Context, onReady: (Boolean) -> Unit)
    fun speak(textToSpeak: String, languageCode: String, utteranceId: String)
    fun stop()
    fun shutdown()
    fun isLanguageAvailable(languageCode: String): Boolean
}
```

---

## 3. STT / TTS Subsystem Architecture

```text
               ┌───────────────────────────────────────────────┐
               │           User Spoken Utterance               │
               │      (English, Telugu, Kannada, Hindi)        │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │            SpeechInputProvider                │
               │   Wraps Android SpeechRecognizer (API 27+)    │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │         Multilingual Command Normalizer       │
               │   Maps "కలిపించు 7" / "click seven" → CLICK(7) │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │           Universal GoalDispatcher            │
               │   Executes action via Universal Core Pipeline  │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │            SpeechOutputProvider               │
               │   Synthesizes status response via TextToSpeech│
               └───────────────────────────────────────────────┘
```

---

## 4. Multilingual Language Resolution & Capability Detection

On Android 8.1 (API 27), offline voice recognition models for Indian regional languages (Telugu, Kannada, Hindi) are not guaranteed to be pre-installed on all OEM ROMs.

### Language Resolution Protocol
1. **Locale Query:** Query `TextToSpeech.isLanguageAvailable(Locale(languageCode))` and `RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS`.
2. **Capability Check:**
   - If language model is available offline → Execute local STT/TTS.
   - If language model requires network and device is online → Execute system recognizer.
   - If language model unavailable → Gracefully inform user via TTS in fallback language (English) and log `STT_LANGUAGE_UNAVAILABLE`.

### Multilingual Command Syntax Normalization Rules

| Language | Spoken Utterance Sample | Normalized Intent |
|---|---|---|
| **English** | "Click seven" / "Open Settings" | `UI_CLICK("7")` / `APP_LAUNCH("com.android.settings")` |
| **Telugu** | "ఏడు మీద నొక్కు" / "సెట్టింగ్స్ ఓపెన్ చేయి" | `UI_CLICK("7")` / `APP_LAUNCH("com.android.settings")` |
| **Kannada** | "ಏಳು ಕ್ಲಿಕ್ ಮಾಡಿ" / "ಸೆಟ್ಟಿಂಗ್‌ಗಳನ್ನು ತೆರೆಯಿರಿ" | `UI_CLICK("7")` / `APP_LAUNCH("com.android.settings")` |
| **Hindi** | "सात पर क्लिक करें" / "सेटिंग्स खोलो" | `UI_CLICK("7")` / `APP_LAUNCH("com.android.settings")` |

---

## 5. Resource Management & Lifecycle Discipline

To prevent speech engines from exhausting memory on low-RAM devices:

1. **Destroy After Speech Recognition:** Upon receiving `onFinalResults()` or `onError()`, call `SpeechRecognizer.destroy()` immediately.
2. **TTS Shutdown:** Call `TextToSpeech.shutdown()` when the agent transitions to `IDLE` state or when `ResourceManager` triggers `TRIM_MEMORY_RUNNING_LOW`.
3. **No Continuous Background Listening:** Speech input is activated exclusively by user tap on Overlay/Console mic icon or hardware voice key event.
