package io.javafleet.fleetnavigator.dto;

import lombok.Data;

@Data
public class UploadContextFileRequest {
    private Long projectId;
    private String filename;
    private String content;
    private String fileType; // .txt, .md, .java, etc.
}
