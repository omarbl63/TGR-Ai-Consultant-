package com.pfe.adminagent.request.repository;

import com.pfe.adminagent.request.domain.RequestEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestEventRepository extends JpaRepository<RequestEvent, UUID> {

    List<RequestEvent> findByRequestIdOrderByCreatedAtAsc(UUID requestId);
}
