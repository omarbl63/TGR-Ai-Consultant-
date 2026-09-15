package com.pfe.adminagent.request.domain;

/**
 * The AI's non-binding recommendation to the human approver.
 */
public enum Recommendation {
    APPROVE("Approbation"),
    REJECT("Rejet"),
    REQUEST_CHANGES("Modifications à demander");

    private final String label;

    Recommendation(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
