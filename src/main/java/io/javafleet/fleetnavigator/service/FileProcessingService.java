package io.javafleet.fleetnavigator.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for processing uploaded files (PDFs, images, text files)
 */
@Service
@Slf4j
public class FileProcessingService {

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
        if (contentType.equals("application/pdf")) {
            // Extract text from PDF
            String text = extractTextFromPDF(file);
            result.setTextContent(text);
            result.setType("pdf");
        } else if (contentType.startsWith("image/")) {
            // Encode image as Base64 for Vision models
            String base64 = encodeImageToBase64(file);
            result.setBase64Content(base64);
            result.setType("image");
        } else if (contentType.startsWith("text/") ||
                   originalFilename.endsWith(".txt") ||
                   originalFilename.endsWith(".md")) {
            // Read plain text file
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            result.setTextContent(text);
            result.setType("text");
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }

        return result;
    }

    /**
     * Extract text from PDF file
     */
    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Extracted {} characters from PDF", text.length());
            return text;
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            throw new IOException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
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
        private String type; // "pdf", "image", "text"
        private String textContent; // For PDFs and text files
        private String base64Content; // For images

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
    }
}
