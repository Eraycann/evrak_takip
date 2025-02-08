package org.kafka.evraktakip.mapper;

import org.kafka.evraktakip.dto.CompanyDTO;
import org.kafka.evraktakip.model.Company;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    CompanyDTO toDto(Company company);
    Company toEntity(CompanyDTO companyDTO);
}
