package com.pfe.adminagent.user.web;

import com.pfe.adminagent.common.exception.ResourceNotFoundException;
import com.pfe.adminagent.leave.LeaveBalanceService;
import com.pfe.adminagent.leave.dto.LeaveBalanceDto;
import com.pfe.adminagent.security.UserPrincipal;
import com.pfe.adminagent.user.domain.Role;
import com.pfe.adminagent.user.domain.User;
import com.pfe.adminagent.user.dto.EmployeeOverviewDto;
import com.pfe.adminagent.user.dto.UserDto;
import com.pfe.adminagent.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile & directory endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;

    public UserController(UserRepository userRepository, LeaveBalanceService leaveBalanceService) {
        this.userRepository = userRepository;
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .map(UserDto::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Directory of employees with their leave balance (dashboard users excluded)")
    public List<EmployeeOverviewDto> directory() {
        int year = LeaveBalanceService.currentYear();
        Map<UUID, LeaveBalanceDto> balances = leaveBalanceService.allForYear(year).stream()
                .collect(Collectors.toMap(LeaveBalanceDto::userId, b -> b));

        return userRepository.findAll().stream()
                // Only real employees — managers/admins are dashboard users, not listed here.
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .sorted(Comparator.comparing(User::getFullName, Comparator.nullsLast(String::compareTo)))
                .map(u -> toOverview(u, balances.get(u.getId())))
                .toList();
    }

    private EmployeeOverviewDto toOverview(User u, LeaveBalanceDto b) {
        double entitled = b != null ? b.entitledDays() : LeaveBalanceService.DEFAULT_ENTITLED_DAYS;
        double used = b != null ? b.usedDays() : 0;
        double remaining = b != null ? b.remainingDays() : entitled;
        return new EmployeeOverviewDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                u.getDepartment(), u.getJobTitle(), entitled, used, remaining);
    }
}
