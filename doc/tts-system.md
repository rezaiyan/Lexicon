# Text-to-Speech System

## Architecture

```
SpeakWordUseCase
    └── ITtsRepository (TtsRepositoryImpl)
         ├── ITtsEngine (platform-specific)
         │    ├── AndroidTtsEngine (Sherpa ONNX + AudioTrack)
         │    └── IosTtsEngine (Sherpa ONNX + AVAudioPlayer)
         │
         ├── IModelFileManager (platform-specific)
         │    ├── AndroidModelFileManager (Apache Commons Compress)
         │    └── IosModelFileManager (bz2lib + custom tar)
         │
         └── LanguageModelMapping (model URLs)
```

## Supported Languages (13)

| Code | Language | Model |
|------|----------|-------|
| en | English (US) | vits-piper-en_US-kristin-medium |
| de | German | vits-piper-de_DE-thorsten-medium |
| es | Spanish (Mexico) | vits-piper-es_MX-ald-medium |
| fr | French | vits-piper-fr_FR-siwis-medium |
| it | Italian | vits-piper-it_IT-riccardo-x_low |
| pt | Portuguese (Brazil) | vits-piper-pt_BR-faber-medium |
| ru | Russian | vits-piper-ru_RU-ruslan-medium |
| zh | Chinese | vits-piper-zh_CN-huayan-medium |
| tr | Turkish | vits-piper-tr_TR-fettah-medium |
| nl | Dutch | vits-piper-nl_NL-miro-high |
| ar | Arabic | vits-piper-ar_JO-kareem-medium |
| hi | Hindi | vits-piper-hi_IN-rohan-medium |
| fa | Persian | vits-piper-fa-haaniye_low |

## TTS State Machine
```kotlin
sealed class TtsState {
    data object Idle
    data class Downloading(val languageCode: String, val progress: Float)
    data object Loading
    data object Speaking
    data class Error(val message: String)
}
```

## Speak Flow

```
SpeakWordUseCase(text, languageCode)
    │
    ├── Normalize language code
    ├── Check if language supported
    │    └── No → fallback to user's current language
    │
    ├── Check if model downloaded
    │    └── No → downloadModel(languageCode)
    │         ├── State: Downloading(lang, 0.0 → 1.0)
    │         ├── Downloads tar.bz2 from GitHub releases
    │         └── Extracts .onnx + tokens.txt + espeak-ng-data
    │
    ├── State: Loading
    ├── Initialize engine with model files
    │
    ├── State: Speaking
    ├── Synthesize audio from text
    ├── Play audio
    │
    └── State: Idle
```

## Model Storage
- **Android**: App-specific storage (internal/external)
- **iOS**: App Documents directory
- Models are Piper VITS models from sherpa-onnx GitHub releases
- Each model: .onnx file + tokens.txt + espeak-ng-data directory

## Model Download
- Archives: `.tar.bz2` format
- Hosted on GitHub releases (sherpa-onnx project)
- Progress tracked via `Flow<Float>` (0.0 to 1.0)
- Handles GitHub redirects for download URLs

## Platform Details

### Android
- **Engine**: Sherpa ONNX (`com.k2fsa.sherpa.onnx`)
- **Playback**: `AudioTrack` with sample rate from VITS config
- **Extraction**: Apache Commons Compress (BZip2CompressorInputStream + TarArchiveInputStream)

### iOS
- **Engine**: Sherpa ONNX (C interop via cinterop)
- **Playback**: `AVAudioPlayer` with WAV file output
- **Extraction**: bz2lib (C interop) + custom tar parser

### Web
- Stub implementation (TTS not available)

## Integration with UI
- TTS state exposed via `StudyViewModel.ttsState: StateFlow<TtsState>`
- Review screen shows TTS button per word
- Tapping speaks the word in its source language
- Stop button available during playback
