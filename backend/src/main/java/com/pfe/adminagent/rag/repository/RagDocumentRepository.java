package com.pfe.adminagent.rag.repository;

import com.pfe.adminagent.rag.domain.RagDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RagDocumentRepository extends JpaRepository<RagDocument, UUID> {

    Optional<RagDocument> findByChecksum(String checksum);

    boolean existsByChecksum(String checksum);

    List<RagDocument> findAllByOrderByCreatedAtDesc();
}
