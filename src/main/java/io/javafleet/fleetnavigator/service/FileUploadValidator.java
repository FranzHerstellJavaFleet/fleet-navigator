package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service zur Validierung von Datei-Uploads.
 * Prüft Dateigröße, Dateityp, Dateinamen und potentiell gefährliche Inhalte.
 */
@Service
@Slf4j
public class FileUploadValidator {

    // Maximale Dateigröße: 50 MB
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024;
    private static final long MAX_FILE_SIZE_MB = 50;

    // Erlaubte Dateiendungen (Whitelist)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        // Dokumente
        "pdf", "doc", "docx", "odt", "rtf", "txt", "md",
        // Tabellen
        "xls", "xlsx", "ods", "csv",
        // Bilder
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
        // Code/Text
        "java", "py", "js", "ts", "html", "css", "json", "xml", "yaml", "yml",
        "sh", "bat", "sql", "go", "rs", "c", "cpp", "h", "hpp",
        // Archive (für spätere Verarbeitung)
        "zip"
    );

    // Erlaubte MIME-Types
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        // Dokumente
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.oasis.opendocument.text",
        "application/rtf",
        "text/plain",
        "text/markdown",
        // Tabellen
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.oasis.opendocument.spreadsheet",
        "text/csv",
        // Bilder
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "image/bmp",
        "image/svg+xml",
        // Code/Text
        "text/html",
        "text/css",
        "text/javascript",
        "application/javascript",
        "application/json",
        "application/xml",
        "text/xml",
        "application/x-yaml",
        "text/yaml",
        "application/x-sh",
        "text/x-java-source",
        "text/x-python",
        // Archive
        "application/zip",
        "application/x-zip-compressed",
        // Fallback für unbekannte Textdateien
        "application/octet-stream"
    );

    // Pattern für ungültige Dateinamen-Zeichen
    private static final Pattern INVALID_FILENAME_PATTERN = Pattern.compile(
        "[<>:\"/\\\\|?*\\x00-\\x1F]|\\.\\./"
    );

    // Pattern für gefährliche Dateiendungen (doppelte Extensions)
    private static final Pattern DANGEROUS_EXTENSION_PATTERN = Pattern.compile(
        ".*\\.(exe|bat|cmd|sh|ps1|vbs|js|jar|msi|dll|scr|com)\\.[a-z]+$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Validiert eine hochgeladene Datei vollständig.
     *
     * @param file Die hochgeladene Datei
     * @throws FileUploadException wenn die Validierung fehlschlägt
     */
    public void validate(MultipartFile file) {
        validateNotEmpty(file);
        validateFileSize(file);
        validateFileName(file);
        validateFileExtension(file);
        validateMimeType(file);
        validateNoSuspiciousContent(file);

        log.debug("Datei-Validierung erfolgreich: {} ({} bytes, {})",
            file.getOriginalFilename(), file.getSize(), file.getContentType());
    }

    /**
     * Prüft, ob die Datei nicht leer ist.
     */
    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw FileUploadException.emptyFile();
        }
    }

    /**
     * Prüft die Dateigröße.
     */
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            log.warn("Datei zu groß: {} bytes (max: {} bytes)",
                file.getSize(), MAX_FILE_SIZE_BYTES);
            throw FileUploadException.fileTooLarge(MAX_FILE_SIZE_MB);
        }
    }

    /**
     * Prüft den Dateinamen auf ungültige Zeichen und gefährliche Patterns.
     */
    private void validateFileName(MultipartFile file) {
        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw FileUploadException.invalidFileName();
        }

        // Prüfe auf ungültige Zeichen
        if (INVALID_FILENAME_PATTERN.matcher(filename).find()) {
            log.warn("Ungültiger Dateiname (enthält verbotene Zeichen): {}", filename);
            throw FileUploadException.invalidFileName();
        }

        // Prüfe auf Path-Traversal-Versuche
        if (filename.contains("..") || filename.startsWith("/") || filename.startsWith("\\")) {
            log.warn("Verdächtiger Dateiname (Path Traversal): {}", filename);
            throw FileUploadException.suspiciousFile("Verdächtiger Dateipfad");
        }

        // Prüfe auf gefährliche doppelte Extensions (z.B. "harmlos.pdf.exe")
        if (DANGEROUS_EXTENSION_PATTERN.matcher(filename).matches()) {
            log.warn("Gefährliche doppelte Dateiendung: {}", filename);
            throw FileUploadException.suspiciousFile("Verdächtige Dateiendung");
        }
    }

    /**
     * Prüft die Dateiendung gegen die Whitelist.
     */
    private void validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw FileUploadException.invalidFileName();
        }

        String extension = getFileExtension(filename).toLowerCase();

        if (extension.isEmpty()) {
            log.warn("Datei ohne Endung: {}", filename);
            throw FileUploadException.invalidFileType(String.join(", ", ALLOWED_EXTENSIONS));
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("Nicht erlaubte Dateiendung: {} (Datei: {})", extension, filename);
            throw FileUploadException.invalidFileType(String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /**
     * Prüft den MIME-Type der Datei.
     */
    private void validateMimeType(MultipartFile file) {
        String contentType = file.getContentType();

        // Einige Browser senden keinen Content-Type - dann überspringen
        if (contentType == null || contentType.isBlank()) {
            log.debug("Kein Content-Type gesendet, überspringe MIME-Validierung");
            return;
        }

        // Normalisiere den MIME-Type (entferne charset etc.)
        String normalizedType = contentType.split(";")[0].trim().toLowerCase();

        if (!ALLOWED_MIME_TYPES.contains(normalizedType)) {
            // Erlaube application/octet-stream als Fallback für Textdateien
            String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
            if (normalizedType.equals("application/octet-stream") && isTextExtension(extension)) {
                log.debug("Erlaube application/octet-stream für Textdatei: {}", extension);
                return;
            }

            log.warn("Nicht erlaubter MIME-Type: {} (Datei: {})", contentType, file.getOriginalFilename());
            throw FileUploadException.invalidFileType("Unterstützte Dokumentformate");
        }
    }

    /**
     * Prüft auf verdächtige Inhalte in der Datei.
     */
    private void validateNoSuspiciousContent(MultipartFile file) {
        // Bei kleinen Dateien: Prüfe auf verdächtige Patterns
        if (file.getSize() < 1024 * 10) { // < 10 KB
            try {
                byte[] bytes = file.getBytes();
                String content = new String(bytes, 0, Math.min(bytes.length, 1000));

                // Prüfe auf eingebettete Skripte in vermeintlichen Dokumenten
                String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
                if (isDocumentExtension(extension)) {
                    if (content.contains("<script") || content.contains("javascript:")) {
                        log.warn("Verdächtiger Inhalt in Dokument gefunden: {}", file.getOriginalFilename());
                        throw FileUploadException.suspiciousFile("Enthält potentiell schädlichen Code");
                    }
                }
            } catch (FileUploadException e) {
                throw e;
            } catch (Exception e) {
                // Fehler beim Lesen ignorieren, nicht sicherheitskritisch
                log.debug("Konnte Dateiinhalt nicht prüfen: {}", e.getMessage());
            }
        }
    }

    /**
     * Extrahiert die Dateiendung.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Prüft, ob die Extension eine Textdatei ist.
     */
    private boolean isTextExtension(String extension) {
        return Set.of("java", "py", "js", "ts", "html", "css", "json", "xml",
            "yaml", "yml", "sh", "bat", "sql", "go", "rs", "c", "cpp", "h",
            "hpp", "txt", "md", "csv").contains(extension);
    }

    /**
     * Prüft, ob die Extension ein Dokument ist.
     */
    private boolean isDocumentExtension(String extension) {
        return Set.of("pdf", "doc", "docx", "odt", "rtf", "xls", "xlsx", "ods").contains(extension);
    }
}
