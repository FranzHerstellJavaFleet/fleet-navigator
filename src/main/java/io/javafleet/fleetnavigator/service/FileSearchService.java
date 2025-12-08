package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for local file search functionality (RAG source)
 * Uses Apache Lucene for fast full-text indexing and search
 * Uses Linux 'locate' command for fast filename search (if available)
 */
@Service
@Slf4j
public class FileSearchService {

    @Value("${fleet.filesearch.index-path:./data/file-index}")
    private String indexPath;

    @Value("${fleet.filesearch.use-locate:true}")
    private boolean useLocate;

    // Inject SettingsService lazily to avoid circular dependency
    @Autowired
    @Lazy
    private SettingsService settingsService;

    // JSON mapper for config persistence
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // Lucene components
    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;

    // Search folder configurations (persisted in database)
    private final Map<String, FileSearchConfig> searchConfigs = new ConcurrentHashMap<>();

    // Indexing state
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final AtomicInteger indexedFileCount = new AtomicInteger(0);
    private LocalDateTime lastIndexUpdate;

    // Executor for background indexing
    private final ExecutorService indexExecutor = Executors.newSingleThreadExecutor();

    // Supported file extensions
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".pdf", ".docx", ".doc", ".odt", ".txt", ".md", ".rtf"
    );

    @PostConstruct
    public void init() {
        try {
            // Create index directory
            Path indexDir = Paths.get(indexPath);
            Files.createDirectories(indexDir);

            // Initialize Lucene
            directory = FSDirectory.open(indexDir);
            analyzer = new StandardAnalyzer();

            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            indexWriter = new IndexWriter(directory, config);

            log.info("FileSearchService initialized with index at: {}", indexPath);

        } catch (Exception e) {
            log.error("Failed to initialize FileSearchService: {}", e.getMessage());
            // Fallback to in-memory index
            try {
                directory = new ByteBuffersDirectory();
                analyzer = new StandardAnalyzer();
                IndexWriterConfig config = new IndexWriterConfig(analyzer);
                indexWriter = new IndexWriter(directory, config);
                log.info("Using in-memory index as fallback");
            } catch (Exception ex) {
                log.error("Failed to create fallback index: {}", ex.getMessage());
            }
        }

        // Check if locate is available
        if (useLocate) {
            useLocate = isLocateAvailable();
            log.info("Linux 'locate' command available: {}", useLocate);
        }

        // Load saved folder configurations from database
        loadFolderConfigsFromDatabase();
    }

    /**
     * Load folder configurations from database
     */
    private void loadFolderConfigsFromDatabase() {
        try {
            if (settingsService != null) {
                String foldersJson = settingsService.getFileSearchFolders();
                if (foldersJson != null && !foldersJson.equals("[]")) {
                    List<FileSearchConfig> configs = objectMapper.readValue(
                            foldersJson, new TypeReference<List<FileSearchConfig>>() {});

                    for (FileSearchConfig config : configs) {
                        // Validate folder still exists
                        if (Files.exists(Paths.get(config.getFolderPath()))) {
                            searchConfigs.put(config.getFolderId(), config);
                            log.info("Loaded search folder from DB: {} -> {}", config.getName(), config.getFolderPath());
                        } else {
                            log.warn("Skipping non-existent folder: {}", config.getFolderPath());
                        }
                    }

                    // Re-index folders that need it
                    for (FileSearchConfig config : searchConfigs.values()) {
                        if (!config.isIndexed() || config.getLastIndexed() == null) {
                            indexFolderAsync(config.getFolderId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error loading folder configs from database: {}", e.getMessage());
        }
    }

    /**
     * Save folder configurations to database
     */
    private void saveFolderConfigsToDatabase() {
        try {
            if (settingsService != null) {
                String foldersJson = objectMapper.writeValueAsString(new ArrayList<>(searchConfigs.values()));
                settingsService.saveFileSearchFolders(foldersJson);
                log.debug("Saved {} folder configs to database", searchConfigs.size());
            }
        } catch (Exception e) {
            log.error("Error saving folder configs to database: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (indexWriter != null) {
                indexWriter.close();
            }
            if (directory != null) {
                directory.close();
            }
            indexExecutor.shutdownNow();
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage());
        }
    }

    // ==================== Configuration ====================

    /**
     * Configuration for a search folder
     */
    public static class FileSearchConfig {
        private String folderId;
        private String folderPath;
        private String name;
        private boolean searchContent;
        private boolean recursive;
        private int maxDepth;
        private Set<String> allowedExtensions;
        private boolean enabled;
        private LocalDateTime lastIndexed;
        private int fileCount;
        private boolean indexed;

        public FileSearchConfig() {
            this.searchContent = true;
            this.recursive = true;
            this.maxDepth = 10;
            this.allowedExtensions = new HashSet<>(SUPPORTED_EXTENSIONS);
            this.enabled = true;
            this.indexed = false;
        }

        // Getters and Setters
        public String getFolderId() { return folderId; }
        public void setFolderId(String folderId) { this.folderId = folderId; }
        public String getFolderPath() { return folderPath; }
        public void setFolderPath(String folderPath) { this.folderPath = folderPath; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isSearchContent() { return searchContent; }
        public void setSearchContent(boolean searchContent) { this.searchContent = searchContent; }
        public boolean isRecursive() { return recursive; }
        public void setRecursive(boolean recursive) { this.recursive = recursive; }
        public int getMaxDepth() { return maxDepth; }
        public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
        public Set<String> getAllowedExtensions() { return allowedExtensions; }
        public void setAllowedExtensions(Set<String> allowedExtensions) { this.allowedExtensions = allowedExtensions; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public LocalDateTime getLastIndexed() { return lastIndexed; }
        public void setLastIndexed(LocalDateTime lastIndexed) { this.lastIndexed = lastIndexed; }
        public int getFileCount() { return fileCount; }
        public void setFileCount(int fileCount) { this.fileCount = fileCount; }
        public boolean isIndexed() { return indexed; }
        public void setIndexed(boolean indexed) { this.indexed = indexed; }
    }

    /**
     * Search result
     */
    public static class FileSearchResult {
        private String filePath;
        private String fileName;
        private String fileType;
        private long fileSize;
        private LocalDateTime lastModified;
        private String matchType;
        private String snippet;
        private double relevanceScore;

        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
        public String getMatchType() { return matchType; }
        public void setMatchType(String matchType) { this.matchType = matchType; }
        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }
        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
    }

    /**
     * Search options
     */
    public static class SearchOptions {
        private boolean searchByName = true;
        private boolean searchByContent = true;
        private int maxResults = 20;
        private Set<String> fileTypes = null;

        public boolean isSearchByName() { return searchByName; }
        public void setSearchByName(boolean searchByName) { this.searchByName = searchByName; }
        public boolean isSearchByContent() { return searchByContent; }
        public void setSearchByContent(boolean searchByContent) { this.searchByContent = searchByContent; }
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        public Set<String> getFileTypes() { return fileTypes; }
        public void setFileTypes(Set<String> fileTypes) { this.fileTypes = fileTypes; }
    }

    // ==================== Folder Management ====================

    public void addSearchFolder(FileSearchConfig config) {
        if (config.getFolderId() == null) {
            config.setFolderId(UUID.randomUUID().toString());
        }

        Path folderPath = Paths.get(config.getFolderPath());
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new IllegalArgumentException("Ordner existiert nicht: " + config.getFolderPath());
        }

        searchConfigs.put(config.getFolderId(), config);
        log.info("Search folder added: {} -> {}", config.getName(), config.getFolderPath());

        // Persist to database
        saveFolderConfigsToDatabase();

        // Start async indexing
        indexFolderAsync(config.getFolderId());
    }

    public void removeSearchFolder(String folderId) {
        FileSearchConfig config = searchConfigs.remove(folderId);
        if (config != null) {
            // Remove documents from index
            try {
                indexWriter.deleteDocuments(new Term("folderId", folderId));
                indexWriter.commit();
                log.info("Search folder removed and unindexed: {}", folderId);
            } catch (Exception e) {
                log.error("Error removing folder from index: {}", e.getMessage());
            }

            // Persist to database
            saveFolderConfigsToDatabase();
        }
    }

    /**
     * Update a folder configuration and persist
     */
    public void updateSearchFolder(FileSearchConfig config) {
        if (config.getFolderId() != null && searchConfigs.containsKey(config.getFolderId())) {
            searchConfigs.put(config.getFolderId(), config);
            saveFolderConfigsToDatabase();
        }
    }

    public List<FileSearchConfig> getSearchFolders() {
        return new ArrayList<>(searchConfigs.values());
    }

    public Optional<FileSearchConfig> getSearchFolder(String folderId) {
        return Optional.ofNullable(searchConfigs.get(folderId));
    }

    // ==================== Indexing ====================

    /**
     * Index a folder asynchronously
     */
    @Async
    public void indexFolderAsync(String folderId) {
        indexExecutor.submit(() -> indexFolder(folderId));
    }

    /**
     * Index all configured folders
     */
    public void indexAllFolders() {
        for (String folderId : searchConfigs.keySet()) {
            indexFolder(folderId);
        }
    }

    /**
     * Index a specific folder
     */
    public void indexFolder(String folderId) {
        FileSearchConfig config = searchConfigs.get(folderId);
        if (config == null || !config.isEnabled()) return;

        if (!indexingInProgress.compareAndSet(false, true)) {
            log.info("Indexing already in progress, skipping");
            return;
        }

        try {
            log.info("Starting indexing for folder: {} ({})", config.getName(), config.getFolderPath());
            Path basePath = Paths.get(config.getFolderPath());

            if (!Files.exists(basePath)) {
                log.error("Folder does not exist: {}", config.getFolderPath());
                return;
            }

            // Delete existing documents from this folder
            indexWriter.deleteDocuments(new Term("folderId", folderId));

            AtomicInteger count = new AtomicInteger(0);
            int maxDepth = config.isRecursive() ? config.getMaxDepth() : 1;

            Files.walkFileTree(basePath, EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String extension = getExtension(file.getFileName().toString()).toLowerCase();

                    if (!config.getAllowedExtensions().isEmpty() &&
                        !config.getAllowedExtensions().contains(extension)) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (SUPPORTED_EXTENSIONS.contains(extension)) {
                        try {
                            indexFile(file, folderId, attrs);
                            count.incrementAndGet();

                            if (count.get() % 100 == 0) {
                                log.info("Indexed {} files...", count.get());
                            }
                        } catch (Exception e) {
                            log.debug("Error indexing {}: {}", file, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            indexWriter.commit();

            config.setLastIndexed(LocalDateTime.now());
            config.setFileCount(count.get());
            config.setIndexed(true);
            indexedFileCount.set(count.get());
            lastIndexUpdate = LocalDateTime.now();

            // Persist updated config
            saveFolderConfigsToDatabase();

            log.info("Indexing complete for {}: {} files indexed", config.getName(), count.get());

        } catch (Exception e) {
            log.error("Error indexing folder {}: {}", config.getName(), e.getMessage());
        } finally {
            indexingInProgress.set(false);
        }
    }

    /**
     * Index a single file
     */
    private void indexFile(Path file, String folderId, BasicFileAttributes attrs) throws IOException {
        String content = extractContent(file.toFile());
        if (content == null || content.isEmpty()) return;

        Document doc = new Document();
        doc.add(new StringField("path", file.toAbsolutePath().toString(), Field.Store.YES));
        doc.add(new StringField("folderId", folderId, Field.Store.YES));
        doc.add(new TextField("fileName", file.getFileName().toString(), Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.NO));
        doc.add(new StoredField("fileSize", attrs.size()));
        doc.add(new StoredField("lastModified", attrs.lastModifiedTime().toMillis()));
        doc.add(new StringField("extension", getExtension(file.getFileName().toString()), Field.Store.YES));

        indexWriter.addDocument(doc);
    }

    // ==================== Search ====================

    /**
     * Main search method
     */
    public List<FileSearchResult> search(String query, SearchOptions options) {
        List<FileSearchResult> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String normalizedQuery = query.trim();

        // 1. Search by filename using locate (fast)
        if (options.isSearchByName()) {
            results.addAll(searchByFilename(normalizedQuery, options.getMaxResults()));
        }

        // 2. Search by content using Lucene (fast with index)
        if (options.isSearchByContent()) {
            results.addAll(searchByContent(normalizedQuery, options.getMaxResults()));
        }

        // Deduplicate by file path
        Map<String, FileSearchResult> uniqueResults = new LinkedHashMap<>();
        for (FileSearchResult result : results) {
            if (!uniqueResults.containsKey(result.getFilePath()) ||
                result.getRelevanceScore() > uniqueResults.get(result.getFilePath()).getRelevanceScore()) {
                uniqueResults.put(result.getFilePath(), result);
            }
        }

        // Sort by relevance and limit
        List<FileSearchResult> sortedResults = new ArrayList<>(uniqueResults.values());
        sortedResults.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

        if (options.getMaxResults() > 0 && sortedResults.size() > options.getMaxResults()) {
            return sortedResults.subList(0, options.getMaxResults());
        }

        return sortedResults;
    }

    /**
     * Search by filename using Linux 'locate' command (very fast)
     */
    private List<FileSearchResult> searchByFilename(String query, int maxResults) {
        List<FileSearchResult> results = new ArrayList<>();

        if (useLocate) {
            try {
                // Build locate command with patterns for all search folders
                List<String> patterns = new ArrayList<>();
                for (FileSearchConfig config : searchConfigs.values()) {
                    if (config.isEnabled()) {
                        patterns.add(config.getFolderPath() + "/*" + query + "*");
                    }
                }

                if (patterns.isEmpty()) return results;

                // Run locate for each pattern
                for (String pattern : patterns) {
                    ProcessBuilder pb = new ProcessBuilder("locate", "-i", "-l", String.valueOf(maxResults), pattern);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null && results.size() < maxResults) {
                            File file = new File(line);
                            if (file.exists() && file.isFile()) {
                                FileSearchResult result = createResultFromFile(file);
                                result.setMatchType("name");
                                result.setRelevanceScore(0.8);
                                results.add(result);
                            }
                        }
                    }

                    process.waitFor(5, TimeUnit.SECONDS);
                }

            } catch (Exception e) {
                log.debug("Locate search failed, falling back to manual search: {}", e.getMessage());
                results.addAll(searchByFilenameManual(query, maxResults));
            }
        } else {
            results.addAll(searchByFilenameManual(query, maxResults));
        }

        return results;
    }

    /**
     * Fallback: manual filename search
     */
    private List<FileSearchResult> searchByFilenameManual(String query, int maxResults) {
        List<FileSearchResult> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (FileSearchConfig config : searchConfigs.values()) {
            if (!config.isEnabled()) continue;

            try {
                Path basePath = Paths.get(config.getFolderPath());
                int maxDepth = config.isRecursive() ? config.getMaxDepth() : 1;

                Files.walkFileTree(basePath, EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (results.size() >= maxResults) {
                            return FileVisitResult.TERMINATE;
                        }

                        String fileName = file.getFileName().toString().toLowerCase();
                        if (fileName.contains(lowerQuery)) {
                            FileSearchResult result = createResultFromFile(file.toFile());
                            result.setMatchType("name");
                            result.setRelevanceScore(fileName.equals(lowerQuery) ? 1.0 :
                                    fileName.startsWith(lowerQuery) ? 0.9 : 0.7);
                            results.add(result);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });

            } catch (Exception e) {
                log.debug("Error in manual filename search: {}", e.getMessage());
            }
        }

        return results;
    }

    /**
     * Search by content using Lucene index (fast)
     */
    private List<FileSearchResult> searchByContent(String query, int maxResults) {
        List<FileSearchResult> results = new ArrayList<>();

        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", analyzer);
            parser.setAllowLeadingWildcard(true);

            // Parse query (allow wildcards)
            Query luceneQuery = parser.parse(query + "~"); // Fuzzy search

            TopDocs topDocs = searcher.search(luceneQuery, maxResults);

            // Highlighter for snippets
            QueryScorer scorer = new QueryScorer(luceneQuery);
            Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("**", "**"), scorer);
            highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, 150));

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String filePath = doc.get("path");
                File file = new File(filePath);

                if (!file.exists()) continue;

                // Check if file is in an enabled folder
                boolean inEnabledFolder = false;
                for (FileSearchConfig config : searchConfigs.values()) {
                    if (config.isEnabled() && filePath.startsWith(config.getFolderPath())) {
                        inEnabledFolder = true;
                        break;
                    }
                }
                if (!inEnabledFolder) continue;

                FileSearchResult result = new FileSearchResult();
                result.setFilePath(filePath);
                result.setFileName(doc.get("fileName"));
                result.setFileType(doc.get("extension"));
                result.setFileSize(file.length());
                result.setLastModified(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(file.lastModified()),
                        ZoneId.systemDefault()));
                result.setMatchType("content");
                result.setRelevanceScore(scoreDoc.score / 10.0); // Normalize score

                // Extract snippet
                try {
                    String content = extractContent(file);
                    if (content != null) {
                        String snippet = highlighter.getBestFragment(analyzer, "content", content);
                        result.setSnippet(snippet != null ? snippet : content.substring(0, Math.min(200, content.length())));
                    }
                } catch (Exception e) {
                    log.debug("Error extracting snippet: {}", e.getMessage());
                }

                results.add(result);
            }

        } catch (Exception e) {
            log.error("Lucene search error: {}", e.getMessage());
        }

        return results;
    }

    // ==================== Content Extraction ====================

    /**
     * Extract text content from file
     */
    public String extractContent(File file) {
        String extension = getExtension(file.getName()).toLowerCase();

        try {
            return switch (extension) {
                case ".pdf" -> extractPdfContent(file);
                case ".docx" -> extractDocxContent(file);
                case ".odt" -> extractOdtContent(file);
                case ".txt", ".md" -> Files.readString(file.toPath());
                default -> null;
            };
        } catch (Exception e) {
            log.debug("Error extracting content from {}: {}", file.getName(), e.getMessage());
            return null;
        }
    }

    private String extractPdfContent(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocxContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractOdtContent(File file) {
        try {
            OdfTextDocument odt = OdfTextDocument.loadDocument(file);
            String content = odt.getContentRoot().getTextContent();
            odt.close();
            return content;
        } catch (Exception e) {
            log.debug("Error reading ODT: {}", e.getMessage());
            return null;
        }
    }

    // ==================== Utilities ====================

    private FileSearchResult createResultFromFile(File file) {
        FileSearchResult result = new FileSearchResult();
        result.setFilePath(file.getAbsolutePath());
        result.setFileName(file.getName());
        result.setFileType(getExtension(file.getName()));
        result.setFileSize(file.length());
        result.setLastModified(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(file.lastModified()),
                ZoneId.systemDefault()));
        return result;
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    private boolean isLocateAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "locate");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== RAG Integration ====================

    /**
     * Format results for LLM context
     */
    public String formatResultsAsContext(List<FileSearchResult> results) {
        if (results == null || results.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("=== LOKALE DOKUMENTE (Dateisuche) ===\n\n");

        for (int i = 0; i < results.size(); i++) {
            FileSearchResult result = results.get(i);
            sb.append(String.format("**Dokument %d:** %s\n", i + 1, result.getFileName()));
            sb.append(String.format("- Pfad: %s\n", result.getFilePath()));
            sb.append(String.format("- Match: %s\n", result.getMatchType()));
            if (result.getSnippet() != null) {
                sb.append("- Vorschau: ").append(result.getSnippet()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Get full content of files for RAG
     */
    public String getFilesContentForRAG(List<FileSearchResult> results, int maxTotalLength) {
        if (results == null || results.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int currentLength = 0;

        for (FileSearchResult result : results) {
            if (currentLength >= maxTotalLength) break;

            try {
                String content = extractContent(new File(result.getFilePath()));
                if (content != null && !content.isEmpty()) {
                    int remaining = maxTotalLength - currentLength;
                    String truncated = content.length() > remaining ?
                            content.substring(0, remaining) + "...[abgeschnitten]" : content;

                    sb.append("\n=== ").append(result.getFileName()).append(" ===\n");
                    sb.append(truncated).append("\n");
                    currentLength += truncated.length();
                }
            } catch (Exception e) {
                log.debug("Error reading file: {}", e.getMessage());
            }
        }

        return sb.toString();
    }

    // ==================== Status ====================

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("indexingInProgress", indexingInProgress.get());
        status.put("indexedFileCount", indexedFileCount.get());
        status.put("lastIndexUpdate", lastIndexUpdate);
        status.put("searchFoldersCount", searchConfigs.size());
        status.put("locateAvailable", useLocate);
        status.put("searchFolders", getSearchFolders());
        return status;
    }

    public boolean isIndexingInProgress() {
        return indexingInProgress.get();
    }
}
