# First Setup Linux - Erfahrungsbericht

**Datum:** 2025-12-08
**Testsystem:** Ubuntu Linux (frische Installation, kein Entwicklungsrechner)
**Version:** 0.6.8

## Zusammenfassung

Dieser Bericht dokumentiert alle Probleme, die bei der ersten Installation von Fleet Navigator auf einem neuen Linux-System auftraten, sowie deren Lösungen.

---

## Problem 1: 403 Forbidden bei Model-API-Aufrufen

### Symptom
Beim Setzen eines Modells als Default oder beim Herunterladen von Modellen erschien ein `403 Forbidden` Fehler.

### Ursache
Die CSRF-Protection in Spring Security blockierte API-Anfragen an `/api/models/**` und `/api/model-store/**`.

### Lösung
In `SecurityConfig.java` wurden die Model-Endpoints zur CSRF-Ignore-Liste hinzugefügt:

```java
.ignoringRequestMatchers(
    "/api/chat/**",
    "/api/models/**",         // Model API (includes setting default)
    "/api/model-store/**"     // Model Store API (downloads)
)
```

---

## Problem 2: Default-Modell "phi:latest" nicht vorhanden

### Symptom
Nach dem Download eines Modells wurde `phi:latest` als Default angezeigt, obwohl dieses Modell nicht existierte.

### Ursache
Der Fallback in `ModelController.java` war hartcodiert auf `phi:latest`.

### Lösung
Der Fallback wurde geändert, um das erste verfügbare Modell zu verwenden:

```java
@GetMapping("/default")
public ResponseEntity<Map<String, String>> getDefaultModel() {
    Optional<ModelMetadata> defaultModel = metadataService.getDefaultModel();
    if (defaultModel.isPresent() && !"phi:latest".equals(defaultModel.get().getName())) {
        return ResponseEntity.ok(Map.of("model", defaultModel.get().getName()));
    }

    // Fallback: Erstes verfügbares Modell verwenden
    try {
        List<ModelInfo> availableModels = llmProviderService.getAvailableModels();
        if (!availableModels.isEmpty()) {
            String firstModel = availableModels.get(0).getName();
            return ResponseEntity.ok(Map.of("model", firstModel));
        }
    } catch (IOException e) {
        log.warn("Could not fetch available models for fallback: {}", e.getMessage());
    }
    return ResponseEntity.ok(Map.of("model", ""));
}
```

---

## Problem 3: llama-server Binary nicht gefunden

### Symptom
Der llama-server startete nicht, Fehlermeldung: "Binary nicht gefunden".

### Ursache
Auf dem neuen System war kein llama-server Binary vorhanden. Das Binary muss separat heruntergeladen werden.

### Lösung
1. **Download von GitHub Releases:**
   ```bash
   cd /home/trainer/Fleet-Navigator
   mkdir -p bin/llama-b7325
   cd bin/llama-b7325

   # CPU-Version herunterladen
   wget https://github.com/ggml-org/llama.cpp/releases/download/b7325/llama-b7325-bin-ubuntu-x64.tar.gz
   tar -xzf llama-b7325-bin-ubuntu-x64.tar.gz
   chmod +x llama-server
   ```

2. **Verzeichnisstruktur für CPU und CUDA:**
   ```
   bin/
   ├── llama-b7325/          # CPU-only Version
   │   ├── llama-server
   │   ├── libllama.so
   │   ├── libggml.so
   │   └── ...
   └── llama-cuda/           # CUDA-Version (optional)
       ├── llama-server
       └── ...
   ```

---

## Problem 4: Automatische GPU-Erkennung fehlte

### Symptom
Es gab keine automatische Unterscheidung zwischen CPU- und CUDA-Version des llama-servers.

### Ursache
Die `LlamaServerProcessManager` prüfte nicht, ob eine NVIDIA GPU verfügbar ist.

### Lösung
GPU-Erkennung via `nvidia-smi` implementiert:

```java
private boolean checkNvidiaGpu() {
    try {
        ProcessBuilder pb = new ProcessBuilder("nvidia-smi", "--query-gpu=name", "--format=csv,noheader");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean finished = p.waitFor(5, TimeUnit.SECONDS);
        if (finished && p.exitValue() == 0) {
            String output = new String(p.getInputStream().readAllBytes()).trim();
            if (!output.isEmpty()) {
                log.info("🎮 NVIDIA GPU erkannt: {}", output);
                return true;
            }
        }
    } catch (Exception e) {
        log.debug("nvidia-smi check failed: {}", e.getMessage());
    }
    return false;
}
```

Die `resolveBinaryPath()` Methode sucht dann nach dem passenden Binary:
- Mit GPU: Erst `bin/llama-cuda/`, dann Fallback auf CPU
- Ohne GPU: Direkt `bin/llama-b7325/` (CPU-only)

---

## Problem 5: llama-server startete nicht automatisch nach Model-Download

### Symptom
Nach dem Download eines Modells musste der Server manuell gestartet werden.

### Ursache
Der `ModelStoreController` startete den llama-server nicht nach dem Download.

### Lösung
In `ModelStoreController.java` nach erfolgreichem Download:

```java
// Nach Download: Als Default setzen und llama-server starten
try {
    var currentDefault = modelMetadataService.getDefaultModel();
    if (currentDefault.isEmpty() || "phi:latest".equals(currentDefault.get().getName())) {
        modelMetadataService.setDefaultModel(filename);
        log.info("✅ Downloaded model {} set as default", filename);
    }

    // Start llama-server if not already running
    LlamaServerProcessManager.ServerStatus status = llamaServerManager.getStatus();
    if (!status.isOnline()) {
        log.info("🚀 Starting llama-server with newly downloaded model: {}", filename);
        LlamaServerProcessManager.StartResult result = llamaServerManager.startServer(
            filename, 2026, 8192, 99
        );
        if (result.isSuccess()) {
            log.info("✅ llama-server gestartet auf Port {}", result.getPort());
        }
    }
} catch (Exception e) {
    log.warn("Could not set downloaded model as default or start llama-server: {}", e.getMessage());
}
```

---

## Problem 6: LD_LIBRARY_PATH nicht korrekt gesetzt

### Symptom
llama-server startete, konnte aber shared libraries (.so Dateien) nicht finden.

### Ursache
LD_LIBRARY_PATH war nicht auf das Verzeichnis mit den .so Dateien gesetzt.

### Lösung
In `LlamaServerProcessManager.startServer()`:

```java
// LD_LIBRARY_PATH auf das Binary-Verzeichnis setzen
Path binaryDir = Paths.get(binaryPath).getParent();
if (binaryDir != null) {
    String ldPath = binaryDir.toAbsolutePath().toString();
    String existingLdPath = env.get("LD_LIBRARY_PATH");
    if (existingLdPath != null && !existingLdPath.isEmpty()) {
        ldPath = ldPath + ":" + existingLdPath;
    }
    env.put("LD_LIBRARY_PATH", ldPath);
    log.info("🔧 LD_LIBRARY_PATH set to: {}", ldPath);
}
```

---

## Checkliste für neue Linux-Installation

1. [ ] Java 17+ installiert (`java -version`)
2. [ ] Fleet Navigator JAR vorhanden
3. [ ] llama-server Binary heruntergeladen und entpackt in `bin/llama-b7325/`
4. [ ] (Optional) CUDA-Version in `bin/llama-cuda/` für GPU-Beschleunigung
5. [ ] Ausführungsrechte gesetzt (`chmod +x bin/llama-b7325/llama-server`)
6. [ ] Port 2025 (Web) und 2026 (llama-server) verfügbar
7. [ ] Mindestens ein GGUF-Modell heruntergeladen

---

## Empfohlene llama-server Downloads

### CPU-only (Ubuntu/Debian x64)
```bash
wget https://github.com/ggml-org/llama.cpp/releases/download/b7325/llama-b7325-bin-ubuntu-x64.tar.gz
```

### CUDA 12.x (Ubuntu/Debian x64)
```bash
wget https://github.com/ggml-org/llama.cpp/releases/download/b7325/llama-b7325-bin-ubuntu-x64-cuda-cu12.4.1.tar.gz
```

---

## Bekannte Einschränkungen

- **Erste Installation ohne Modelle:** Der SetupWizard führt durch den Download des ersten Modells
- **CUDA erfordert NVIDIA-Treiber:** Die CUDA-Version funktioniert nur mit installierten NVIDIA-Treibern
- **java-llama-cpp Fallback:** Wenn kein llama-server Binary gefunden wird, wird die JNI-basierte Inferenz verwendet (langsamer, aber funktioniert)

---

## Nächste Schritte

- [ ] `firstSetupWindows.md` erstellen für Windows-spezifische Probleme
- [ ] Automatischer Download von llama-server im SetupWizard (TODO)
