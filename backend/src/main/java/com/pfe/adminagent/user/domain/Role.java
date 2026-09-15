package com.pfe.adminagent.user.domain;

/**
 * Application roles. Employees create requests via the AI agent; managers
 * (responsables) approve them via the dashboard; admins manage the platform.
 */
public enum Role {
    EMPLOYEE,
    MANAGER,
    ADMIN
}
