package org.kafka.evraktakip.controller;

import io.swagger.v3.oas.annotations.Parameter;
import org.kafka.evraktakip.dto.CompanyDTO;
import org.kafka.evraktakip.dto.DocumentDTO;
import org.kafka.evraktakip.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // Firma oluşturma endpointi
    @PostMapping("/companies")
    public ResponseEntity<CompanyDTO> createCompany(@Valid @RequestBody CompanyDTO companyDTO) {
        CompanyDTO savedCompany = documentService.createCompany(companyDTO);
        return new ResponseEntity<>(savedCompany, HttpStatus.CREATED);
    }

    // Firma için evrak yükleme (multipart form-data)
    @PostMapping(value = "/companies/{companyId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDTO> uploadDocument(
            @PathVariable Long companyId,
            @Parameter(description = "Yüklenecek dosya") @RequestParam("file") MultipartFile file) {
        DocumentDTO documentDTO = documentService.uploadDocument(companyId, file);
        return new ResponseEntity<>(documentDTO, HttpStatus.CREATED);
    }

    // Firma evraklarını pagination ile listeleme endpointi
    @GetMapping("/companies/{companyId}/documents")
    public ResponseEntity<Page<DocumentDTO>> getDocumentsByCompany(@PathVariable Long companyId, Pageable pageable) {
        Page<DocumentDTO> documents = documentService.getDocumentsByCompany(companyId, pageable);
        return new ResponseEntity<>(documents, HttpStatus.OK);
    }
}
