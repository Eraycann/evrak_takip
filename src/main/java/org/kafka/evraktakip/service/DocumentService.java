package org.kafka.evraktakip.service;

import org.kafka.evraktakip.dto.DocumentDTO;
import org.kafka.evraktakip.dto.DocumentSearchCriteria;
import org.kafka.evraktakip.exception.BusinessException;
import org.kafka.evraktakip.exception.ErrorCode;
import org.kafka.evraktakip.exception.SystemException;
import org.kafka.evraktakip.mapper.DocumentMapper;
import org.kafka.evraktakip.model.Company;
import org.kafka.evraktakip.model.Document;
import org.kafka.evraktakip.repository.CompanyRepository;
import org.kafka.evraktakip.repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.kafka.evraktakip.specification.DocumentSpecifications;

import jakarta.transaction.Transactional;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CompanyRepository companyRepository;
    private final DocumentMapper documentMapper;

    public DocumentService(DocumentRepository documentRepository,
                         CompanyRepository companyRepository,
                         DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.companyRepository = companyRepository;
        this.documentMapper = documentMapper;
    }

    public DocumentDTO uploadDocument(Long companyId, MultipartFile file) {
        // Firma kontrolü
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // Dosya tipi kontrolü
        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        // Dosya boyutu kontrolü
        if (file.getSize() > 10_000_000) { // 10MB
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // Upload dizini kontrolü
        Path uploadPath = Paths.get("uploads");
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new SystemException(ErrorCode.DISK_SPACE_ERROR, e);
        }

        // Dosya kaydetme
        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = generateUniqueFileName(originalFileName);
        Path filePath = uploadPath.resolve(uniqueFileName);

        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, e);
        }

        // Veritabanı kaydı
        try {
            Document document = new Document();
            document.setFileName(uniqueFileName);
            document.setFilePath(filePath.toAbsolutePath().toString());
            document.setCompany(company);
            document.setOriginalFileName(originalFileName);
            document.setFileType(contentType);
            document.setUploadDate(Instant.now());

            Document savedDocument = documentRepository.save(document);
            return documentMapper.toDto(savedDocument);
        } catch (Exception e) {
            // Dosya kaydedildiyse sil
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {}
            throw new BusinessException(ErrorCode.DATABASE_ERROR, e);
        }
    }

    public Page<DocumentDTO> getDocumentsByCompany(Long companyId, DocumentSearchCriteria criteria, Pageable pageable) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        Specification<Document> spec = Specification.where(DocumentSpecifications.belongsToCompany(company));

        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().isEmpty()) {
            spec = spec.and(DocumentSpecifications.originalFileNameContains(criteria.getSearchTerm()));
        }

        if (criteria.getFileType() != null && !criteria.getFileType().isEmpty()) {
            spec = spec.and(DocumentSpecifications.hasFileType(criteria.getFileType()));
        }

        if (criteria.getStartDate() != null) {
            spec = spec.and(DocumentSpecifications.uploadedAfter(criteria.getStartDate().atStartOfDay()));
        }

        if (criteria.getEndDate() != null) {
            spec = spec.and(DocumentSpecifications.uploadedBefore(criteria.getEndDate().plusDays(1).atStartOfDay()));
        }

        Page<Document> documentPage = documentRepository.findAll(spec, pageable);
        return documentPage.map(documentMapper::toDto);
    }

    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
                
        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_DELETE_ERROR, e);
        }
        
        documentRepository.deleteById(id);
    }

    public void openDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        File file = new File(document.getFilePath());
        if (!file.exists()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            String fileExtension = getFileExtension(document.getOriginalFileName()).toLowerCase();

            ProcessBuilder processBuilder;
            if (os.contains("win")) {
                if (isImageFile(fileExtension)) {
                    // Windows için resim dosyalarını explorer ile aç
                    String absolutePath = file.getAbsolutePath();
                    processBuilder = new ProcessBuilder(
                        "explorer.exe",
                        absolutePath
                    );
                } else {
                    // Diğer dosyalar için varsayılan uygulamayı kullanalım
                    processBuilder = new ProcessBuilder();
                    processBuilder.command("cmd", "/c", "start", "", file.getAbsolutePath());
                }
            } else if (os.contains("mac")) {
                processBuilder = new ProcessBuilder("open", file.getAbsolutePath());
            } else if (os.contains("nix") || os.contains("nux")) {
                processBuilder = new ProcessBuilder("xdg-open", file.getAbsolutePath());
            } else {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        desktop.open(file);
                        return;
                    }
                }
                throw new RuntimeException("Sistem dosya açma işlemini desteklemiyor");
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String errorOutput = reader.lines().collect(Collectors.joining("\n"));
                    throw new RuntimeException("Dosya açılamadı. Hata kodu: " + exitCode + "\nHata: " + errorOutput);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new BusinessException(ErrorCode.FILE_OPEN_ERROR, e);
        }
    }

    private boolean isImageFile(String extension) {
        return extension.matches("jpg|jpeg|png|gif|bmp|webp|tiff|ico");
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // Uzantı yoksa boş string döndür
        }
        return fileName.substring(lastIndexOf + 1);
    }

    public Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
            contentType.startsWith("image/") ||
            contentType.equals("application/pdf") ||
            contentType.startsWith("application/msword") ||
            contentType.startsWith("application/vnd.openxmlformats-officedocument") ||
            contentType.startsWith("application/vnd.ms-excel") ||
            contentType.startsWith("text/")
        );
    }

    private String generateUniqueFileName(String originalFileName) {
        // Implement the logic to generate a unique file name based on the original file name
        // This is a placeholder and should be replaced with the actual implementation
        return originalFileName;
    }
}
