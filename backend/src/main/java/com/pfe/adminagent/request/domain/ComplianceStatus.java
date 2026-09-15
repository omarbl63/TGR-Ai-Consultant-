package com.pfe.adminagent.request.domain;

/**
 * Result of validating a request against retrieved regulations (see REG-VAL-2024).
 */
public enum ComplianceStatus {
    COMPLIANT,
    COMPLIANT_WITH_RESERVATION,
    INCOMPLETE,
    NON_COMPLIANT
}
