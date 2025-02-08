package org.kafka.evraktakip.mapper;

import org.kafka.evraktakip.dto.DocumentDTO;
import org.kafka.evraktakip.model.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    @Mapping(source = "company.id", target = "companyId")
    DocumentDTO toDto(Document document);

    // Not: Document oluşturulurken company bilgisini ayrı atayacağımız için ters dönüşüm burada gerekmez.
    Document toEntity(DocumentDTO documentDTO);
}
