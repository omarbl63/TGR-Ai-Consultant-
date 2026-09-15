package com.pfe.adminagent.request.domain;

/**
 * Lifecycle of an administrative request.
 * DRAFT → SUBMITTED → UNDER_REVIEW → (APPROVED | REJECTED | CHANGES_REQUESTED).
 */
public enum RequestStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    CHANGES_REQUESTED
}
