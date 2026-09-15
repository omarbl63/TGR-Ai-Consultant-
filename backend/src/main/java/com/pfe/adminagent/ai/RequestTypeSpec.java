package com.pfe.adminagent.ai;

import com.pfe.adminagent.request.domain.RequestType;

import java.util.List;
import java.util.Map;

/**
 * Per-request-type metadata used by the orchestrator: the human label, the
 * required structured fields (to detect missing information), and keywords that
 * steer RAG retrieval toward the relevant regulations.
 */
public final class RequestTypeSpec {

    public record Field(String key, String label) {
    }

    private static final Map<RequestType, String> LABELS = Map.of(
            RequestType.LEAVE, "demande de congé",
            RequestType.MISSION_ORDER, "ordre de mission",
            RequestType.EXPENSE, "remboursement de frais");

    private static final Map<RequestType, List<Field>> REQUIRED = Map.of(
            RequestType.LEAVE, List.of(
                    new Field("leaveType", "type de congé"),
                    new Field("startDate", "date de début"),
                    new Field("endDate", "date de fin")),
            RequestType.MISSION_ORDER, List.of(
                    new Field("destination", "destination"),
                    new Field("purpose", "objet de la mission"),
                    new Field("departureDate", "date de départ"),
                    new Field("returnDate", "date de retour")),
            RequestType.EXPENSE, List.of(
                    new Field("amount", "montant"),
                    new Field("category", "catégorie de dépense"),
                    new Field("description", "description")));

    private static final Map<RequestType, String> RETRIEVAL_HINTS = Map.of(
            RequestType.LEAVE, "politique de congés solde délai de prévenance validation jours ouvrables",
            RequestType.MISSION_ORDER, "ordre de mission délai indemnité transport validation pièces justificatives",
            RequestType.EXPENSE, "remboursement de frais plafonds pièces justificatives seuils de validation budget");

    /**
     * Supporting documents the employee is invited to attach before sending.
     * Note: documents that merely encode a SUPERIOR'S AUTHORIZATION (visa hiérarchique,
     * signature de l'ordonnateur, validation du Directeur Général…) are intentionally
     * excluded — that approval is handled by the responsible in the dashboard, so
     * requiring them upfront would duplicate the workflow. Only genuine supporting
     * pieces the agent actually holds are requested.
     */
    private static final Map<RequestType, List<String>> REQUIRED_DOCUMENTS = Map.of(
            RequestType.LEAVE, List.of(
                    "Formulaire de demande de congé (correctement renseigné et signé par vous)",
                    "Certificat médical (uniquement en cas de congé de maladie)",
                    "Justificatif de l'événement (mariage, naissance, décès…) le cas échéant"),
            RequestType.MISSION_ORDER, List.of(
                    "Demande de mission établie par vos soins (objet, destination, dates)",
                    "Justificatif de la mission le cas échéant (convocation, invitation, ordre de service)"),
            RequestType.EXPENSE, List.of(
                    "État de frais (complété et signé par vous)",
                    "Ordre de mission correspondant",
                    "Billets de transport ou cartes d'embarquement",
                    "Factures d'hébergement (si l'indemnité forfaitaire n'est pas appliquée)"));

    private RequestTypeSpec() {
    }

    public static List<String> requiredDocuments(RequestType type) {
        return REQUIRED_DOCUMENTS.getOrDefault(type, List.of());
    }

    public static String label(RequestType type) {
        return LABELS.getOrDefault(type, type.name());
    }

    public static List<Field> requiredFields(RequestType type) {
        return REQUIRED.getOrDefault(type, List.of());
    }

    public static String retrievalHint(RequestType type) {
        return RETRIEVAL_HINTS.getOrDefault(type, "");
    }
}
