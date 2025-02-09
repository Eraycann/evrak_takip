package org.kafka.evraktakip.service;

import org.kafka.evraktakip.dto.DocumentDTO;
import org.kafka.evraktakip.exception.NotFoundException;
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
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CompanyRepository companyRepository;
    private final DocumentMapper documentMapper;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository,
                         CompanyRepository companyRepository,
                         DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.companyRepository = companyRepository;
        this.documentMapper = documentMapper;
    }

    public DocumentDTO uploadDocument(Long companyId, MultipartFile file) {
        // Firmayı kontrol et
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Firma bulunamadı: " + companyId));

        // Upload dizinini kontrol et
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                throw new RuntimeException("Upload dizini oluşturulamadı", e);
            }
        }

        // Benzersiz dosya adı oluştur
        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = Instant.now().toEpochMilli() + "_" + originalFileName;
        Path filePath = uploadPath.resolve(uniqueFileName);

        // Dosyayı kaydet
        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new RuntimeException("Dosya kaydedilemedi: " + originalFileName, e);
        }

        // Döküman bilgilerini veritabanına kaydet
        Document document = new Document();
        document.setFileName(uniqueFileName);
        document.setFilePath(filePath.toAbsolutePath().toString());
        document.setCompany(company);
        document.setOriginalFileName(originalFileName);
        document.setFileType(file.getContentType());
        document.setUploadDate(Instant.now());

        Document savedDocument = documentRepository.save(document);
        return documentMapper.toDto(savedDocument);
    }

    public Page<DocumentDTO> getDocumentsByCompany(Long companyId, Pageable pageable) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Firma bulunamadı: " + companyId));

        Page<Document> documentPage = documentRepository.findByCompany(company, pageable);
        return documentPage.map(documentMapper::toDto);
    }

    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evrak bulunamadı: " + id));
                
        // Dosyayı fiziksel olarak sil
        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Dosya silinirken hata oluştu", e);
        }
        
        documentRepository.deleteById(id);
    }

    public void openDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evrak bulunamadı: " + id));

        try {
            File file = new File(document.getFilePath());
            if (!file.exists()) {
                throw new RuntimeException("Dosya bulunamadı: " + document.getFilePath());
            }

            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows
                Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", file.getAbsolutePath() });
            } else if (os.contains("mac")) {
                // macOS
                Runtime.getRuntime().exec(new String[] { "open", file.getAbsolutePath() });
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux/Unix
                Runtime.getRuntime().exec(new String[] { "xdg-open", file.getAbsolutePath() });
            } else {
                // Diğer işletim sistemleri için Desktop API'yi deneyelim
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        desktop.open(file);
                    } else {
                        throw new RuntimeException("Dosya açma işlemi desteklenmiyor");
                    }
                } else {
                    throw new RuntimeException("Sistem dosya açma işlemini desteklemiyor");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Dosya açılırken hata oluştu: " + e.getMessage(), e);
        }
    }

    public Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evrak bulunamadı: " + id));
    }
}
