package com.pfe.adminagent.auth.dto;

import com.pfe.adminagent.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères") String password,
        @NotBlank @Size(max = 160) String fullName,
        Role role,
        @Size(max = 120) String department,
        @Size(max = 120) String jobTitle
) {
}
