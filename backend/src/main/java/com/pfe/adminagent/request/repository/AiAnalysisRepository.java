package com.pfe.adminagent.request.repository;

import com.pfe.adminagent.request.domain.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {

    Optional<AiAnalysis> findByRequestId(UUID requestId);
}
