package org.kafka.evraktakip.service;

import jakarta.transaction.Transactional;
import org.kafka.evraktakip.dto.CompanyDTO;
import org.kafka.evraktakip.exception.NotFoundException;
import org.kafka.evraktakip.mapper.CompanyMapper;
import org.kafka.evraktakip.model.Company;
import org.kafka.evraktakip.repository.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public CompanyService(CompanyRepository companyRepository, CompanyMapper companyMapper) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
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

    public Page<CompanyDTO> getAllCompanies(Pageable pageable) {
        return companyRepository.findAll(pageable)
                .map(companyMapper::toDto);
    }
} 