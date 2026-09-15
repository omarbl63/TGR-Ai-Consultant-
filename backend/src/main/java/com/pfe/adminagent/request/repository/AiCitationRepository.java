package com.pfe.adminagent.request.repository;

import com.pfe.adminagent.request.domain.AiCitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiCitationRepository extends JpaRepository<AiCitation, UUID> {

    List<AiCitation> findByAnalysisIdOrderByScoreDesc(UUID analysisId);
}
