package io.javafleet.fleetnavigator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for creating ZIP archives
 */
@Slf4j
@Service
public class ZipService {

    /**
     * Create a ZIP file from a directory
     *
     * @param sourceDir The source directory to zip
     * @param zipFile   The output ZIP file path
     * @return The created ZIP file
     */
    public File createZip(Path sourceDir, Path zipFile) throws IOException {
        log.info("Creating ZIP: {} from {}", zipFile, sourceDir);

        try (FileOutputStream fos = new FileOutputStream(zipFile.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File sourceDirFile = sourceDir.toFile();
            zipDirectory(sourceDirFile, sourceDirFile.getName(), zos);

            log.info("ZIP created successfully: {} ({} bytes)", zipFile, zipFile.toFile().length());
        }

        return zipFile.toFile();
    }

    /**
     * Recursively add directory contents to ZIP
     */
    private void zipDirectory(File directory, String baseName, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) return;

        byte[] buffer = new byte[8192];

        for (File file : files) {
            String entryName = baseName + "/" + file.getName();

            if (file.isDirectory()) {
                // Add directory entry
                ZipEntry dirEntry = new ZipEntry(entryName + "/");
                zos.putNextEntry(dirEntry);
                zos.closeEntry();

                // Recurse into subdirectory
                zipDirectory(file, entryName, zos);
            } else {
                // Add file entry
                ZipEntry fileEntry = new ZipEntry(entryName);
                fileEntry.setTime(file.lastModified());
                zos.putNextEntry(fileEntry);

                // Write file content
                try (FileInputStream fis = new FileInputStream(file)) {
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }

                zos.closeEntry();
                log.debug("Added to ZIP: {}", entryName);
            }
        }
    }

    /**
     * Get file size in human-readable format
     */
    public String getHumanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
