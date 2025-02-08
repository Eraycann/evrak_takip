package org.kafka.evraktakip.repository;

import org.kafka.evraktakip.model.Company;
import org.kafka.evraktakip.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByCompany(Company company, Pageable pageable);
}
