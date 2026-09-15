package com.pfe.adminagent.request.domain;

/**
 * Timeline event on a request. Actor is null for system/AI-generated events.
 */
public enum RequestEventType {
    CREATED,
    SUBMITTED,
    AI_ANALYZED,
    APPROVED,
    REJECTED,
    CHANGES_REQUESTED,
    COMMENTED
}
