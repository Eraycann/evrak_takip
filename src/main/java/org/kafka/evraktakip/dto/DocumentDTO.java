package org.kafka.evraktakip.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class DocumentDTO {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private Instant uploadDate;
    private Long companyId;
    private String companyName;
}
