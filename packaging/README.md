# Packaging - Native Libraries

Dieses Verzeichnis enthält plattformspezifische native Bibliotheken für Fleet Navigator.

## Struktur

```
packaging/
├── linux-x64/
│   ├── ocr/           # Tesseract + tessdata
│   └── native/llama/  # llama.cpp JNI (.so)
├── win-x64/
│   ├── ocr/           # Tesseract + tessdata
│   └── native/llama/  # llama.cpp JNI (.dll)
└── macos-arm64/
    ├── ocr/           # Tesseract + tessdata
    └── native/llama/  # llama.cpp JNI (.dylib)
```

## Benötigte Dateien

### llama.cpp JNI
- **Linux:** `libllama.so`, `libjllama.so`
- **Windows:** `llama.dll`, `jllama.dll`
- **macOS:** `libllama.dylib`, `libjllama.dylib`

### OCR (Optional)
- Tesseract Binary
- tessdata (Sprachmodelle: deu, eng)

## Hinweis

Diese Verzeichnisse können leer sein. Die GitHub Action kopiert nur vorhandene Dateien.
Modelle (.gguf) werden NICHT hier abgelegt - diese lädt der User beim ersten Start von Hugging Face.
