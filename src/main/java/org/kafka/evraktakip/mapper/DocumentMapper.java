package org.kafka.evraktakip.mapper;

import org.kafka.evraktakip.dto.DocumentDTO;
import org.kafka.evraktakip.model.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.name", target = "companyName")
    DocumentDTO toDto(Document document);

    @Mapping(target = "company", ignore = true)
    Document toEntity(DocumentDTO documentDTO);
}
