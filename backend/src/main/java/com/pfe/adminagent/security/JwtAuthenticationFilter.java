package com.pfe.adminagent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the Bearer access token on each request and populates the
 * {@link SecurityContextHolder}. Invalid/expired tokens are simply ignored,
 * leaving the request anonymous (entry point handles the 401).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length());
        try {
            if (jwtService.isAccessToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String email = jwtService.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (userDetails instanceof UserPrincipal principal && jwtService.isValid(token, principal)) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            // Malformed/expired token → stay anonymous.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
