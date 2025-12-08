package io.javafleet.fleetnavigator.llm;

import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Zentrale Registry aller verfügbaren GGUF-Modelle von HuggingFace
 *
 * Diese Modelle können direkt über Fleet Navigator heruntergeladen werden.
 *
 * Native Image Safe:
 * - Statische Initialisierung
 * - Keine Reflection
 * - Compile-Time bekannt
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.9
 */
@Component
public class ModelRegistry {

    private static final List<ModelRegistryEntry> MODELS = new ArrayList<>();

    static {
        // ===== DEUTSCHE & MEHRSPRACHIGE CHAT-MODELLE =====

        MODELS.add(ModelRegistryEntry.builder()
            .id("qwen2.5-3b-instruct")
            .displayName("Qwen 2.5 (3B) - Instruct")
            .provider("Alibaba Cloud")
            .architecture("qwen2")
            .version("2.5")
            .parameterSize("3B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("Qwen/Qwen2.5-3B-Instruct-GGUF")
            .filename("qwen2.5-3b-instruct-q4_k_m.gguf")
            .sizeBytes(1_967_004_960L) // 1.97 GB
            .sizeHuman("1.97 GB")
            .description("⭐ EMPFOHLEN: Exzellentes mehrsprachiges Modell mit hervorragendem Deutsch. " +
                        "Sehr gute Balance zwischen Geschwindigkeit und Qualität. Ideal für Briefe, E-Mails und Gespräche.")
            .languages(List.of("Deutsch", "Englisch", "Französisch", "Spanisch", "und 25+ weitere"))
            .useCases(List.of("Briefe schreiben", "E-Mails", "Chat", "Übersetzungen", "Zusammenfassungen"))
            .license("Apache 2.0")
            .rating(4.9f)
            .downloads(120000)
            .minRamGB(4)
            .recommendedRamGB(8)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(true)
            .category("chat")
            .releaseDate("2024-09")
            .trainedUntil("2024-06")
            .contextWindow("32K tokens")
            .primaryTasks("Chat, Briefe schreiben, E-Mails, Übersetzungen")
            .strengths("Exzellentes Deutsch, Mehrsprachig (29 Sprachen), Hohe Qualität bei geringer Größe")
            .limitations("Bei sehr langen Dokumenten (>32K) kann Kontext verloren gehen")
            .build()
        );

        MODELS.add(ModelRegistryEntry.builder()
            .id("llama-3.2-1b-instruct")
            .displayName("Llama 3.2 (1B) - Instruct")
            .provider("Meta AI")
            .architecture("llama")
            .version("3.2")
            .parameterSize("1B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("bartowski/Llama-3.2-1B-Instruct-GGUF")
            .filename("Llama-3.2-1B-Instruct-Q4_K_M.gguf")
            .sizeBytes(711_000_000L) // 711 MB
            .sizeHuman("711 MB")
            .description("⚡ SEHR KLEIN & SCHNELL: Extrem kompaktes Modell von Meta AI. " +
                        "Ideal für ressourcenbeschränkte Systeme und schnelle Antworten. Deutsch funktioniert!")
            .languages(List.of("Deutsch", "Englisch", "und weitere"))
            .useCases(List.of("Schnelle Antworten", "Einfache Aufgaben", "Chat", "Testen"))
            .license("Llama 3.2 Community License")
            .rating(4.3f)
            .downloads(89000)
            .minRamGB(2)
            .recommendedRamGB(4)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(true)
            .category("compact")
            .releaseDate("2024-09")
            .trainedUntil("2024-06")
            .contextWindow("128K tokens")
            .primaryTasks("Quick Answers, Simple Chat, Testing")
            .strengths("Extrem klein, Sehr schnell, Läuft auf schwacher Hardware, 128K Context!")
            .limitations("Begrenzte Fähigkeiten bei komplexen Aufgaben, Qualität niedriger als 3B+")
            .build()
        );

        MODELS.add(ModelRegistryEntry.builder()
            .id("llama-3.2-3b-instruct")
            .displayName("Llama 3.2 (3B) - Instruct")
            .provider("Meta AI")
            .architecture("llama")
            .version("3.2")
            .parameterSize("3B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("bartowski/Llama-3.2-3B-Instruct-GGUF")
            .filename("Llama-3.2-3B-Instruct-Q4_K_M.gguf")
            .sizeBytes(2_018_066_080L) // 2.02 GB
            .sizeHuman("2.02 GB")
            .description("Schnelles Allzweck-Modell von Meta AI. Gutes Deutsch, sehr effizient. " +
                        "Ideal für alltägliche Aufgaben und schnelle Antworten.")
            .languages(List.of("Deutsch", "Englisch", "und weitere"))
            .useCases(List.of("Chat", "Briefe", "Q&A", "Allgemeine Aufgaben"))
            .license("Llama 3.2 Community License")
            .rating(4.7f)
            .downloads(125000)
            .minRamGB(4)
            .recommendedRamGB(8)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(true)
            .category("chat")
            .build()
        );

        // WICHTIG: bartowski Repository hat Single-File Versionen (nicht gesplittet!)
        MODELS.add(ModelRegistryEntry.builder()
            .id("qwen2.5-7b-instruct")
            .displayName("Qwen 2.5 (7B) - Instruct")
            .provider("Alibaba Cloud")
            .architecture("qwen2")
            .version("2.5")
            .parameterSize("7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("bartowski/Qwen2.5-7B-Instruct-GGUF")
            .filename("Qwen2.5-7B-Instruct-Q4_K_M.gguf")
            .sizeBytes(4_680_000_000L) // 4.68 GB
            .sizeHuman("4.68 GB")
            .description("Premium-Modell mit exzellenter Qualität auf Deutsch. " +
                        "Besonders stark bei komplexen Aufgaben, Code und Mathematik. Benötigt mehr RAM.")
            .languages(List.of("Deutsch", "Englisch", "Chinesisch", "und 25+ weitere"))
            .useCases(List.of("Komplexe Texte", "Code", "Analyse", "Mehrsprachig", "Mathematik"))
            .license("Apache 2.0")
            .rating(4.9f)
            .downloads(89000)
            .minRamGB(8)
            .recommendedRamGB(16)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(true)
            .category("chat")
            .build()
        );

        // Q5_K_M Version - bessere Qualität, größere Datei
        // WICHTIG: bartowski Repository hat Single-File Versionen (nicht gesplittet!)
        MODELS.add(ModelRegistryEntry.builder()
            .id("qwen2.5-7b-instruct-q5")
            .displayName("Qwen 2.5 (7B) - Instruct Q5")
            .provider("Alibaba Cloud")
            .architecture("qwen2")
            .version("2.5")
            .parameterSize("7B")
            .quantization("Q5_K_M")
            .huggingFaceRepo("bartowski/Qwen2.5-7B-Instruct-GGUF")
            .filename("Qwen2.5-7B-Instruct-Q5_K_M.gguf")
            .sizeBytes(5_440_000_000L) // 5.44 GB
            .sizeHuman("5.44 GB")
            .description("Premium-Modell mit HÖCHSTER Qualität (Q5). Bessere Präzision als Q4, " +
                        "ideal wenn GPU-Speicher kein Problem ist. Exzellentes Deutsch!")
            .languages(List.of("Deutsch", "Englisch", "Chinesisch", "und 25+ weitere"))
            .useCases(List.of("Komplexe Texte", "Code", "Analyse", "Mehrsprachig", "Mathematik", "Maximale Qualität"))
            .license("Apache 2.0")
            .rating(5.0f)
            .downloads(45000)
            .minRamGB(10)
            .recommendedRamGB(16)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(false)
            .category("chat")
            .releaseDate("2024-09")
            .trainedUntil("2024-06")
            .contextWindow("32K tokens")
            .primaryTasks("Komplexe Texte, Briefe, Code, Analyse, Mathematik")
            .strengths("Höchste Qualität (Q5), Exzellentes Deutsch, 29 Sprachen, Top für RTX 3060 12GB")
            .limitations("Benötigt mehr VRAM als Q4_K_M")
            .build()
        );

        MODELS.add(ModelRegistryEntry.builder()
            .id("phi-3-mini-instruct")
            .displayName("Phi-3 Mini (3.8B) - Instruct")
            .provider("Microsoft")
            .architecture("phi3")
            .version("3")
            .parameterSize("3.8B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("microsoft/Phi-3-mini-4k-instruct-gguf")
            .filename("Phi-3-mini-4k-instruct-q4.gguf")
            .sizeBytes(2_356_934_272L) // 2.36 GB
            .sizeHuman("2.36 GB")
            .description("Kompaktes High-Performance Modell von Microsoft. " +
                        "Gutes Deutsch trotz kleiner Größe. Sehr effizient.")
            .languages(List.of("Deutsch", "Englisch", "und weitere"))
            .useCases(List.of("Chat", "Q&A", "Zusammenfassungen", "Schnelle Antworten"))
            .license("MIT")
            .rating(4.6f)
            .downloads(67000)
            .minRamGB(4)
            .recommendedRamGB(8)
            .gpuAccelSupported(true)
            .featured(true)
            .category("chat")
            .build()
        );

        // ===== CODE-GENERIERUNG =====

        MODELS.add(ModelRegistryEntry.builder()
            .id("qwen2.5-coder-3b-instruct")
            .displayName("Qwen 2.5 Coder (3B) - Instruct")
            .provider("Alibaba Cloud")
            .architecture("qwen2")
            .version("2.5")
            .parameterSize("3B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("Qwen/Qwen2.5-Coder-3B-Instruct-GGUF")
            .filename("qwen2.5-coder-3b-instruct-q4_k_m.gguf")
            .sizeBytes(1_967_004_960L)
            .sizeHuman("1.97 GB")
            .description("Qwen2.5-Coder ist ein Code-fokussiertes LLM von Alibaba Cloud, trainiert auf 5.5T Tokens Code-Daten.")
            .languages(List.of("Python", "Java", "C++", "JavaScript", "Go", "Rust", "SQL"))
            .useCases(List.of("Code Generation", "Code Completion", "Code Explanation", "Debugging"))
            .license("Apache 2.0")
            .rating(4.8f)
            .downloads(54000)
            .minRamGB(4)
            .recommendedRamGB(8)
            .gpuAccelSupported(true)
            .featured(true)
            .category("code")
            .releaseDate("2024-11")
            .trainedUntil("2024-09")
            .contextWindow("128K tokens")
            .primaryTasks("Code Generation, Code Completion, Code Explanation, Debugging")
            .strengths("Exzellente Code-Qualität, Multi-Language Support, 128k Context, Schnelle Inferenz")
            .limitations("Nicht für natürlichsprachige Konversation optimiert")
            .build()
        );

        MODELS.add(ModelRegistryEntry.builder()
            .id("qwen2.5-coder-7b-instruct")
            .displayName("Qwen 2.5 Coder (7B) - Instruct")
            .provider("Alibaba Cloud")
            .architecture("qwen2")
            .version("2.5")
            .parameterSize("7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("Qwen/Qwen2.5-Coder-7B-Instruct-GGUF")
            .filename("qwen2.5-coder-7b-instruct-q4_k_m.gguf")
            .sizeBytes(4_736_032_032L)
            .sizeHuman("4.73 GB")
            .description("Premium Code-Modell mit höchster Qualität. " +
                        "Versteht komplexe deutsche Anweisungen und generiert professionellen Code.")
            .languages(List.of("Python", "Java", "C++", "JavaScript", "Go", "Rust", "SQL"))
            .useCases(List.of("Komplexer Code", "Architektur", "Code Review", "Dokumentation"))
            .license("Apache 2.0")
            .rating(4.9f)
            .downloads(43000)
            .minRamGB(8)
            .recommendedRamGB(16)
            .gpuAccelSupported(true)
            .featured(true)
            .category("code")
            .releaseDate("2024-11")
            .trainedUntil("2024-09")
            .contextWindow("128K tokens")
            .primaryTasks("Complex Code Generation, Architecture Design, Code Review")
            .strengths("Professioneller Code, Architektur-Verständnis, 128k Context")
            .limitations("Ressourcen-intensiv (4.7 GB)")
            .build()
        );

        // DeepSeek Coder - Sehr beliebt!
        MODELS.add(ModelRegistryEntry.builder()
            .id("deepseek-coder-6.7b-instruct")
            .displayName("DeepSeek Coder (6.7B) - Instruct")
            .provider("DeepSeek AI")
            .architecture("deepseek")
            .version("1.0")
            .parameterSize("6.7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("TheBloke/deepseek-coder-6.7B-instruct-GGUF")
            .filename("deepseek-coder-6.7b-instruct.Q4_K_M.gguf")
            .sizeBytes(4_150_000_000L)
            .sizeHuman("4.15 GB")
            .description("⭐ TOP CODE-MODELL: DeepSeek Coder erreicht State-of-the-Art Ergebnisse in Code-Benchmarks. " +
                        "Spezialisiert auf Python, Java, C++, JavaScript. Exzellente Code-Completion.")
            .languages(List.of("Python", "Java", "C++", "JavaScript", "Go", "TypeScript", "C#"))
            .useCases(List.of("Code Generation", "Code Completion", "Bug Fixing", "Code Explanation"))
            .license("DeepSeek License")
            .rating(4.9f)
            .downloads(156000)
            .minRamGB(8)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(true)
            .category("code")
            .releaseDate("2024-01")
            .trainedUntil("2023-11")
            .contextWindow("16K tokens")
            .primaryTasks("Code Generation, Completion, Bug Detection, Refactoring")
            .strengths("State-of-the-Art Code Quality, 87% HumanEval, Multi-Language")
            .limitations("16K Context (kleiner als Qwen)")
            .build()
        );

        MODELS.add(ModelRegistryEntry.builder()
            .id("deepseek-coder-1.3b-instruct")
            .displayName("DeepSeek Coder (1.3B) - Instruct")
            .provider("DeepSeek AI")
            .architecture("deepseek")
            .version("1.0")
            .parameterSize("1.3B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("TheBloke/deepseek-coder-1.3b-instruct-GGUF")
            .filename("deepseek-coder-1.3b-instruct.Q4_K_M.gguf")
            .sizeBytes(830_000_000L)
            .sizeHuman("830 MB")
            .description("Kompaktes Code-Modell von DeepSeek. Trotz kleiner Größe sehr gute Code-Qualität. " +
                        "Perfekt für Code-Completion auf schwächerer Hardware.")
            .languages(List.of("Python", "Java", "JavaScript", "C++", "Go"))
            .useCases(List.of("Code Completion", "Simple Code Gen", "Code Explanation"))
            .license("DeepSeek License")
            .rating(4.6f)
            .downloads(89000)
            .minRamGB(2)
            .recommendedRamGB(4)
            .gpuAccelSupported(true)
            .featured(false)
            .category("code")
            .releaseDate("2024-01")
            .trainedUntil("2023-11")
            .contextWindow("16K tokens")
            .primaryTasks("Code Completion, Simple Generation")
            .strengths("Sehr kompakt, Schnell, Gute Code-Qualität für die Größe")
            .limitations("Für komplexe Architekturen weniger geeignet")
            .build()
        );

        // ===== VISION MODELLE =====

        MODELS.add(ModelRegistryEntry.builder()
            .id("llava-1.6-mistral-7b")
            .displayName("LLaVA 1.6 Mistral (7B)")
            .provider("Haotian Liu (UW Madison)")
            .architecture("llava")
            .version("1.6")
            .parameterSize("7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("cjpais/llava-1.6-mistral-7b-gguf")
            .filename("llava-v1.6-mistral-7b.Q4_K_M.gguf")
            .sizeBytes(4_370_000_000L)
            .sizeHuman("4.37 GB")
            .description("⭐ VISION-MODELL: LLaVA kombiniert Bildverständnis mit Sprachmodell. " +
                        "Kann Bilder analysieren, beschreiben und Fragen dazu beantworten. Deutsch möglich!")
            .languages(List.of("Englisch", "Deutsch (eingeschränkt)", "Bilder"))
            .useCases(List.of("Bildanalyse", "Bildbeschreibung", "OCR", "Visuelles Q&A"))
            .license("Apache 2.0")
            .rating(4.7f)
            .downloads(67000)
            .minRamGB(8)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(false)
            .category("vision")
            .releaseDate("2024-01")
            .trainedUntil("2023-09")
            .contextWindow("4K tokens")
            .primaryTasks("Image Analysis, Image Description, Visual Question Answering, OCR")
            .strengths("Versteht Bilder, OCR, Multimodal, Gute Beschreibungen")
            .limitations("Benötigt MMPROJ-Datei für Bildverarbeitung")
            .isVisionModel(true)
            .mmprojFilename("mmproj-model-f16.gguf")
            .build()
        );

        MODELS.add(ModelRegistryEntry.builder()
            .id("llava-phi3-mini")
            .displayName("LLaVA Phi-3 Mini")
            .provider("Microsoft + Haotian Liu")
            .architecture("llava-phi3")
            .version("1.0")
            .parameterSize("3.8B")
            .quantization("INT4")
            .huggingFaceRepo("xtuner/llava-phi-3-mini-gguf")
            .filename("llava-phi-3-mini-int4.gguf")
            .sizeBytes(2_400_000_000L)
            .sizeHuman("2.4 GB")
            .description("Kompaktes Vision-Modell basierend auf Phi-3. Versteht Bilder und kann sie beschreiben. " +
                        "Kleinere Alternative zu LLaVA Mistral.")
            .languages(List.of("Englisch", "Bilder"))
            .useCases(List.of("Bildanalyse", "Bildbeschreibung", "Visuelles Q&A"))
            .license("MIT")
            .rating(4.5f)
            .downloads(34000)
            .minRamGB(6)
            .recommendedRamGB(8)
            .gpuAccelSupported(true)
            .featured(true)
            .category("vision")
            .releaseDate("2024-04")
            .trainedUntil("2024-01")
            .contextWindow("4K tokens")
            .primaryTasks("Image Understanding, Visual Q&A")
            .strengths("Kompakt, Schnell, Gute Bild-Text-Verknüpfung")
            .limitations("Benötigt MMPROJ-Datei für Bildverarbeitung")
            .isVisionModel(true)
            .mmprojFilename("llava-phi-3-mini-mmproj-f16.gguf")
            .build()
        );

        // ===== KOMPAKTE MODELLE =====

        MODELS.add(ModelRegistryEntry.builder()
            .id("qwen2.5-1.5b-instruct")
            .displayName("Qwen 2.5 (1.5B) - Instruct")
            .provider("Alibaba Cloud")
            .architecture("qwen2")
            .version("2.5")
            .parameterSize("1.5B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("Qwen/Qwen2.5-1.5B-Instruct-GGUF")
            .filename("qwen2.5-1.5b-instruct-q4_k_m.gguf")
            .sizeBytes(1_049_000_000L)
            .sizeHuman("1.05 GB")
            .description("Kompaktes mehrsprachiges Modell. Besseres Deutsch als Llama 1B. " +
                        "Gute Balance für ältere Hardware.")
            .languages(List.of("Deutsch", "Englisch", "und weitere"))
            .useCases(List.of("Chat", "Briefe", "Q&A", "Für schwache PCs"))
            .license("Apache 2.0")
            .rating(4.5f)
            .downloads(78000)
            .minRamGB(3)
            .recommendedRamGB(4)
            .gpuAccelSupported(true)
            .featured(false)
            .category("compact")
            .build()
        );

        // ===== PREMIUM MODELLE (GROSSE MODELLE) =====

        MODELS.add(ModelRegistryEntry.builder()
            .id("mistral-7b-instruct")
            .displayName("Mistral 7B v0.3 - Instruct")
            .provider("Mistral AI")
            .architecture("mistral")
            .version("0.3")
            .parameterSize("7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("MaziyarPanahi/Mistral-7B-Instruct-v0.3-GGUF")
            .filename("Mistral-7B-Instruct-v0.3.Q4_K_M.gguf")
            .sizeBytes(4_368_438_688L)
            .sizeHuman("4.37 GB")
            .description("Balanced Allrounder-Modell. Gutes Deutsch, sehr vielseitig. " +
                        "Gute Wahl für verschiedene Aufgaben.")
            .languages(List.of("Deutsch", "Englisch", "Französisch", "und weitere"))
            .useCases(List.of("Chat", "Analyse", "Zusammenfassungen", "Vielseitig"))
            .license("Apache 2.0")
            .rating(4.7f)
            .downloads(98000)
            .minRamGB(8)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(false)
            .category("chat")
            .build()
        );

        // ===== NEUE PREMIUM-MODELLE FÜR RTX 3060 12GB =====

        // Meta Llama 3.1 8B - Das beliebteste 8B Modell!
        MODELS.add(ModelRegistryEntry.builder()
            .id("llama-3.1-8b-instruct")
            .displayName("Llama 3.1 (8B) - Instruct")
            .provider("Meta AI")
            .architecture("llama")
            .version("3.1")
            .parameterSize("8B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("bartowski/Meta-Llama-3.1-8B-Instruct-GGUF")
            .filename("Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf")
            .sizeBytes(4_920_000_000L) // 4.92 GB
            .sizeHuman("4.92 GB")
            .description("⭐ TOP-EMPFEHLUNG: Metas bestes 8B Modell! Hervorragendes Deutsch, " +
                        "128K Context, State-of-the-Art Qualität. Perfekt für RTX 3060 12GB!")
            .languages(List.of("Deutsch", "Englisch", "Französisch", "Spanisch", "und weitere"))
            .useCases(List.of("Chat", "Analyse", "Briefe", "Code", "Zusammenfassungen"))
            .license("Llama 3.1 Community License")
            .rating(4.9f)
            .downloads(185000)
            .minRamGB(8)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(true)
            .category("chat")
            .releaseDate("2024-07")
            .trainedUntil("2024-04")
            .contextWindow("128K tokens")
            .primaryTasks("Chat, Analyse, Coding, Briefe, Zusammenfassungen")
            .strengths("128K Context, State-of-the-Art 8B, Exzellentes Deutsch, Sehr vielseitig")
            .limitations("Größer als 3B Modelle")
            .build()
        );

        // Llama 3.1 8B Q5 Version - Höhere Qualität
        MODELS.add(ModelRegistryEntry.builder()
            .id("llama-3.1-8b-instruct-q5")
            .displayName("Llama 3.1 (8B) - Instruct Q5")
            .provider("Meta AI")
            .architecture("llama")
            .version("3.1")
            .parameterSize("8B")
            .quantization("Q5_K_M")
            .huggingFaceRepo("bartowski/Meta-Llama-3.1-8B-Instruct-GGUF")
            .filename("Meta-Llama-3.1-8B-Instruct-Q5_K_M.gguf")
            .sizeBytes(5_730_000_000L) // 5.73 GB
            .sizeHuman("5.73 GB")
            .description("Premium Q5-Version von Llama 3.1 8B mit höchster Qualität. " +
                        "Ideal wenn maximale Präzision wichtiger ist als Geschwindigkeit.")
            .languages(List.of("Deutsch", "Englisch", "Französisch", "Spanisch", "und weitere"))
            .useCases(List.of("Komplexe Analysen", "Präzise Texte", "Maximale Qualität"))
            .license("Llama 3.1 Community License")
            .rating(5.0f)
            .downloads(67000)
            .minRamGB(10)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(false)
            .category("chat")
            .releaseDate("2024-07")
            .trainedUntil("2024-04")
            .contextWindow("128K tokens")
            .primaryTasks("Präzise Texte, Analysen, Maximale Qualität")
            .strengths("Höchste Q5 Qualität, 128K Context, Perfekt für RTX 3060 12GB")
            .limitations("Etwas langsamer als Q4")
            .build()
        );

        // Google Gemma 2 9B - Exzellentes Modell von Google
        MODELS.add(ModelRegistryEntry.builder()
            .id("gemma-2-9b-it")
            .displayName("Gemma 2 (9B) - Instruct")
            .provider("Google")
            .architecture("gemma2")
            .version("2")
            .parameterSize("9B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("bartowski/gemma-2-9b-it-GGUF")
            .filename("gemma-2-9b-it-Q4_K_M.gguf")
            .sizeBytes(5_760_000_000L) // 5.76 GB
            .sizeHuman("5.76 GB")
            .description("⭐ GOOGLE PREMIUM: Googles neuestes 9B Modell mit exzellenter Qualität. " +
                        "Sehr stark bei Reasoning und Analyse. Gutes Deutsch!")
            .languages(List.of("Deutsch", "Englisch", "und viele weitere"))
            .useCases(List.of("Reasoning", "Analyse", "Chat", "Komplexe Aufgaben"))
            .license("Gemma License")
            .rating(4.8f)
            .downloads(120000)
            .minRamGB(10)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(true)
            .category("chat")
            .releaseDate("2024-06")
            .trainedUntil("2024-03")
            .contextWindow("8K tokens")
            .primaryTasks("Reasoning, Analyse, Komplexe Aufgaben, Chat")
            .strengths("Google-Qualität, Exzellentes Reasoning, Sehr präzise")
            .limitations("8K Context (kleiner als Llama 3.1)")
            .build()
        );

        // Gemma 2 9B Q5 Version
        MODELS.add(ModelRegistryEntry.builder()
            .id("gemma-2-9b-it-q5")
            .displayName("Gemma 2 (9B) - Instruct Q5")
            .provider("Google")
            .architecture("gemma2")
            .version("2")
            .parameterSize("9B")
            .quantization("Q5_K_M")
            .huggingFaceRepo("bartowski/gemma-2-9b-it-GGUF")
            .filename("gemma-2-9b-it-Q5_K_M.gguf")
            .sizeBytes(6_650_000_000L) // 6.65 GB
            .sizeHuman("6.65 GB")
            .description("Premium Q5-Version von Gemma 2 9B. Höchste Qualität für anspruchsvolle Aufgaben. " +
                        "Noch präziser als Q4, optimal für RTX 3060 12GB.")
            .languages(List.of("Deutsch", "Englisch", "und viele weitere"))
            .useCases(List.of("Maximale Präzision", "Wissenschaftliche Texte", "Analyse"))
            .license("Gemma License")
            .rating(4.9f)
            .downloads(45000)
            .minRamGB(11)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(false)
            .category("chat")
            .releaseDate("2024-06")
            .trainedUntil("2024-03")
            .contextWindow("8K tokens")
            .primaryTasks("Maximale Präzision, Wissenschaft, Analyse")
            .strengths("Höchste Qualität, Google-Niveau, Exzellentes Reasoning")
            .limitations("Benötigt fast vollen GPU-Speicher auf 12GB")
            .build()
        );

        // ===== DEUTSCHES SPEZIAL-MODELL =====

        // EM German Leo Mistral - Speziell für Deutsch trainiert!
        MODELS.add(ModelRegistryEntry.builder()
            .id("em-german-leo-mistral")
            .displayName("EM German Leo Mistral (7B)")
            .provider("jphme / LAION")
            .architecture("mistral")
            .version("1.0")
            .parameterSize("7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("TheBloke/em_german_leo_mistral-GGUF")
            .filename("em_german_leo_mistral.Q4_K_M.gguf")
            .sizeBytes(4_370_000_000L) // 4.37 GB
            .sizeHuman("4.37 GB")
            .description("🇩🇪 DEUTSCH-SPEZIALIST: Speziell für die deutsche Sprache trainiert! " +
                        "Basiert auf LeoLM und Mistral. Ideal für deutsche Briefe, E-Mails und Texte.")
            .languages(List.of("Deutsch (primär)", "Englisch"))
            .useCases(List.of("Deutsche Briefe", "E-Mails", "Formelle Texte", "Übersetzungen DE↔EN"))
            .license("Apache 2.0")
            .rating(4.7f)
            .downloads(67000)
            .minRamGB(8)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(false)
            .category("chat")
            .releaseDate("2023-11")
            .trainedUntil("2023-09")
            .contextWindow("32K tokens")
            .primaryTasks("Deutsche Texte, Briefe, E-Mails, Formelle Kommunikation")
            .strengths("Exzellentes Deutsch (LAION trainiert), Natürliche Sprache, Formelle Texte")
            .limitations("Primär Deutsch - weniger gut bei anderen Sprachen")
            .build()
        );
    }

    // ===== PUBLIC API =====

    /**
     * Alle verfügbaren Modelle
     */
    public List<ModelRegistryEntry> getAllModels() {
        return new ArrayList<>(MODELS);
    }

    /**
     * Featured Modelle (für Startseite)
     */
    public List<ModelRegistryEntry> getFeaturedModels() {
        return MODELS.stream()
            .filter(ModelRegistryEntry::isFeatured)
            .sorted(Comparator.comparing(ModelRegistryEntry::getRating).reversed())
            .toList();
    }

    /**
     * Trending Modelle
     */
    public List<ModelRegistryEntry> getTrendingModels() {
        return MODELS.stream()
            .filter(ModelRegistryEntry::isTrending)
            .sorted(Comparator.comparing(ModelRegistryEntry::getDownloads).reversed())
            .limit(5)
            .toList();
    }

    /**
     * Nach Kategorie filtern
     */
    public List<ModelRegistryEntry> getByCategory(String category) {
        return MODELS.stream()
            .filter(m -> m.getCategory().equalsIgnoreCase(category))
            .toList();
    }

    /**
     * Nach RAM-Requirements filtern
     */
    public List<ModelRegistryEntry> getByMaxRam(int maxRamGB) {
        return MODELS.stream()
            .filter(m -> m.getMinRamGB() <= maxRamGB)
            .sorted(Comparator.comparing(ModelRegistryEntry::getRating).reversed())
            .toList();
    }

    /**
     * Modell nach ID finden
     */
    public Optional<ModelRegistryEntry> findById(String id) {
        return MODELS.stream()
            .filter(m -> m.getId().equals(id))
            .findFirst();
    }

    /**
     * Suche
     */
    public List<ModelInfo> search(String query) {
        String lowerQuery = query.toLowerCase();
        return MODELS.stream()
            .filter(m ->
                m.getDisplayName().toLowerCase().contains(lowerQuery) ||
                m.getDescription().toLowerCase().contains(lowerQuery) ||
                m.getLanguages().stream().anyMatch(lang -> lang.toLowerCase().contains(lowerQuery)) ||
                m.getUseCases().stream().anyMatch(use -> use.toLowerCase().contains(lowerQuery))
            )
            .map(this::toModelInfo)
            .toList();
    }

    /**
     * Konvertiert Registry Entry zu ModelInfo
     */
    private ModelInfo toModelInfo(ModelRegistryEntry entry) {
        return ModelInfo.builder()
            .name(entry.getFilename())
            .displayName(entry.getDisplayName())
            .provider("llamacpp")
            .size(entry.getSizeBytes())
            .sizeHuman(entry.getSizeHuman())
            .architecture(entry.getArchitecture())
            .quantization(entry.getQuantization())
            .description(entry.getDescription())
            .installed(false) // Wird später geprüft
            .custom(false)
            .build();
    }
}