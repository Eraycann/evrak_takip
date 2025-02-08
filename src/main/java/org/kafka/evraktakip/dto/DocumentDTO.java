package org.kafka.evraktakip.dto;

import lombok.Data;

@Data
public class DocumentDTO {
    private Long id;
    private String fileName;
    private String filePath;
    private Long companyId;
}
