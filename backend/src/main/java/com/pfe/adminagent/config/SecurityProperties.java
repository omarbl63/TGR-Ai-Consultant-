package com.pfe.adminagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Typed configuration for authentication / JWT, bound from {@code adminai.security.*}.
 */
@ConfigurationProperties(prefix = "adminai.security")
public class SecurityProperties {

    @NestedConfigurationProperty
    private final Jwt jwt = new Jwt();

    public Jwt getJwt() {
        return jwt;
    }

    public static class Jwt {
        /** Base64-encoded signing secret (>= 256 bits). */
        private String secret;
        /** Access-token lifetime in milliseconds. */
        private long accessExpirationMs = 3_600_000L;
        /** Refresh-token lifetime in milliseconds. */
        private long refreshExpirationMs = 604_800_000L;
        /** Token issuer claim. */
        private String issuer = "adminai";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessExpirationMs() {
            return accessExpirationMs;
        }

        public void setAccessExpirationMs(long accessExpirationMs) {
            this.accessExpirationMs = accessExpirationMs;
        }

        public long getRefreshExpirationMs() {
            return refreshExpirationMs;
        }

        public void setRefreshExpirationMs(long refreshExpirationMs) {
            this.refreshExpirationMs = refreshExpirationMs;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }
}
