package com.pfe.adminagent.leave;

import com.pfe.adminagent.leave.domain.LeaveBalance;
import com.pfe.adminagent.leave.dto.LeaveBalanceDto;
import com.pfe.adminagent.leave.repository.LeaveBalanceRepository;
import com.pfe.adminagent.user.domain.User;
import com.pfe.adminagent.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads and maintains employee leave balances. Consumed by the AI orchestrator
 * (to answer balance questions) and the mobile profile screen.
 */
@Service
public class LeaveBalanceService {

    /** Default annual entitlement per the leave policy (POL-CONG-2024, Art. 3). */
    public static final double DEFAULT_ENTITLED_DAYS = 22;

    private final LeaveBalanceRepository repository;
    private final UserRepository userRepository;

    public LeaveBalanceService(LeaveBalanceRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public static int currentYear() {
        return Year.now().getValue();
    }

    @Transactional
    public LeaveBalance getOrCreate(UUID userId, int year) {
        return repository.findByUserIdAndYear(userId, year)
                .orElseGet(() -> repository.save(new LeaveBalance(userId, year, DEFAULT_ENTITLED_DAYS, 0)));
    }

    @Transactional
    public void ensureExists(UUID userId, double usedDays) {
        int year = currentYear();
        if (!repository.existsByUserIdAndYear(userId, year)) {
            repository.save(new LeaveBalance(userId, year, DEFAULT_ENTITLED_DAYS, usedDays));
        }
    }

    @Transactional(readOnly = true)
    public LeaveBalanceDto forUser(UUID userId, int year) {
        LeaveBalance b = repository.findByUserIdAndYear(userId, year)
                .orElse(new LeaveBalance(userId, year, DEFAULT_ENTITLED_DAYS, 0));
        User u = userRepository.findById(userId).orElse(null);
        return toDto(b, u);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> allForYear(int year) {
        List<LeaveBalance> balances = repository.findByYear(year);
        Map<UUID, User> users = userRepository.findAllById(
                        balances.stream().map(LeaveBalance::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        return balances.stream().map(b -> toDto(b, users.get(b.getUserId()))).toList();
    }

    private LeaveBalanceDto toDto(LeaveBalance b, User u) {
        return new LeaveBalanceDto(
                b.getUserId(),
                u != null ? u.getFullName() : null,
                u != null ? u.getDepartment() : null,
                b.getYear(),
                b.getEntitledDays(),
                b.getUsedDays(),
                b.getRemainingDays());
    }
}
