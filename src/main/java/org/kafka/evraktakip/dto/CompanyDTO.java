package org.kafka.evraktakip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyDTO {
    private Long id;

    @NotBlank(message = "Company name must not be blank")
    private String name;
}
