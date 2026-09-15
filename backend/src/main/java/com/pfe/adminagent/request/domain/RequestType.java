package com.pfe.adminagent.request.domain;

/**
 * The administrative request types supported by the AI agent.
 */
public enum RequestType {
    LEAVE("Congé"),
    MISSION_ORDER("Ordre de mission"),
    EXPENSE("Remboursement de frais");

    private final String label;

    RequestType(String label) {
        this.label = label;
    }

    /** Human-readable French label (never expose the raw enum name to users). */
    public String label() {
        return label;
    }
}
