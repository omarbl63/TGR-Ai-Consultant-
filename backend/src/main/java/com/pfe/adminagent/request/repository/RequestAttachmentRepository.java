package com.pfe.adminagent.request.repository;

import com.pfe.adminagent.request.domain.RequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestAttachmentRepository extends JpaRepository<RequestAttachment, UUID> {

    List<RequestAttachment> findByRequestIdOrderByCreatedAtAsc(UUID requestId);
}
