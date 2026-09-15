package com.pfe.adminagent.user.dto;

import com.pfe.adminagent.user.domain.Role;

import java.util.UUID;

/**
 * Employee record for the administrator's directory: identity + leave balance.
 */
public record EmployeeOverviewDto(
        UUID id,
        String fullName,
        String email,
        Role role,
        String department,
        String jobTitle,
        double entitledLeave,
        double usedLeave,
        double remainingLeave
) {
}
