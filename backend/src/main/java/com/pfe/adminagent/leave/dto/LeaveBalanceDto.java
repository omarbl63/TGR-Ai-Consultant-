package com.pfe.adminagent.leave.dto;

import java.util.UUID;

public record LeaveBalanceDto(
        UUID userId,
        String userName,
        String department,
        int year,
        double entitledDays,
        double usedDays,
        double remainingDays
) {
}
