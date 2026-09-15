package com.pfe.adminagent.request.repository;

import com.pfe.adminagent.request.domain.AdministrativeRequest;
import com.pfe.adminagent.request.domain.RequestStatus;
import com.pfe.adminagent.request.domain.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AdministrativeRequestRepository extends JpaRepository<AdministrativeRequest, UUID> {

    @Query(value = "SELECT nextval('request_reference_seq')", nativeQuery = true)
    long nextReferenceValue();

    List<AdministrativeRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    List<AdministrativeRequest> findByStatusInOrderByCreatedAtDesc(Collection<RequestStatus> statuses);

    List<AdministrativeRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<AdministrativeRequest> findByTypeOrderByCreatedAtDesc(RequestType type);

    List<AdministrativeRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(RequestStatus status);
}
