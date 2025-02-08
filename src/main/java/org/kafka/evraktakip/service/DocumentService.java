package org.kafka.evraktakip.service;

import org.kafka.evraktakip.dto.CompanyDTO;
import org.kafka.evraktakip.dto.DocumentDTO;
import org.kafka.evraktakip.exception.NotFoundException;
import org.kafka.evraktakip.mapper.CompanyMapper;
import org.kafka.evraktakip.mapper.DocumentMapper;
import org.kafka.evraktakip.model.Company;
import org.kafka.evraktakip.model.Document;
import org.kafka.evraktakip.repository.CompanyRepository;
import org.kafka.evraktakip.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Service
@Transactional
public class DocumentService {

    private final CompanyRepository companyRepository;
    private final DocumentRepository documentRepository;
    private final CompanyMapper companyMapper;
    private final DocumentMapper documentMapper;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public DocumentService(CompanyRepository companyRepository,
                           DocumentRepository documentRepository,
                           CompanyMapper companyMapper,
                           DocumentMapper documentMapper) {
        this.companyRepository = companyRepository;
        this.documentRepository = documentRepository;
        this.companyMapper = companyMapper;
        this.documentMapper = documentMapper;
    }

    // Firma oluşturma
    public CompanyDTO createCompany(CompanyDTO companyDTO) {
        Company company = companyMapper.toEntity(companyDTO);
        Company savedCompany = companyRepository.save(company);
        return companyMapper.toDto(savedCompany);
    }

    // Firma için evrak yükleme
    public DocumentDTO uploadDocument(Long companyId, MultipartFile file) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + companyId));

        // Upload dizininin varlığını kontrol et, yoksa oluştur
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                throw new RuntimeException("Could not create upload directory", e);
            }
        }

        // Çakışmayı önlemek için benzersiz dosya adı: timestamp_orijinalAd
        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = Instant.now().toEpochMilli() + "_" + originalFileName;
        Path filePath = uploadPath.resolve(uniqueFileName);

        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error storing file: " + originalFileName, e);
        }

        Document document = new Document();
        document.setFileName(uniqueFileName);
        document.setFilePath(filePath.toAbsolutePath().toString());
        document.setCompany(company);

        Document savedDocument = documentRepository.save(document);
        return documentMapper.toDto(savedDocument);
    }

    // Firma evraklarını pagination ile listeleme
    public Page<DocumentDTO> getDocumentsByCompany(Long companyId, Pageable pageable) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + companyId));

        Page<Document> documentPage = documentRepository.findByCompany(company, pageable);
        return documentPage.map(documentMapper::toDto);
    }
}
