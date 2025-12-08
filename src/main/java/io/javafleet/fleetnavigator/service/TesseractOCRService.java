package io.javafleet.fleetnavigator.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Service für Tesseract OCR - zuverlässige Textextraktion aus Bildern
 *
 * Verwendet das System-installierte Tesseract für echte OCR statt Vision-Models.
 * Benötigt: tesseract-ocr und tesseract-ocr-deu auf dem System
 */
@Service
@Slf4j
public class TesseractOCRService {

    private Tesseract tesseract;
    private boolean available = false;
    private String unavailableReason = "";

    @PostConstruct
    public void init() {
        try {
            tesseract = new Tesseract();

            // Standard-Datapfade für verschiedene Betriebssysteme
            String dataPath = findTessDataPath();
            if (dataPath != null) {
                tesseract.setDatapath(dataPath);
                log.info("Tesseract datapath: {}", dataPath);
            }

            // Deutsch als primäre Sprache, Englisch als Fallback
            tesseract.setLanguage("deu+eng");

            // OCR Engine Mode: LSTM (beste Qualität)
            tesseract.setOcrEngineMode(1);

            // Page Segmentation Mode: Auto
            tesseract.setPageSegMode(3);

            // Test ob Tesseract funktioniert (einfacher Init-Test)
            log.info("Tesseract OCR initialisiert (Sprachen: deu+eng)");
            available = true;

        } catch (Exception e) {
            unavailableReason = e.getMessage();
            log.warn("Tesseract OCR nicht verfügbar: {}. " +
                    "Installiere mit: sudo apt install tesseract-ocr tesseract-ocr-deu",
                    e.getMessage());
            available = false;
        }
    }

    /**
     * Findet den tessdata Pfad je nach Betriebssystem
     */
    private String findTessDataPath() {
        String[] possiblePaths = {
            "/usr/share/tesseract-ocr/5/tessdata",    // Ubuntu/Debian 5.x
            "/usr/share/tesseract-ocr/4.00/tessdata", // Ubuntu/Debian 4.x
            "/usr/share/tessdata",                     // Generic Linux
            "/usr/local/share/tessdata",               // macOS Homebrew
            "/opt/homebrew/share/tessdata",            // macOS ARM Homebrew
            "C:\\Program Files\\Tesseract-OCR\\tessdata", // Windows
            System.getenv("TESSDATA_PREFIX")           // Environment variable
        };

        for (String path : possiblePaths) {
            if (path != null) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    // Prüfe ob deu.traineddata existiert
                    File deuData = new File(dir, "deu.traineddata");
                    if (deuData.exists()) {
                        return path;
                    }
                }
            }
        }

        log.warn("Kein tessdata Pfad gefunden. Versuche ohne expliziten Pfad...");
        return null;
    }

    /**
     * Prüft ob Tesseract OCR verfügbar ist
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Gibt den Grund zurück, warum Tesseract nicht verfügbar ist
     */
    public String getUnavailableReason() {
        return unavailableReason;
    }

    /**
     * Führt OCR auf einem BufferedImage aus
     *
     * @param image Das zu analysierende Bild
     * @return Extrahierter Text
     * @throws TesseractException Bei OCR-Fehlern
     */
    public String performOCR(BufferedImage image) throws TesseractException {
        if (!available) {
            throw new IllegalStateException("Tesseract OCR ist nicht verfügbar: " + unavailableReason);
        }

        long startTime = System.currentTimeMillis();
        String result = tesseract.doOCR(image);
        long duration = System.currentTimeMillis() - startTime;

        log.debug("OCR abgeschlossen in {}ms, {} Zeichen extrahiert",
                duration, result.length());

        return result.trim();
    }

    /**
     * Führt OCR auf mehreren Bildern aus und kombiniert das Ergebnis
     *
     * @param images Liste von BufferedImages (z.B. PDF-Seiten)
     * @return Kombinierter extrahierter Text
     */
    public String performOCROnPages(List<BufferedImage> images) throws TesseractException {
        if (!available) {
            throw new IllegalStateException("Tesseract OCR ist nicht verfügbar: " + unavailableReason);
        }

        StringBuilder fullText = new StringBuilder();
        int pageNum = 1;

        for (BufferedImage image : images) {
            log.info("OCR Seite {}/{}", pageNum, images.size());
            String pageText = performOCR(image);

            if (!pageText.isEmpty()) {
                if (fullText.length() > 0) {
                    fullText.append("\n\n--- Seite ").append(pageNum).append(" ---\n\n");
                }
                fullText.append(pageText);
            }
            pageNum++;
        }

        log.info("OCR abgeschlossen: {} Seiten, {} Zeichen total",
                images.size(), fullText.length());

        return fullText.toString();
    }

    /**
     * Führt OCR auf einer Datei aus
     *
     * @param imageFile Die Bilddatei
     * @return Extrahierter Text
     */
    public String performOCR(File imageFile) throws TesseractException {
        if (!available) {
            throw new IllegalStateException("Tesseract OCR ist nicht verfügbar: " + unavailableReason);
        }

        return tesseract.doOCR(imageFile).trim();
    }
}
