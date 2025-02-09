package org.kafka.evraktakip.service;

import jakarta.transaction.Transactional;
import org.kafka.evraktakip.dto.CompanyDTO;
import org.kafka.evraktakip.exception.NotFoundException;
import org.kafka.evraktakip.mapper.CompanyMapper;
import org.kafka.evraktakip.model.Company;
import org.kafka.evraktakip.repository.CompanyRepository;
import org.kafka.evraktakip.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final DocumentRepository documentRepository;

    public CompanyService(CompanyRepository companyRepository, CompanyMapper companyMapper, DocumentRepository documentRepository) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
        this.documentRepository = documentRepository;
    }

    public CompanyDTO createCompany(CompanyDTO companyDTO) {
        Company company = companyMapper.toEntity(companyDTO);
        Company savedCompany = companyRepository.save(company);
        return companyMapper.toDto(savedCompany);
    }

    public CompanyDTO updateCompany(Long id, CompanyDTO companyDTO) {
        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Firma bulunamadı: " + id));
        
        existingCompany.setName(companyDTO.getName());
        Company updatedCompany = companyRepository.save(existingCompany);
        return companyMapper.toDto(updatedCompany);
    }

    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new NotFoundException("Firma bulunamadı: " + id);
        }
        companyRepository.deleteById(id);
    }

    public CompanyDTO getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Firma bulunamadı: " + id));
        return companyMapper.toDto(company);
    }

    public Page<CompanyDTO> getAllCompanies(Pageable pageable, String search) {
        Specification<Company> spec = Specification.where(null);
        
        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%")
            );
        }
        
        return companyRepository.findAll(spec, pageable)
                .map(company -> {
                    CompanyDTO dto = companyMapper.toDto(company);
                    Long documentCount = documentRepository.countByCompany(company);
                    dto.setDocumentCount(documentCount);
                    return dto;
                });
    }
} 