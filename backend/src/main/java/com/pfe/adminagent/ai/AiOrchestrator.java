package com.pfe.adminagent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.adminagent.ai.dto.AskResponse;
import com.pfe.adminagent.ai.dto.ChatResponse;
import com.pfe.adminagent.ai.model.LlmIntent;
import com.pfe.adminagent.ai.prompt.Prompts;
import com.pfe.adminagent.conversation.ConversationService;
import com.pfe.adminagent.conversation.domain.MessageRole;
import com.pfe.adminagent.conversation.dto.ConversationDetailDto;
import com.pfe.adminagent.conversation.dto.MessageDto;
import com.pfe.adminagent.leave.LeaveBalanceService;
import com.pfe.adminagent.leave.dto.LeaveBalanceDto;
import com.pfe.adminagent.rag.DocumentParsingService;
import com.pfe.adminagent.request.domain.RequestAttachment;
import com.pfe.adminagent.request.domain.RequestType;
import com.pfe.adminagent.request.dto.CreateRequestRequest;
import com.pfe.adminagent.request.dto.RequestDetailDto;
import com.pfe.adminagent.user.domain.Role;
import com.pfe.adminagent.user.domain.User;
import com.pfe.adminagent.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The AI Orchestrator — the central coordinator. For each employee message it:
 * detects intent, and either answers an administrative question (RAG), asks for
 * missing information, or extracts a complete request, creates & submits it, and
 * runs the RAG-grounded compliance analysis. It never approves anything.
 */
@Service
public class AiOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestrator.class);

    private final ConversationService conversationService;
    private final QaService qaService;
    private final AnalysisService analysisService;
    private final com.pfe.adminagent.request.RequestService requestService;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final LeaveBalanceService leaveBalanceService;
    private final UserRepository userRepository;
    private final DocumentParsingService parsingService;

    public AiOrchestrator(ConversationService conversationService,
                          QaService qaService,
                          AnalysisService analysisService,
                          com.pfe.adminagent.request.RequestService requestService,
                          LlmService llmService,
                          ObjectMapper objectMapper,
                          LeaveBalanceService leaveBalanceService,
                          UserRepository userRepository,
                          DocumentParsingService parsingService) {
        this.conversationService = conversationService;
        this.qaService = qaService;
        this.analysisService = analysisService;
        this.requestService = requestService;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.leaveBalanceService = leaveBalanceService;
        this.userRepository = userRepository;
        this.parsingService = parsingService;
    }

    public ChatResponse chat(UUID userId, UUID conversationId, String message) {
        UUID convId = conversationId != null ? conversationId
                : conversationService.create(userId, deriveConversationTitle(message)).id();
        conversationService.postUserMessage(convId, userId, message);

        ConversationDetailDto detail = conversationService.getDetail(convId, userId);

        // If a draft is awaiting its supporting documents, this message finalizes it
        // (verifies the uploaded pieces and either submits or asks for correct ones).
        UUID pendingDraftId = pendingDraftId(detail);
        if (pendingDraftId != null) {
            ChatResponse fin = finalizeDraft(userId, convId, pendingDraftId);
            Map<String, Object> md = new HashMap<>();
            if (fin.awaitingDocuments() && fin.createdRequestId() != null) {
                // Still a draft: pieces were missing or incorrect — keep the doc phase alive.
                md.put("draftRequestId", fin.createdRequestId().toString());
            } else if (fin.createdRequestId() != null) {
                md.put("requestId", fin.createdRequestId().toString());
                md.put("reference", fin.requestReference());
            }
            conversationService.addAssistantMessage(convId, fin.reply(), md);
            return fin;
        }

        // Top-level routing: "soumettre une demande" vs "obtenir une information".
        ChatResponse routed = routeTopLevel(convId, message);
        if (routed != null) {
            conversationService.addAssistantMessage(convId, routed.reply(),
                    Map.of("intent", routed.intent().name()));
            return routed;
        }

        String transcript = buildTranscript(detail.messages());
        // A tapped quick-reply (request type) routes deterministically; otherwise detect.
        IntentType intent = matchRequestTypeChoice(message);
        if (intent == null) {
            intent = detectIntent(transcript, message);
        }
        log.info("Chat intent detected: {}", intent);

        // Did we already ask the employee to complete this request on a previous turn?
        boolean alreadyAsked = detail.messages().stream().anyMatch(m ->
                m.role() == MessageRole.ASSISTANT && m.metadata() != null
                        && Boolean.TRUE.equals(m.metadata().get("clarify")));

        ChatResponse response = switch (intent) {
            case SMALL_TALK -> smallTalk(convId, message);
            case ADMIN_QUESTION -> answerQuestion(convId, message);
            case BALANCE_QUERY -> handleBalance(userId, convId, message);
            case LEAVE, MISSION_ORDER, EXPENSE -> handleRequest(userId, convId, intent, transcript, alreadyAsked);
        };

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("intent", intent.name());
        if (response.awaitingDocuments() && response.createdRequestId() != null) {
            // Draft created, waiting for the employee to attach documents and send.
            metadata.put("draftRequestId", response.createdRequestId().toString());
        } else if (response.createdRequestId() != null) {
            metadata.put("requestId", response.createdRequestId().toString());
            metadata.put("reference", response.requestReference());
        } else if (isRequestIntent(intent) && !response.missingInformation().isEmpty()) {
            // This reply is a request for missing info; remember it so we don't loop forever.
            metadata.put("clarify", true);
        }
        conversationService.addAssistantMessage(convId, response.reply(), metadata);
        return response;
    }

    /** The id of a still-DRAFT request awaiting its documents, or null. */
    private UUID pendingDraftId(ConversationDetailDto detail) {
        UUID lastDraft = null;
        for (MessageDto m : detail.messages()) {
            if (m.role() == MessageRole.ASSISTANT && m.metadata() != null) {
                Object v = m.metadata().get("draftRequestId");
                if (v != null) {
                    try {
                        lastDraft = UUID.fromString(v.toString());
                    } catch (IllegalArgumentException ignored) {
                        // not a UUID
                    }
                }
            }
        }
        return (lastDraft != null && requestService.isDraft(lastDraft)) ? lastDraft : null;
    }

    private ChatResponse finalizeDraft(UUID userId, UUID convId, UUID draftId) {
        RequestType type = requestService.typeOf(draftId);
        String reference = requestService.referenceOf(draftId);

        // The AI verifies the CONTENT of the uploaded pieces. If any is off-topic or
        // incorrect, the request is NOT sent; the employee is told what to fix.
        List<String> invalid = verifyProvidedDocuments(draftId, type);
        if (!invalid.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("⚠️ Je ne peux pas transmettre votre demande ").append(reference)
                    .append(" en l'état : le contenu de certaines pièces jointes ne correspond pas à ce qui est attendu :\n");
            for (String b : invalid) {
                sb.append("• ").append(b).append("\n");
            }
            sb.append("\nMerci de joindre des pièces correctes et lisibles, puis d'appuyer sur « Envoyer la demande ».");
            return new ChatResponse(convId, sb.toString(), IntentType.valueOf(type.name()), draftId, reference,
                    null, List.of(), List.of(), List.of("Envoyer la demande", "Je n'ai pas ces pièces"),
                    true, RequestTypeSpec.requiredDocuments(type));
        }

        RequestDetailDto created = requestService.submit(draftId, userId);
        try {
            analysisService.analyze(draftId);
        } catch (Exception e) {
            log.warn("Analysis failed for {}: {}", created.reference(), e.getMessage());
        }
        int n = requestService.attachmentCount(draftId);
        String pieces = n == 0 ? "sans pièce jointe"
                : (n == 1 ? "avec 1 pièce jointe" : "avec " + n + " pièces jointes");
        String reply = "C'est fait ✅ Votre demande " + created.reference()
                + " a bien été transmise pour validation (" + pieces + "). Souhaitez-vous autre chose ?";
        IntentType intent = IntentType.valueOf(created.type().name());
        return new ChatResponse(convId, reply, intent, created.id(), created.reference(),
                null, List.of(), List.of(), MENU_CHOICES, false, List.of());
    }

    /**
     * Reads the text of every uploaded piece and asks the LLM whether each matches the
     * expected document type. Returns human-readable descriptions of the invalid pieces
     * (empty if all uploaded pieces are plausible). Fails open on error (never blocks
     * because of a technical failure of the verifier).
     */
    @SuppressWarnings("unchecked")
    private List<String> verifyProvidedDocuments(UUID requestId, RequestType type) {
        // Keep only the MOST RECENT attachment per document label (a re-upload supersedes
        // an earlier, possibly incorrect, file for the same piece).
        java.util.LinkedHashMap<String, RequestAttachment> byLabel = new java.util.LinkedHashMap<>();
        for (RequestAttachment a : requestService.attachmentsOf(requestId)) {
            if (a.getDocumentLabel() != null && !a.getDocumentLabel().isBlank()) {
                byLabel.put(a.getDocumentLabel(), a);
            }
        }
        List<RequestAttachment> labeled = new ArrayList<>(byLabel.values());
        if (labeled.isEmpty()) {
            return List.of();
        }
        StringBuilder block = new StringBuilder();
        for (RequestAttachment a : labeled) {
            String text;
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(a.getStoragePath()));
                text = parsingService.extractText(bytes, a.getFilename());
            } catch (Exception e) {
                text = "";
            }
            if (text == null) {
                text = "";
            }
            if (text.length() > 1500) {
                text = text.substring(0, 1500);
            }
            block.append("- Libellé: ").append(a.getDocumentLabel())
                    .append("\n  Fichier: ").append(a.getFilename())
                    .append("\n  Texte extrait: ").append(text.isBlank() ? "(vide / illisible)" : text)
                    .append("\n\n");
        }
        String expected = RequestTypeSpec.requiredDocuments(type).stream()
                .map(d -> "- " + d).collect(java.util.stream.Collectors.joining("\n"));
        try {
            Map<String, Object> res = llmService.completeJson(Prompts.DOC_VERIFY_SYSTEM,
                    Prompts.docVerifyUser(expected, block.toString().trim()), Map.class);
            Object inv = res.get("invalid");
            List<String> bad = new ArrayList<>();
            if (inv instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> mm) {
                        Object label = mm.get("label");
                        Object reason = mm.get("reason");
                        String entry = (label != null ? label.toString() : "Pièce jointe")
                                + (reason != null ? " — " + reason : "");
                        bad.add(entry);
                    } else if (o != null) {
                        bad.add(o.toString());
                    }
                }
            }
            return bad;
        } catch (Exception e) {
            log.warn("Document verification failed, allowing submission: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isRequestIntent(IntentType intent) {
        return intent == IntentType.LEAVE || intent == IntentType.MISSION_ORDER || intent == IntentType.EXPENSE;
    }

    // ---------------------------------------------------------------- branches

    /** Quick-reply options offered to the employee. */
    private static final List<String> MENU_CHOICES = List.of(
            "Demande de congé", "Ordre de mission", "Remboursement de frais", "Mon solde de congés");

    private static final List<String> ROUTE_CHOICES =
            List.of("Soumettre une demande", "Obtenir une information");
    private static final List<String> REQUEST_TYPE_CHOICES =
            List.of("Demande de congé", "Ordre de mission", "Remboursement de frais");

    private ChatResponse smallTalk(UUID convId, String message) {
        String reply = "Bonjour ! Je suis l'assistant administratif de la Trésorerie Générale du Royaume. "
                + "Souhaitez-vous soumettre une demande ou obtenir une information ?";
        return new ChatResponse(convId, reply, IntentType.SMALL_TALK, null, null, null,
                List.of(), List.of(), ROUTE_CHOICES, false, List.of());
    }

    /** Handles the two top-level routing choices; returns null if the message is neither. */
    private ChatResponse routeTopLevel(UUID convId, String message) {
        String m = message.trim().toLowerCase();
        if (m.equals("soumettre une demande")) {
            String reply = "Très bien. Quel type de demande souhaitez-vous soumettre ?";
            return new ChatResponse(convId, reply, IntentType.SMALL_TALK, null, null, null,
                    List.of(), List.of(), REQUEST_TYPE_CHOICES, false, List.of());
        }
        if (m.startsWith("obtenir une information") || m.equals("poser une question")
                || m.equals("obtenir une information / poser une question")) {
            String reply = "Bien sûr. Quelle information souhaitez-vous ? "
                    + "Posez votre question (congés, ordres de mission, frais, solde de congés, procédures…) "
                    + "et je vous réponds en m'appuyant sur la réglementation en vigueur.";
            return new ChatResponse(convId, reply, IntentType.ADMIN_QUESTION, null, null, null,
                    List.of(), List.of(), List.of(), false, List.of());
        }
        return null;
    }

    private ChatResponse handleBalance(UUID userId, UUID convId, String message) {
        User user = userRepository.findById(userId).orElse(null);
        boolean approver = user != null
                && (user.getRole() == Role.MANAGER || user.getRole() == Role.ADMIN);
        int year = LeaveBalanceService.currentYear();

        StringBuilder data = new StringBuilder();
        if (approver) {
            // Approvers can see everyone's balance.
            List<LeaveBalanceDto> all = leaveBalanceService.allForYear(year);
            for (LeaveBalanceDto b : all) {
                data.append(formatBalance(b)).append('\n');
            }
            if (all.isEmpty()) {
                data.append("(aucun solde enregistré)");
            }
        } else {
            LeaveBalanceDto mine = leaveBalanceService.forUser(userId, year);
            data.append(formatBalance(mine));
        }

        String reply;
        try {
            reply = llmService.complete(Prompts.BALANCE_SYSTEM, Prompts.balanceUser(message, data.toString().trim()));
        } catch (Exception e) {
            reply = "Votre solde de congés : " + (user != null ? "" : "")
                    + Math.round(leaveBalanceService.forUser(userId, year).remainingDays()) + " jours restants.";
        }
        return new ChatResponse(convId, reply, IntentType.BALANCE_QUERY, null, null, null, List.of(), List.of(), List.of(), false, List.of());
    }

    private String formatBalance(LeaveBalanceDto b) {
        return String.format("- %s%s : %.0f jours restants (sur %.0f, %.0f pris) — année %d",
                b.userName() != null ? b.userName() : "Employé",
                b.department() != null ? " (" + b.department() + ")" : "",
                b.remainingDays(), b.entitledDays(), b.usedDays(), b.year());
    }

    private ChatResponse answerQuestion(UUID convId, String message) {
        AskResponse ask = qaService.ask(message, null);
        return new ChatResponse(convId, ask.answer(), IntentType.ADMIN_QUESTION,
                null, null, null, List.of(), ask.citations(), List.of(), false, List.of());
    }

    private ChatResponse handleRequest(UUID userId, UUID convId, IntentType intent, String transcript,
                                       boolean alreadyAsked) {
        RequestType type = RequestType.valueOf(intent.name());
        Map<String, Object> extracted = extractEntities(type, transcript);

        // Refuse dates that are in the past (based on the local date) — ask for a valid one.
        String pastWarning = pastDateWarning(type, extracted);
        if (pastWarning != null) {
            return new ChatResponse(convId, pastWarning, intent, null, null, null,
                    List.of(), List.of(), List.of(), false, List.of());
        }

        // Business rule (SGFP art. 41): the pilgrimage (Hajj) leave is granted only once
        // in a civil servant's career.
        if (type == RequestType.LEAVE && isHajjType(extracted.get("leaveType"))
                && requestService.hasPriorHajjLeave(userId)) {
            String reply = "D'après vos demandes, vous avez déjà bénéficié d'un congé pour le pèlerinage (Hajj). "
                    + "Conformément au Statut Général de la Fonction Publique (art. 41), cette autorisation n'est "
                    + "accordée qu'une seule fois au cours de la carrière ; je ne peux donc pas créer une nouvelle "
                    + "demande de ce type. Souhaitez-vous formuler une autre demande ?";
            return new ChatResponse(convId, reply, intent, null, null, null,
                    List.of(), List.of(), ROUTE_CHOICES, false, List.of());
        }

        List<RequestTypeSpec.Field> required = RequestTypeSpec.requiredFields(type);
        List<String> missingLabels = new ArrayList<>();
        for (RequestTypeSpec.Field f : required) {
            if (isBlank(extracted.get(f.key()))) {
                missingLabels.add(f.label());
            }
        }

        // Ask the employee once for everything that's missing. If they've already been
        // asked (and still didn't provide it, or said they don't have it), proceed and
        // submit the request with whatever we have.
        if (!missingLabels.isEmpty() && !alreadyAsked) {
            String reply = llmService.complete(Prompts.CLARIFICATION_SYSTEM,
                    Prompts.clarificationUser(type, missingLabels, toJson(extracted)));
            return new ChatResponse(convId, reply, intent, null, null, null, missingLabels, List.of(), List.of(), false, List.of());
        }

        RequestDetailDto created = requestService.create(userId,
                new CreateRequestRequest(type, deriveRequestTitle(type, extracted), extracted, convId));

        // If this request type needs supporting documents, keep it as a DRAFT and invite the
        // employee to attach them (they can skip any they don't have) before sending.
        List<String> documents = RequestTypeSpec.requiredDocuments(type);
        if (!documents.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Votre demande ").append(created.reference()).append(" est prête. ")
                    .append("Merci de joindre les pièces justificatives ci-dessous :\n");
            for (String d : documents) {
                sb.append("• ").append(d).append("\n");
            }
            sb.append("\nJoignez les pièces que vous possédez, puis appuyez sur « Envoyer la demande ». ")
                    .append("Je vérifierai leur contenu avant de transmettre la demande.");
            return new ChatResponse(convId, sb.toString(), intent, created.id(), created.reference(),
                    null, List.of(), List.of(), List.of("Envoyer la demande", "Je n'ai pas ces pièces"), true, documents);
        }

        // No supporting documents required — submit immediately.
        requestService.submit(created.id(), userId);
        try {
            analysisService.analyze(created.id());
        } catch (Exception e) {
            log.warn("Analysis failed for {}: {}", created.reference(), e.getMessage());
        }
        String reply = "C'est fait ✅ Votre demande " + created.reference()
                + " a bien été créée et transmise pour validation. Souhaitez-vous autre chose ?";
        return new ChatResponse(convId, reply, intent, created.id(), created.reference(),
                null, List.of(), List.of(), MENU_CHOICES, false, List.of());
    }

    private static final java.time.ZoneId ZONE = java.time.ZoneId.of("Africa/Casablanca");

    /** Returns a warning if a relevant date is in the past, else null. */
    private String pastDateWarning(RequestType type, Map<String, Object> data) {
        if (type == RequestType.EXPENSE || data == null) {
            return null; // expenses concern past spending
        }
        java.time.LocalDate today = java.time.LocalDate.now(ZONE);
        List<String> keys = type == RequestType.LEAVE
                ? List.of("startDate", "endDate")
                : List.of("departureDate", "returnDate");
        for (String k : keys) {
            java.time.LocalDate d = tryParseDate(data.get(k));
            if (d != null && d.isBefore(today)) {
                return "La date indiquée (" + d + ") est déjà passée — nous sommes le " + today
                        + ". Merci d'indiquer une date à venir.";
            }
        }
        return null;
    }

    private java.time.LocalDate tryParseDate(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(v.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ helpers

    private boolean isHajjType(Object leaveType) {
        if (leaveType == null) return false;
        String s = leaveType.toString().toLowerCase();
        return s.contains("hajj") || s.contains("pèlerinage") || s.contains("pelerinage");
    }

    /** Maps a tapped request-type quick-reply to its intent, or null for free text. */
    private IntentType matchRequestTypeChoice(String message) {
        return switch (message.trim().toLowerCase()) {
            case "demande de congé", "demande de conge" -> IntentType.LEAVE;
            case "ordre de mission" -> IntentType.MISSION_ORDER;
            case "remboursement de frais" -> IntentType.EXPENSE;
            case "mon solde de congés", "mon solde de conges" -> IntentType.BALANCE_QUERY;
            default -> null;
        };
    }

    private IntentType detectIntent(String transcript, String message) {
        try {
            LlmIntent li = llmService.completeJson(Prompts.INTENT_SYSTEM,
                    Prompts.intentUser(transcript, message), LlmIntent.class);
            IntentType t = Enums.parse(IntentType.class, li.intent());
            return t != null ? t : IntentType.ADMIN_QUESTION;
        } catch (Exception e) {
            log.warn("Intent detection failed, defaulting to ADMIN_QUESTION: {}", e.getMessage());
            return IntentType.ADMIN_QUESTION;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractEntities(RequestType type, String transcript) {
        try {
            Map<String, Object> m = llmService.completeJson(
                    Prompts.extractionSystem(type), Prompts.extractionUser(type, transcript), Map.class);
            return m != null ? m : new HashMap<>();
        } catch (Exception e) {
            log.warn("Entity extraction failed: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String buildTranscript(List<MessageDto> messages) {
        StringBuilder sb = new StringBuilder();
        for (MessageDto m : messages) {
            String who = switch (m.role()) {
                case USER -> "Employé";
                case ASSISTANT -> "Assistant";
                case SYSTEM -> "Système";
            };
            sb.append(who).append(": ").append(m.content()).append("\n");
        }
        return sb.toString().trim();
    }

    private String deriveConversationTitle(String message) {
        String t = message.strip();
        return t.length() > 60 ? t.substring(0, 60) + "…" : t;
    }

    private String deriveRequestTitle(RequestType type, Map<String, Object> data) {
        return switch (type) {
            case LEAVE -> "Demande de congé";
            case MISSION_ORDER -> "Ordre de mission"
                    + (data.get("destination") != null ? " - " + data.get("destination") : "");
            case EXPENSE -> "Remboursement de frais";
        };
    }

    private boolean isBlank(Object v) {
        return v == null || (v instanceof String s && s.isBlank());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
