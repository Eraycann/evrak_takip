package org.kafka.evraktakip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyDTO {
    private Long id;

    @NotBlank(message = "Firma adı boş olamaz")
    private String name;

    private String address;
    private String phone;
    private String email;
    private Long documentCount;
}
