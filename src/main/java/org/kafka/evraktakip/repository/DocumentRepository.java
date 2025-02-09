package org.kafka.evraktakip.repository;

import org.kafka.evraktakip.model.Company;
import org.kafka.evraktakip.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
    Page<Document> findByCompany(Company company, Pageable pageable);
    Long countByCompany(Company company);
}
