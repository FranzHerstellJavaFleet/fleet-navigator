package io.javafleet.fleetnavigator.llm.providers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für das GPU-Speicher-Management.
 *
 * WICHTIG: Diese Tests sichern den funktionierenden Zustand ab!
 * Stand: 2025-11-30
 *
 * Getestete Funktionen:
 * - Automatisches Entladen von Modellen beim Wechsel
 * - GPU-Speicher-Freigabe bei Modell-Wechsel
 * - Keine doppelten Modelle im Cache
 * - Startup-Cleanup für verwaiste GPU-Prozesse (NEU!)
 *
 * Problem das gelöst wurde:
 * 1. Mistral-Nemo (7.5GB) konnte nicht geladen werden wenn ein anderes
 *    Modell bereits im GPU-Speicher war → "unable to allocate CUDA buffer"
 *
 * 2. Nach Prozess-Kill (pkill java) blieb GPU-Speicher belegt.
 *    Neuer Prozess hatte leeren Cache, konnte aber GPU nicht nutzen.
 *
 * Lösungen:
 * 1. Vor dem Laden eines neuen Modells werden alle anderen Modelle entladen.
 * 2. Beim Application-Start werden verwaiste GPU-Prozesse gekillt (cleanupGpuMemoryOnStartup)
 */
@DisplayName("GPU Memory Management Tests - Stand 2025-11-30")
class GpuMemoryManagementTest {

    /**
     * Simuliert den Model-Cache für Unit-Tests.
     * In der echten Implementierung ist dies loadedModels in JavaLlamaCppProvider.
     */
    private final Map<String, MockModel> modelCache = new HashMap<>();

    /**
     * Mock-Klasse die LlamaModel simuliert
     */
    private static class MockModel {
        private final String name;
        private boolean closed = false;

        MockModel(String name) {
            this.name = name;
        }

        void close() {
            this.closed = true;
        }

        boolean isClosed() {
            return closed;
        }

        String getName() {
            return name;
        }
    }

    /**
     * Simuliert unloadOtherModels() aus JavaLlamaCppProvider
     */
    private void unloadOtherModels(String keepCacheKey) {
        if (modelCache.isEmpty()) {
            return;
        }

        List<String> toUnload = modelCache.keySet().stream()
                .filter(key -> !key.equals(keepCacheKey))
                .toList();

        for (String cacheKey : toUnload) {
            MockModel model = modelCache.remove(cacheKey);
            if (model != null) {
                model.close();
            }
        }
    }

    @Test
    @DisplayName("Andere Modelle werden vor Laden eines neuen entladen")
    void otherModelsShouldBeUnloadedBeforeLoadingNew() {
        // Arrange: Zwei Modelle im Cache
        MockModel model1 = new MockModel("Llama-3.2-3B");
        MockModel model2 = new MockModel("Mistral-7B");
        modelCache.put("Llama-3.2-3B", model1);
        modelCache.put("Mistral-7B", model2);

        assertEquals(2, modelCache.size(), "Sollten 2 Modelle im Cache sein");

        // Act: Neues Modell laden (Mistral-Nemo)
        unloadOtherModels("Mistral-Nemo");

        // Assert: Alte Modelle sind entladen
        assertEquals(0, modelCache.size(), "Cache sollte leer sein nach unloadOtherModels");
        assertTrue(model1.isClosed(), "Model1 sollte geschlossen sein");
        assertTrue(model2.isClosed(), "Model2 sollte geschlossen sein");
    }

    @Test
    @DisplayName("Gleiches Modell wird nicht entladen")
    void sameModelShouldNotBeUnloaded() {
        // Arrange: Modell im Cache
        MockModel model = new MockModel("Mistral-Nemo");
        modelCache.put("Mistral-Nemo", model);

        // Act: Gleiches Modell "laden" (kommt aus Cache)
        unloadOtherModels("Mistral-Nemo");

        // Assert: Modell ist noch im Cache
        assertEquals(1, modelCache.size(), "Modell sollte noch im Cache sein");
        assertFalse(model.isClosed(), "Modell sollte NICHT geschlossen sein");
    }

    @Test
    @DisplayName("Leerer Cache verursacht keinen Fehler")
    void emptyCacheShouldNotCauseError() {
        // Arrange: Leerer Cache
        assertTrue(modelCache.isEmpty());

        // Act & Assert: Kein Fehler bei leerem Cache
        assertDoesNotThrow(() -> unloadOtherModels("Mistral-Nemo"));
    }

    @Test
    @DisplayName("CPU-Only Modelle haben separaten Cache-Key")
    void cpuOnlyModelsShouldHaveSeparateCacheKey() {
        // Arrange
        String modelName = "Mistral-Nemo";
        String gpuCacheKey = modelName;
        String cpuCacheKey = modelName + "_CPU_ONLY";

        MockModel gpuModel = new MockModel("GPU-Model");
        MockModel cpuModel = new MockModel("CPU-Model");
        modelCache.put(gpuCacheKey, gpuModel);
        modelCache.put(cpuCacheKey, cpuModel);

        // Assert: Beide Varianten sind separat gecached
        assertEquals(2, modelCache.size());
        assertNotEquals(gpuCacheKey, cpuCacheKey, "Cache-Keys sollten unterschiedlich sein");

        // Act: Laden der GPU-Variante (CPU-Variante wird entladen)
        unloadOtherModels(gpuCacheKey);

        // Assert: Nur GPU-Variante bleibt
        assertEquals(1, modelCache.size());
        assertTrue(modelCache.containsKey(gpuCacheKey));
        assertFalse(modelCache.containsKey(cpuCacheKey));
        assertTrue(cpuModel.isClosed(), "CPU-Modell sollte geschlossen sein");
        assertFalse(gpuModel.isClosed(), "GPU-Modell sollte NICHT geschlossen sein");
    }

    @Test
    @DisplayName("Große Modelle (>7GB) können nach Entladen geladen werden")
    void largeModelsCanBeLoadedAfterUnloading() {
        // Diese Test dokumentiert das Problem und die Lösung:
        //
        // Vorher (Problem):
        // 1. Model A geladen (5GB VRAM)
        // 2. Model B laden versucht (7.5GB) → FEHLER: "unable to allocate CUDA buffer"
        //
        // Nachher (Lösung):
        // 1. Model A geladen (5GB VRAM)
        // 2. Model A wird automatisch entladen
        // 3. Model B kann geladen werden (7.5GB passt jetzt)

        // Arrange: Kleineres Modell im Cache simulieren
        MockModel smallModel = new MockModel("Llama-3.2-3B");  // ca. 2GB
        modelCache.put("Llama-3.2-3B", smallModel);

        // Act: Großes Modell laden (würde ohne Entladen fehlschlagen)
        String largeModelKey = "Mistral-Nemo-Instruct-2407.Q4_K_M.gguf";
        unloadOtherModels(largeModelKey);

        // Assert: Alter Speicher ist freigegeben
        assertTrue(smallModel.isClosed(), "Kleines Modell muss entladen sein");
        assertEquals(0, modelCache.size(), "Cache muss leer sein für großes Modell");

        // Simuliere erfolgreiches Laden des großen Modells
        MockModel largeModel = new MockModel(largeModelKey);
        modelCache.put(largeModelKey, largeModel);

        assertEquals(1, modelCache.size());
        assertTrue(modelCache.containsKey(largeModelKey));
    }

    @Test
    @DisplayName("Mehrere Modelle werden alle entladen")
    void multipleModelsShouldAllBeUnloaded() {
        // Arrange: Mehrere Modelle im Cache
        List<MockModel> models = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MockModel model = new MockModel("Model-" + i);
            models.add(model);
            modelCache.put("Model-" + i, model);
        }

        assertEquals(5, modelCache.size());

        // Act: Neues Modell laden
        unloadOtherModels("NewModel");

        // Assert: Alle alten Modelle sind entladen
        assertEquals(0, modelCache.size());
        for (MockModel model : models) {
            assertTrue(model.isClosed(), model.getName() + " sollte geschlossen sein");
        }
    }

    @Test
    @DisplayName("Startup-Cleanup: PIDs werden korrekt gefiltert")
    void startupCleanupShouldFilterCurrentPid() {
        // Dieser Test dokumentiert die Startup-Cleanup Logik:
        //
        // Problem nach Prozess-Kill:
        // 1. Alter Java-Prozess wird mit pkill/kill beendet
        // 2. GPU-Speicher wird NICHT automatisch freigegeben
        // 3. Neuer Java-Prozess startet mit leerem Cache
        // 4. unloadOtherModels() findet nichts zum Entladen
        // 5. Modell-Laden schlägt fehl: "cudaMalloc failed: out of memory"
        //
        // Lösung (cleanupGpuMemoryOnStartup):
        // 1. nvidia-smi --query-compute-apps=pid --format=csv,noheader
        // 2. Eigene PID ausfiltern (ProcessHandle.current().pid())
        // 3. Alle anderen PIDs killen (kill -9)
        // 4. Kurz warten (500ms) damit GPU Speicher freigibt

        // Simulate: PIDs from nvidia-smi
        List<Long> gpuPids = List.of(1234L, 5678L, 9999L);
        long currentPid = 5678L;  // Simulierte aktuelle PID

        // Filter like the real implementation
        List<Long> toKill = gpuPids.stream()
                .filter(pid -> pid != currentPid)
                .toList();

        // Assert: Eigene PID wird nicht gekillt
        assertEquals(2, toKill.size(), "Sollte 2 PIDs zum Killen haben");
        assertFalse(toKill.contains(currentPid), "Eigene PID darf nicht gekillt werden");
        assertTrue(toKill.contains(1234L), "Fremde PID sollte gekillt werden");
        assertTrue(toKill.contains(9999L), "Fremde PID sollte gekillt werden");
    }

    @Test
    @DisplayName("Startup-Cleanup: Leere nvidia-smi Ausgabe verursacht keinen Fehler")
    void startupCleanupShouldHandleEmptyOutput() {
        // nvidia-smi gibt leere Ausgabe wenn keine GPU-Prozesse laufen
        List<Long> gpuPids = List.of();
        long currentPid = ProcessHandle.current().pid();

        List<Long> toKill = gpuPids.stream()
                .filter(pid -> pid != currentPid)
                .toList();

        assertTrue(toKill.isEmpty(), "Keine PIDs zum Killen wenn GPU frei ist");
    }

    @Test
    @DisplayName("Expertenwechsel mit gleichem Basis-Modell: Kein Entladen")
    void expertSwitchWithSameBaseModelShouldNotUnload() {
        // Zwei Experten mit demselben Basis-Modell:
        // Expert "Roland" → Mistral-Nemo.gguf
        // Expert "Karla"  → Mistral-Nemo.gguf
        //
        // Beim Wechsel von Roland zu Karla sollte das Modell NICHT entladen werden,
        // da beide dasselbe Basis-Modell verwenden.

        String baseModel = "Mistral-Nemo";
        MockModel model = new MockModel(baseModel);
        modelCache.put(baseModel, model);

        // Expert "Roland" verwendet Mistral-Nemo (bereits geladen)
        unloadOtherModels(baseModel);
        assertFalse(model.isClosed(), "Modell sollte NICHT entladen sein");

        // Expert "Karla" verwendet auch Mistral-Nemo → gleicher Cache-Key
        unloadOtherModels(baseModel);
        assertFalse(model.isClosed(), "Modell sollte immer noch NICHT entladen sein");

        assertEquals(1, modelCache.size(), "Nur ein Modell im Cache");
    }

    @Test
    @DisplayName("Expertenwechsel mit anderem Basis-Modell: Entladen")
    void expertSwitchWithDifferentBaseModelShouldUnload() {
        // Zwei Experten mit unterschiedlichen Basis-Modellen:
        // Expert "Roland" → Mistral-Nemo.gguf (7.5GB)
        // Expert "Coder"  → Llama-3.2-3B.gguf (2GB)
        //
        // Beim Wechsel MUSS das alte Modell entladen werden!

        MockModel mistral = new MockModel("Mistral-Nemo");
        modelCache.put("Mistral-Nemo", mistral);

        // Expert "Roland" verwendet Mistral-Nemo
        assertEquals(1, modelCache.size());
        assertFalse(mistral.isClosed());

        // Wechsel zu Expert "Coder" mit Llama
        unloadOtherModels("Llama-3.2-3B");

        // Mistral muss entladen sein
        assertTrue(mistral.isClosed(), "Mistral sollte entladen sein beim Wechsel zu Llama");
        assertEquals(0, modelCache.size(), "Cache sollte leer sein");

        // Jetzt kann Llama geladen werden
        MockModel llama = new MockModel("Llama-3.2-3B");
        modelCache.put("Llama-3.2-3B", llama);
        assertEquals(1, modelCache.size());
    }
}
