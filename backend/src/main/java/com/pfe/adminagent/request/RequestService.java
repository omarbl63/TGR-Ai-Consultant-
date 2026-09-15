package com.pfe.adminagent.request;

import com.pfe.adminagent.audit.AuditService;
import com.pfe.adminagent.common.exception.ApiException;
import com.pfe.adminagent.common.exception.ResourceNotFoundException;
import com.pfe.adminagent.config.AiProperties;
import com.pfe.adminagent.notification.NotificationService;
import com.pfe.adminagent.notification.domain.NotificationType;
import com.pfe.adminagent.request.domain.AdministrativeRequest;
import com.pfe.adminagent.request.domain.AiAnalysis;
import com.pfe.adminagent.request.domain.AiCitation;
import com.pfe.adminagent.request.domain.RequestAttachment;
import com.pfe.adminagent.request.domain.RequestEvent;
import com.pfe.adminagent.request.domain.RequestEventType;
import com.pfe.adminagent.request.domain.RequestStatus;
import com.pfe.adminagent.request.dto.AiAnalysisDto;
import com.pfe.adminagent.request.dto.AttachmentDto;
import com.pfe.adminagent.request.dto.CitationDto;
import com.pfe.adminagent.request.dto.CreateRequestRequest;
import com.pfe.adminagent.request.dto.DecisionRequest;
import com.pfe.adminagent.request.dto.RequestDetailDto;
import com.pfe.adminagent.request.dto.RequestEventDto;
import com.pfe.adminagent.request.dto.RequestStatsDto;
import com.pfe.adminagent.request.dto.RequestSummaryDto;
import com.pfe.adminagent.request.repository.AdministrativeRequestRepository;
import com.pfe.adminagent.request.repository.AiAnalysisRepository;
import com.pfe.adminagent.request.repository.AiCitationRepository;
import com.pfe.adminagent.request.repository.RequestAttachmentRepository;
import com.pfe.adminagent.request.repository.RequestEventRepository;
import com.pfe.adminagent.user.domain.Role;
import com.pfe.adminagent.user.domain.User;
import com.pfe.adminagent.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates the request lifecycle: creation, submission, human decisions,
 * timeline, and assembly of the approver-facing detail view (with AI analysis).
 * The AI recommends; the approve/reject decision here is always a human's.
 */
@Service
public class RequestService {

    private static final Set<RequestStatus> SUBMITTABLE =
            Set.of(RequestStatus.DRAFT, RequestStatus.CHANGES_REQUESTED);
    private static final Set<RequestStatus> DECIDABLE =
            Set.of(RequestStatus.SUBMITTED, RequestStatus.UNDER_REVIEW);
    private static final Set<RequestStatus> PENDING =
            Set.of(RequestStatus.SUBMITTED, RequestStatus.UNDER_REVIEW);

    private final AdministrativeRequestRepository requests;
    private final RequestEventRepository events;
    private final RequestAttachmentRepository attachments;
    private final AiAnalysisRepository analyses;
    private final AiCitationRepository citations;
    private final UserRepository users;
    private final NotificationService notifications;
    private final AuditService audit;
    private final AttachmentStorageService attachmentStorage;

    public RequestService(AdministrativeRequestRepository requests,
                          RequestEventRepository events,
                          RequestAttachmentRepository attachments,
                          AiAnalysisRepository analyses,
                          AiCitationRepository citations,
                          UserRepository users,
                          NotificationService notifications,
                          AuditService audit,
                          AttachmentStorageService attachmentStorage) {
        this.requests = requests;
        this.events = events;
        this.attachments = attachments;
        this.analyses = analyses;
        this.citations = citations;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
        this.attachmentStorage = attachmentStorage;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public RequestDetailDto create(UUID requesterId, CreateRequestRequest req) {
        AdministrativeRequest request = AdministrativeRequest.builder()
                .reference(nextReference())
                .type(req.type())
                .title(req.title())
                .requesterId(requesterId)
                .conversationId(req.conversationId())
                .structuredData(req.structuredData() != null ? req.structuredData() : new HashMap<>())
                .build();
        requests.save(request);
        events.save(new RequestEvent(request.getId(), RequestEventType.CREATED, requesterId, null));
        audit.record(requesterId, "REQUEST_CREATED", "AdministrativeRequest",
                request.getId().toString(), Map.of("type", req.type().name(), "reference", request.getReference()));
        return getDetail(request.getId(), requesterId, false);
    }

    // ------------------------------------------------------------------ submit

    @Transactional
    public RequestDetailDto submit(UUID requestId, UUID actingUserId) {
        AdministrativeRequest request = require(requestId);
        if (!request.getRequesterId().equals(actingUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seul le demandeur peut soumettre cette demande");
        }
        if (!SUBMITTABLE.contains(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La demande ne peut pas être soumise depuis le statut " + request.getStatus());
        }
        request.markSubmitted();
        requests.save(request);
        events.save(new RequestEvent(requestId, RequestEventType.SUBMITTED, actingUserId, null));

        notifyApprovers(request);
        audit.record(actingUserId, "REQUEST_SUBMITTED", "AdministrativeRequest",
                requestId.toString(), Map.of("reference", request.getReference()));
        return getDetail(requestId, actingUserId, false);
    }

    // ------------------------------------------------------------------ decide

    @Transactional
    public RequestDetailDto decide(UUID requestId, UUID approverId, DecisionRequest decision) {
        AdministrativeRequest request = require(requestId);
        if (!DECIDABLE.contains(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La demande ne peut pas être décidée depuis le statut " + request.getStatus());
        }
        DecisionRequest.Action action = decision.action();
        request.decide(action.resultingStatus(), approverId);
        requests.save(request);
        events.save(new RequestEvent(requestId, action.eventType(), approverId, decision.comment()));

        notifyRequesterOfDecision(request, action, decision.comment());
        audit.record(approverId, "REQUEST_" + action.name(), "AdministrativeRequest",
                requestId.toString(), Map.of("reference", request.getReference(),
                        "status", action.resultingStatus().name()));
        return getDetail(requestId, approverId, true);
    }

    // ------------------------------------------------------------------- lists

    @Transactional(readOnly = true)
    public List<RequestSummaryDto> listForRequester(UUID requesterId) {
        return toSummaries(requests.findByRequesterIdOrderByCreatedAtDesc(requesterId));
    }

    @Transactional(readOnly = true)
    public List<RequestSummaryDto> listPending() {
        return toSummaries(requests.findByStatusInOrderByCreatedAtDesc(PENDING));
    }

    @Transactional(readOnly = true)
    public List<RequestSummaryDto> listAll() {
        return toSummaries(requests.findAllByOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public RequestStatsDto stats() {
        long pending = requests.countByStatus(RequestStatus.SUBMITTED)
                + requests.countByStatus(RequestStatus.UNDER_REVIEW);
        return new RequestStatsDto(
                pending,
                requests.countByStatus(RequestStatus.APPROVED),
                requests.countByStatus(RequestStatus.REJECTED),
                requests.countByStatus(RequestStatus.CHANGES_REQUESTED),
                requests.count());
    }

    // ------------------------------------------------------------------ detail

    @Transactional(readOnly = true)
    public RequestDetailDto getDetail(UUID requestId, UUID actingUserId, boolean canViewAll) {
        AdministrativeRequest request = require(requestId);
        if (!canViewAll && !request.getRequesterId().equals(actingUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à consulter cette demande");
        }

        User requester = users.findById(request.getRequesterId()).orElse(null);
        String decidedByName = request.getDecidedBy() == null ? null
                : users.findById(request.getDecidedBy()).map(User::getFullName).orElse(null);

        AiAnalysisDto analysisDto = analyses.findByRequestId(requestId)
                .map(a -> AiAnalysisDto.of(a, citations.findByAnalysisIdOrderByScoreDesc(a.getId())
                        .stream().map(CitationDto::from).toList()))
                .orElse(null);

        List<RequestEvent> timeline = events.findByRequestIdOrderByCreatedAtAsc(requestId);
        Map<UUID, String> actorNames = resolveNames(timeline.stream()
                .map(RequestEvent::getActorId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        List<RequestEventDto> timelineDto = timeline.stream()
                .map(e -> RequestEventDto.of(e, e.getActorId() == null ? "Système IA"
                        : actorNames.getOrDefault(e.getActorId(), "Utilisateur")))
                .toList();

        List<AttachmentDto> attachmentDtos = attachments.findByRequestIdOrderByCreatedAtAsc(requestId)
                .stream().map(AttachmentDto::from).toList();

        return RequestDetailDto.of(request,
                requester != null ? requester.getFullName() : null,
                requester != null ? requester.getDepartment() : null,
                decidedByName, analysisDto, timelineDto, attachmentDtos);
    }

    // -------------------------------------------------------------- attachments

    @Transactional
    public AttachmentDto addAttachment(UUID requestId, UUID actingUserId, boolean canViewAll,
                                       String filename, String contentType, byte[] content, String documentLabel) {
        AdministrativeRequest request = require(requestId);
        if (!canViewAll && !request.getRequesterId().equals(actingUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à modifier cette demande");
        }
        // Re-uploading a piece for the same required document replaces the previous file.
        if (documentLabel != null && !documentLabel.isBlank()) {
            for (RequestAttachment old : attachments.findByRequestIdOrderByCreatedAtAsc(requestId)) {
                if (documentLabel.equals(old.getDocumentLabel())) {
                    try {
                        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(old.getStoragePath()));
                    } catch (Exception ignored) {
                        // best effort — orphaned file is harmless
                    }
                    attachments.delete(old);
                }
            }
        }
        var attachment = attachmentStorage.store(requestId, filename, contentType, content, documentLabel);
        attachments.save(attachment);
        audit.record(actingUserId, "ATTACHMENT_ADDED", "AdministrativeRequest",
                requestId.toString(), Map.of("filename", filename, "document", documentLabel == null ? "" : documentLabel));
        return AttachmentDto.from(attachment);
    }

    /** Distinct document labels already provided (attached) for a request. */
    @Transactional(readOnly = true)
    public java.util.Set<String> providedDocumentLabels(UUID requestId) {
        return attachments.findByRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(RequestAttachment::getDocumentLabel)
                .filter(l -> l != null && !l.isBlank())
                .collect(Collectors.toSet());
    }

    /** Loads an attachment for download, after an access check (owner or approver). */
    @Transactional(readOnly = true)
    public RequestAttachment getAttachmentForDownload(UUID requestId, UUID attachmentId,
                                                      UUID actingUserId, boolean canViewAll) {
        AdministrativeRequest request = require(requestId);
        if (!canViewAll && !request.getRequesterId().equals(actingUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à consulter cette pièce");
        }
        return attachments.findById(attachmentId)
                .filter(a -> a.getRequestId().equals(requestId))
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable"));
    }

    // --------------------------------------------- AI analysis persistence (Phase 4)

    /**
     * Persists (or replaces) the AI analysis for a request and records an
     * AI_ANALYZED timeline event. Called by the orchestrator.
     */
    @Transactional
    public void saveAnalysis(AiAnalysis analysis, List<AiCitation> analysisCitations) {
        UUID requestId = analysis.getRequestId();
        require(requestId);
        analyses.findByRequestId(requestId).ifPresent(existing -> {
            citations.deleteAll(citations.findByAnalysisIdOrderByScoreDesc(existing.getId()));
            analyses.delete(existing);
            analyses.flush();
        });
        analyses.save(analysis);
        citations.saveAll(analysisCitations);
        events.save(new RequestEvent(requestId, RequestEventType.AI_ANALYZED, null,
                analysis.getRecommendation() == null ? "Analyse de conformité IA terminée."
                        : "Analyse IA terminée — recommandation : " + analysis.getRecommendation().label() + "."));
    }

    // ------------------------------------------------------------------ helpers

    @Transactional(readOnly = true)
    public boolean isDraft(UUID requestId) {
        return requests.findById(requestId)
                .map(r -> r.getStatus() == RequestStatus.DRAFT).orElse(false);
    }

    @Transactional(readOnly = true)
    public int attachmentCount(UUID requestId) {
        return attachments.findByRequestIdOrderByCreatedAtAsc(requestId).size();
    }

    /** All attachments of a request (used by the AI to read & verify their content). */
    @Transactional(readOnly = true)
    public List<RequestAttachment> attachmentsOf(UUID requestId) {
        return attachments.findByRequestIdOrderByCreatedAtAsc(requestId);
    }

    @Transactional(readOnly = true)
    public com.pfe.adminagent.request.domain.RequestType typeOf(UUID requestId) {
        return require(requestId).getType();
    }

    /**
     * Updates the structured data of a request. Allowed only for the owner and only
     * while the request is editable (DRAFT or CHANGES_REQUESTED).
     */
    @Transactional
    public RequestDetailDto updateStructuredData(UUID requestId, UUID actingUserId, Map<String, Object> data) {
        AdministrativeRequest request = require(requestId);
        if (!request.getRequesterId().equals(actingUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seul le demandeur peut modifier cette demande");
        }
        if (request.getStatus() != RequestStatus.DRAFT && request.getStatus() != RequestStatus.CHANGES_REQUESTED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La demande ne peut être modifiée que si elle est en brouillon ou renvoyée pour modification");
        }
        request.setStructuredData(data != null ? data : new HashMap<>());
        requests.save(request);
        return getDetail(requestId, actingUserId, false);
    }

    /**
     * True if the employee already has a non-rejected leave for the pilgrimage (Hajj).
     * Per the Statut Général de la Fonction Publique (art. 41), this authorisation is
     * granted only ONCE in a civil servant's career, so a prior one blocks a new one.
     */
    @Transactional(readOnly = true)
    public boolean hasPriorHajjLeave(UUID requesterId) {
        return requests.findByRequesterIdOrderByCreatedAtDesc(requesterId).stream()
                .filter(r -> r.getType() == com.pfe.adminagent.request.domain.RequestType.LEAVE)
                .filter(r -> r.getStatus() != RequestStatus.REJECTED && r.getStatus() != RequestStatus.DRAFT)
                .anyMatch(r -> isHajjLeave(r.getStructuredData()));
    }

    private boolean isHajjLeave(Map<String, Object> data) {
        if (data == null) return false;
        Object t = data.get("leaveType");
        if (t == null) return false;
        String s = t.toString().toLowerCase();
        return s.contains("hajj") || s.contains("pèlerinage") || s.contains("pelerinage");
    }

    @Transactional(readOnly = true)
    public String referenceOf(UUID requestId) {
        return require(requestId).getReference();
    }

    private AdministrativeRequest require(UUID requestId) {
        return requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable : " + requestId));
    }

    private String nextReference() {
        return "REQ-" + Year.now().getValue() + "-" + String.format("%06d", requests.nextReferenceValue());
    }

    private void notifyApprovers(AdministrativeRequest request) {
        List<User> approvers = users.findByRoleInAndActiveIsTrue(List.of(Role.MANAGER));
        String title = "Nouvelle demande à examiner";
        String message = "La demande " + request.getReference() + " (" + request.getType().label()
                + ") a été soumise et attend votre décision.";
        for (User approver : approvers) {
            notifications.notify(approver.getId(), NotificationType.REQUEST_SUBMITTED, title, message, request.getId());
        }
    }

    private void notifyRequesterOfDecision(AdministrativeRequest request, DecisionRequest.Action action, String comment) {
        NotificationType type = switch (action) {
            case APPROVE -> NotificationType.REQUEST_APPROVED;
            case REJECT -> NotificationType.REQUEST_REJECTED;
            case REQUEST_CHANGES -> NotificationType.CHANGES_REQUESTED;
        };
        String base = "Votre demande " + request.getReference() + " a été "
                + switch (action) {
                    case APPROVE -> "approuvée.";
                    case REJECT -> "rejetée.";
                    case REQUEST_CHANGES -> "renvoyée pour modification.";
                };
        // Propagate the approver's comment to the requester so it is visible on mobile.
        String message = (comment != null && !comment.isBlank())
                ? base + " Commentaire du responsable : " + comment.trim()
                : base;
        notifications.notify(request.getRequesterId(), type, "Mise à jour de votre demande", message, request.getId());
    }

    private List<RequestSummaryDto> toSummaries(List<AdministrativeRequest> list) {
        Set<UUID> requesterIds = list.stream()
                .map(AdministrativeRequest::getRequesterId).collect(Collectors.toSet());
        Map<UUID, String> names = resolveNames(requesterIds);
        return list.stream().map(r -> {
            Optional<AiAnalysis> analysis = analyses.findByRequestId(r.getId());
            return RequestSummaryDto.of(r,
                    names.get(r.getRequesterId()),
                    analysis.map(AiAnalysis::getRecommendation).orElse(null),
                    analysis.map(AiAnalysis::getRiskLevel).orElse(null),
                    analysis.map(AiAnalysis::getConfidenceScore).orElse(null),
                    analysis.isPresent());
        }).toList();
    }

    private Map<UUID, String> resolveNames(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }
}
