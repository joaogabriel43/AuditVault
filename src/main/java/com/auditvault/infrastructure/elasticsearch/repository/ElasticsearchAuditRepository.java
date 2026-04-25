package com.auditvault.infrastructure.elasticsearch.repository;

import com.auditvault.infrastructure.elasticsearch.document.AuditDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ElasticsearchAuditRepository extends ElasticsearchRepository<AuditDocument, String> {
    Page<AuditDocument> findByPayloadContaining(String text, Pageable pageable);
}
