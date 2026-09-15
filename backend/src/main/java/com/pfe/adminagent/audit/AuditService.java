package com.pfe.adminagent.audit;

import com.pfe.adminagent.audit.domain.AuditLog;
import com.pfe.adminagent.audit.dto.AuditLogDto;
import com.pfe.adminagent.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Central place to append audit records. Runs in its own transaction so an
 * audit failure never rolls back the business operation it describes.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorId, String action, String entityType, String entityId, Map<String, Object> details) {
        auditLogRepository.save(new AuditLog(actorId, action, entityType, entityId, details));
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> recent(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 200)))
                .map(AuditLogDto::from);
    }
}
