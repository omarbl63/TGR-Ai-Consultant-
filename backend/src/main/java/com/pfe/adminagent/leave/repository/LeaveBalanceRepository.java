package com.pfe.adminagent.leave.repository;

import com.pfe.adminagent.leave.domain.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    Optional<LeaveBalance> findByUserIdAndYear(UUID userId, int year);

    List<LeaveBalance> findByYear(int year);

    boolean existsByUserIdAndYear(UUID userId, int year);
}
