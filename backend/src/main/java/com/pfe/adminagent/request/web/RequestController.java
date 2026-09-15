package com.pfe.adminagent.request.web;

import com.pfe.adminagent.request.RequestService;
import com.pfe.adminagent.request.dto.AttachmentDto;
import com.pfe.adminagent.request.dto.CreateRequestRequest;
import com.pfe.adminagent.request.dto.DecisionRequest;
import com.pfe.adminagent.request.dto.RequestDetailDto;
import com.pfe.adminagent.request.dto.RequestStatsDto;
import com.pfe.adminagent.request.dto.RequestSummaryDto;
import com.pfe.adminagent.request.dto.UpdateRequestRequest;
import com.pfe.adminagent.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Requests", description = "Administrative requests: lifecycle, approvals, attachments")
@SecurityRequirement(name = "bearerAuth")
public class RequestController {

    private static final Set<String> APPROVER_ROLES = Set.of("ROLE_MANAGER", "ROLE_ADMIN");

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    // ---- Employee endpoints ----

    @PostMapping
    @Operation(summary = "Create a request (draft)")
    public ResponseEntity<RequestDetailDto> create(@Valid @RequestBody CreateRequestRequest body,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.create(principal.getId(), body));
    }

    @GetMapping("/mine")
    @Operation(summary = "List the current user's requests")
    public List<RequestSummaryDto> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return requestService.listForRequester(principal.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an editable request's data (owner, DRAFT or CHANGES_REQUESTED)")
    public RequestDetailDto update(@PathVariable UUID id,
                                   @RequestBody UpdateRequestRequest body,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return requestService.updateStructuredData(id, principal.getId(), body.structuredData());
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit a request for approval")
    public RequestDetailDto submit(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return requestService.submit(id, principal.getId());
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "Attach a supporting document to a request")
    public ResponseEntity<AttachmentDto> attach(@PathVariable UUID id,
                                                @RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "label", required = false) String label,
                                                @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        AttachmentDto dto = requestService.addAttachment(id, principal.getId(), isApprover(principal),
                file.getOriginalFilename(), file.getContentType(), file.getBytes(), label);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}/attachments/{attId}")
    @Operation(summary = "Download a supporting document (owner or approver)")
    public ResponseEntity<byte[]> download(@PathVariable UUID id, @PathVariable UUID attId,
                                           @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        var att = requestService.getAttachmentForDownload(id, attId, principal.getId(), isApprover(principal));
        byte[] data = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(att.getStoragePath()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE,
                        att.getContentType() != null ? att.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + att.getFilename() + "\"")
                .body(data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full request details (owner or approver)")
    public RequestDetailDto get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return requestService.getDetail(id, principal.getId(), isApprover(principal));
    }

    // ---- Approver endpoints ----

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "List requests awaiting a decision (approver queue)")
    public List<RequestSummaryDto> pending() {
        return requestService.listPending();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "List all requests")
    public List<RequestSummaryDto> all() {
        return requestService.listAll();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Dashboard KPI counters")
    public RequestStatsDto stats() {
        return requestService.stats();
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Approve, reject, or request changes on a request")
    public RequestDetailDto decide(@PathVariable UUID id,
                                   @Valid @RequestBody DecisionRequest decision,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return requestService.decide(id, principal.getId(), decision);
    }

    private boolean isApprover(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(APPROVER_ROLES::contains);
    }
}
