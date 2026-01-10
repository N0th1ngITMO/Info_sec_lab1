package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {
    private String secretKey;
    private long expirationMs;
    private String issuer;

    public JwtProperties() {
        this.secretKey = "3x#K8pL$9qR2sV5yB7wE0zC4fT6hN1jM@";
        this.expirationMs = 86400000L;
        this.issuer = "secure-api";
    }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
