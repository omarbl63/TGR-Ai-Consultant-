package com.pfe.adminagent.leave.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.util.UUID;

/**
 * An employee's annual leave balance (solde de congés) for a given year.
 * The AI reads this to answer "combien de jours de congé reste-t-il ?".
 */
@Entity
@Table(name = "leave_balances")
public class LeaveBalance {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int year;

    @Column(name = "entitled_days", nullable = false)
    private double entitledDays;

    @Column(name = "used_days", nullable = false)
    private double usedDays;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LeaveBalance() {
    }

    public LeaveBalance(UUID userId, int year, double entitledDays, double usedDays) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.year = year;
        this.entitledDays = entitledDays;
        this.usedDays = usedDays;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    @Transient
    public double getRemainingDays() {
        return Math.max(0, entitledDays - usedDays);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public int getYear() { return year; }
    public double getEntitledDays() { return entitledDays; }
    public void setEntitledDays(double entitledDays) { this.entitledDays = entitledDays; }
    public double getUsedDays() { return usedDays; }
    public void setUsedDays(double usedDays) { this.usedDays = usedDays; }
    public Instant getUpdatedAt() { return updatedAt; }
}
