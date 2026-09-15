package com.pfe.adminagent.security;

import com.pfe.adminagent.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Issues and validates JWT access & refresh tokens (HMAC-SHA256).
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_NAME = "name";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final SecurityProperties.Jwt props;

    public JwtService(SecurityProperties securityProperties) {
        this.props = securityProperties.getJwt();
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.getSecret()));
    }

    public String generateAccessToken(UserPrincipal principal, String role, String fullName) {
        return buildToken(principal.getUsername(), TYPE_ACCESS, props.getAccessExpirationMs(),
                Map.of(CLAIM_ROLE, role, CLAIM_NAME, fullName, "uid", principal.getId().toString()));
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal.getUsername(), TYPE_REFRESH, props.getRefreshExpirationMs(), Map.of());
    }

    private String buildToken(String subject, String type, long ttlMs, Map<String, Object> extraClaims) {
        Date now = new Date();
        return Jwts.builder()
                .claims(extraClaims)
                .claim(CLAIM_TYPE, type)
                .subject(subject)
                .issuer(props.getIssuer())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(extractClaim(token, c -> c.get(CLAIM_TYPE, String.class)));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(extractClaim(token, c -> c.get(CLAIM_TYPE, String.class)));
    }

    public boolean isValid(String token, UserPrincipal principal) {
        return extractUsername(token).equalsIgnoreCase(principal.getUsername()) && !isExpired(token);
    }

    public long getAccessExpirationSeconds() {
        return props.getAccessExpirationMs() / 1000;
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
