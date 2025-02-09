package org.kafka.evraktakip.controller;

import jakarta.validation.Valid;
import org.kafka.evraktakip.dto.CompanyDTO;
import org.kafka.evraktakip.service.CompanyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Firma İşlemleri", description = "Firma CRUD işlemleri")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Operation(summary = "Yeni firma oluştur")
    @PostMapping
    public ResponseEntity<CompanyDTO> createCompany(@Valid @RequestBody CompanyDTO companyDTO) {
        return new ResponseEntity<>(companyService.createCompany(companyDTO), HttpStatus.CREATED);
    }

    @Operation(summary = "Firma bilgilerini güncelle")
    @PutMapping("/{id}")
    public ResponseEntity<CompanyDTO> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyDTO companyDTO) {
        return ResponseEntity.ok(companyService.updateCompany(id, companyDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyDTO> getCompany(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CompanyDTO>> getAllCompanies(
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) String search
    ) {
        Page<CompanyDTO> companies = companyService.getAllCompanies(pageable, search);
        return ResponseEntity.ok(companies);
    }
} 