package org.kafka.evraktakip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.kafka.evraktakip.dto.DocumentDTO;
import org.kafka.evraktakip.model.Document;
import org.kafka.evraktakip.service.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Evrak İşlemleri", description = "Evrak yükleme, listeleme ve silme işlemleri")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "Firma için evrak yükle")
    @PostMapping(value = "/upload/{companyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDTO> uploadDocument(
            @PathVariable Long companyId,
            @Parameter(description = "Yüklenecek dosya") 
            @RequestParam("file") MultipartFile file) {
        DocumentDTO documentDTO = documentService.uploadDocument(companyId, file);
        return new ResponseEntity<>(documentDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Firma evraklarını listele")
    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<DocumentDTO>> getDocumentsByCompany(
            @PathVariable Long companyId,
            Pageable pageable) {
        Page<DocumentDTO> documents = documentService.getDocumentsByCompany(companyId, pageable);
        return ResponseEntity.ok(documents);
    }

    @Operation(summary = "Evrak sil")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Evrak aç")
    @PostMapping("/{id}/open")
    public ResponseEntity<Void> openDocument(@PathVariable Long id) {
        documentService.openDocument(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Evrak indir")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        Document document = documentService.getDocument(id);
        try {
            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException("Dosya indirilemedi", e);
        }
    }
}
