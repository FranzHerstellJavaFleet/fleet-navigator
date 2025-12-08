package io.javafleet.fleetnavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service for processing uploaded files (PDFs, images, text files)
 *
 * Verwendet Tesseract OCR für gescannte PDFs (zuverlässige Textextraktion).
 * Vision Models werden nur für echte Bildanalyse verwendet, nicht für OCR.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileProcessingService {

    private final TesseractOCRService tesseractService;

    /**
     * Process uploaded file based on its type
     *
     * @param file The uploaded file
     * @return ProcessedFile containing content and metadata
     */
    public ProcessedFile processFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        log.info("Processing file: {} ({})", originalFilename, contentType);

        if (contentType == null) {
            throw new IllegalArgumentException("Content type is null");
        }

        ProcessedFile result = new ProcessedFile();
        result.setFilename(originalFilename);
        result.setContentType(contentType);
        result.setSize(file.getSize());

        // Handle different file types
        String lowerFilename = originalFilename != null ? originalFilename.toLowerCase() : "";

        if (contentType.equals("application/pdf")) {
            // Process PDF - use Tesseract OCR for scanned PDFs
            processPDF(file, result);

        } else if (contentType.startsWith("image/")) {
            // Encode image as Base64 for Vision models (PNG, JPG, WebP, BMP, GIF, TIFF)
            // Vision models are appropriate for actual image analysis (photos, diagrams)
            String base64 = encodeImageToBase64(file);
            result.setBase64Content(base64);
            result.setType("image");

        } else if (contentType.equals("application/json") || lowerFilename.endsWith(".json")) {
            // JSON file
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            result.setTextContent(text);
            result.setType("json");

        } else if (contentType.equals("application/xml") || contentType.equals("text/xml") ||
                   lowerFilename.endsWith(".xml")) {
            // XML file
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            result.setTextContent(text);
            result.setType("xml");

        } else if (contentType.equals("text/csv") || lowerFilename.endsWith(".csv")) {
            // CSV file - format nicely for LLM
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            result.setTextContent(text);
            result.setType("csv");

        } else if (contentType.equals("text/html") || lowerFilename.endsWith(".html") ||
                   lowerFilename.endsWith(".htm")) {
            // HTML file
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            result.setTextContent(text);
            result.setType("html");

        } else if (contentType.startsWith("text/") ||
                   lowerFilename.endsWith(".txt") ||
                   lowerFilename.endsWith(".md")) {
            // Plain text or Markdown file
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            result.setTextContent(text);
            result.setType("text");

        } else {
            throw new IllegalArgumentException("Unsupported file type: " + contentType +
                " (supported: PDF, images, TXT, MD, HTML, JSON, XML, CSV)");
        }

        return result;
    }

    /**
     * Process PDF file - determines if it's text-based or scanned
     * Uses Tesseract OCR for scanned PDFs (reliable text extraction)
     */
    private void processPDF(MultipartFile file, ProcessedFile result) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            int textLength = text.trim().length();
            int pageCount = document.getNumberOfPages();

            log.info("PDF has {} pages, extracted {} characters of text", pageCount, textLength);

            // Threshold: less than 100 chars per page = likely scanned/image PDF
            if (textLength < pageCount * 100) {
                log.info("PDF appears to be scanned (low text content)");
                processScannedPDF(document, result, pageCount);
            } else {
                // Text-based PDF - use extracted text directly
                result.setTextContent(text);
                result.setType("pdf");
                log.info("Text-based PDF processed successfully");
            }
        }
    }

    /**
     * Process scanned PDF - renders pages and applies Tesseract OCR
     * Falls back to Vision model images only if Tesseract is unavailable
     */
    private void processScannedPDF(PDDocument document, ProcessedFile result, int pageCount) throws IOException {
        int maxPages = Math.min(pageCount, 10); // Max 10 pages

        // Check if Tesseract is available
        if (tesseractService.isAvailable()) {
            log.info("Using Tesseract OCR for scanned PDF ({} pages)", maxPages);
            try {
                // Render pages to BufferedImages
                List<BufferedImage> pageImages = renderPDFPagesToImages(document, maxPages);

                // Perform OCR on all pages
                String ocrText = tesseractService.performOCROnPages(pageImages);

                if (ocrText != null && !ocrText.trim().isEmpty()) {
                    result.setTextContent(ocrText);
                    result.setType("pdf"); // Treated as normal PDF since we have text
                    result.setOcrUsed(true);
                    log.info("Tesseract OCR successful: {} characters extracted", ocrText.length());
                    return;
                } else {
                    log.warn("Tesseract OCR returned empty result, falling back to Vision");
                }
            } catch (TesseractException e) {
                log.error("Tesseract OCR failed: {}, falling back to Vision", e.getMessage());
            }
        } else {
            log.warn("Tesseract OCR not available: {}. Falling back to Vision model.",
                    tesseractService.getUnavailableReason());
        }

        // Fallback: Render as Base64 images for Vision model
        // Note: Vision models may hallucinate - this is a fallback only!
        log.info("Falling back to Vision model for scanned PDF (may be less accurate)");
        List<String> pageImagesBase64 = renderPDFPagesToBase64(document, maxPages);
        result.setPageImages(pageImagesBase64);
        result.setType("scanned-pdf");
        result.setTextContent("SCANNED_PDF:" + pageCount + " pages (Vision fallback - install Tesseract for better results)");
    }

    /**
     * Render PDF pages to BufferedImages for Tesseract OCR
     */
    private List<BufferedImage> renderPDFPagesToImages(PDDocument document, int maxPages) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        PDFRenderer renderer = new PDFRenderer(document);

        int pagesToRender = Math.min(document.getNumberOfPages(), maxPages);
        log.info("Rendering {} PDF pages to images for OCR", pagesToRender);

        for (int page = 0; page < pagesToRender; page++) {
            // Render at 300 DPI for good OCR quality
            BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.RGB);
            images.add(image);
            log.debug("Rendered page {} for OCR", page + 1);
        }

        return images;
    }

    /**
     * Render PDF pages to Base64 images for Vision model analysis
     * Only used as fallback when Tesseract is unavailable
     */
    private List<String> renderPDFPagesToBase64(PDDocument document, int maxPages) throws IOException {
        List<String> pageImages = new ArrayList<>();
        PDFRenderer renderer = new PDFRenderer(document);

        int pagesToRender = Math.min(document.getNumberOfPages(), maxPages);
        log.info("Rendering {} PDF pages to Base64 for Vision model", pagesToRender);

        for (int page = 0; page < pagesToRender; page++) {
            // Render at 300 DPI for better quality
            BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.RGB);

            // Convert to PNG Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

            pageImages.add(base64);
            log.debug("Rendered page {} to Base64 ({} bytes)", page + 1, baos.size());
        }

        log.info("Rendered {} pages to Base64 images", pageImages.size());
        return pageImages;
    }

    /**
     * Encode image to Base64 for Vision models
     */
    private String encodeImageToBase64(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        log.info("Encoded image to Base64 ({} bytes)", bytes.length);
        return base64;
    }

    /**
     * DTO for processed file result
     */
    public static class ProcessedFile {
        private String filename;
        private String contentType;
        private long size;
        private String type; // "pdf", "image", "text", "scanned-pdf"
        private String textContent; // For PDFs and text files
        private String base64Content; // For images
        private List<String> pageImages; // For scanned PDFs (rendered pages as Base64)
        private boolean ocrUsed = false; // True if Tesseract OCR was used

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTextContent() {
            return textContent;
        }

        public void setTextContent(String textContent) {
            this.textContent = textContent;
        }

        public String getBase64Content() {
            return base64Content;
        }

        public void setBase64Content(String base64Content) {
            this.base64Content = base64Content;
        }

        public List<String> getPageImages() {
            return pageImages;
        }

        public void setPageImages(List<String> pageImages) {
            this.pageImages = pageImages;
        }

        public boolean isOcrUsed() {
            return ocrUsed;
        }

        public void setOcrUsed(boolean ocrUsed) {
            this.ocrUsed = ocrUsed;
        }
    }
}
