package com.pfe.adminagent.user.dto;

import com.pfe.adminagent.user.domain.Role;
import com.pfe.adminagent.user.domain.User;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String fullName,
        Role role,
        String department,
        String jobTitle,
        boolean active
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getDepartment(),
                user.getJobTitle(),
                user.isActive());
    }
}
