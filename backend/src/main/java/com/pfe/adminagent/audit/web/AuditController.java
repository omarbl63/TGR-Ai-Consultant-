package com.pfe.adminagent.audit.web;

import com.pfe.adminagent.audit.AuditService;
import com.pfe.adminagent.audit.dto.AuditLogDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Audit log (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List recent audit entries (most recent first)")
    public Page<AuditLogDto> recent(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "50") int size) {
        return auditService.recent(page, size);
    }
}
