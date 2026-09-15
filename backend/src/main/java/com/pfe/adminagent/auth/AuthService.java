package com.pfe.adminagent.auth;

import com.pfe.adminagent.auth.dto.AuthResponse;
import com.pfe.adminagent.auth.dto.LoginRequest;
import com.pfe.adminagent.auth.dto.RefreshRequest;
import com.pfe.adminagent.auth.dto.RegisterRequest;
import com.pfe.adminagent.common.exception.ApiException;
import com.pfe.adminagent.common.exception.ConflictException;
import com.pfe.adminagent.leave.LeaveBalanceService;
import com.pfe.adminagent.security.JwtService;
import com.pfe.adminagent.security.UserPrincipal;
import com.pfe.adminagent.user.domain.Role;
import com.pfe.adminagent.user.domain.User;
import com.pfe.adminagent.user.repository.UserRepository;
import com.pfe.adminagent.user.dto.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles registration, login and token refresh.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LeaveBalanceService leaveBalanceService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       LeaveBalanceService leaveBalanceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.leaveBalanceService = leaveBalanceService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse e-mail");
        }
        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role() != null ? request.role() : Role.EMPLOYEE)
                .department(request.department())
                .jobTitle(request.jobTitle())
                .active(true)
                .build();
        userRepository.save(user);
        leaveBalanceService.ensureExists(user.getId(), 0);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Compte introuvable"));
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();
        if (!jwtService.isRefreshToken(token)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Jeton de rafraîchissement invalide");
        }
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Compte introuvable"));
        UserPrincipal principal = new UserPrincipal(user);
        if (!jwtService.isValid(token, principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Jeton de rafraîchissement expiré");
        }
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal, user.getRole().name(), user.getFullName());
        String refreshToken = jwtService.generateRefreshToken(principal);
        return AuthResponse.of(accessToken, refreshToken, jwtService.getAccessExpirationSeconds(),
                UserDto.from(user));
    }
}
