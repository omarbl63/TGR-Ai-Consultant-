package com.pfe.adminagent.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A platform user. Persisted in the {@code users} table (see Flyway V1).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Column(length = 120)
    private String department;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // JPA
    }

    private User(Builder b) {
        this.id = b.id != null ? b.id : UUID.randomUUID();
        this.email = b.email;
        this.passwordHash = b.passwordHash;
        this.fullName = b.fullName;
        this.role = b.role;
        this.department = b.department;
        this.jobTitle = b.jobTitle;
        this.active = b.active;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    // --- getters ---
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public String getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // --- mutators for the few mutable fields ---
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setDepartment(String department) { this.department = department; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public void setActive(boolean active) { this.active = active; }
    public void setRole(Role role) { this.role = role; }

    public static final class Builder {
        private UUID id;
        private String email;
        private String passwordHash;
        private String fullName;
        private Role role = Role.EMPLOYEE;
        private String department;
        private String jobTitle;
        private boolean active = true;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder role(Role role) { this.role = role; return this; }
        public Builder department(String department) { this.department = department; return this; }
        public Builder jobTitle(String jobTitle) { this.jobTitle = jobTitle; return this; }
        public Builder active(boolean active) { this.active = active; return this; }

        public User build() {
            return new User(this);
        }
    }
}
