package com.pfe.adminagent.request.dto;

/**
 * KPI counters for the approver dashboard.
 */
public record RequestStatsDto(
        long pending,
        long approved,
        long rejected,
        long changesRequested,
        long total
) {
}
