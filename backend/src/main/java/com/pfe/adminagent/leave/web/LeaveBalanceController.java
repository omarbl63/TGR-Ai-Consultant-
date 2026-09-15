package com.pfe.adminagent.leave.web;

import com.pfe.adminagent.leave.LeaveBalanceService;
import com.pfe.adminagent.leave.dto.LeaveBalanceDto;
import com.pfe.adminagent.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leave-balances")
@Tag(name = "Leave balances", description = "Employee leave balances (solde de congés)")
@SecurityRequirement(name = "bearerAuth")
public class LeaveBalanceController {

    private final LeaveBalanceService service;

    public LeaveBalanceController(LeaveBalanceService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @Operation(summary = "Current user's leave balance for this year")
    public LeaveBalanceDto me(@AuthenticationPrincipal UserPrincipal principal) {
        return service.forUser(principal.getId(), LeaveBalanceService.currentYear());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "All employees' leave balances for this year")
    public List<LeaveBalanceDto> all() {
        return service.allForYear(LeaveBalanceService.currentYear());
    }
}
