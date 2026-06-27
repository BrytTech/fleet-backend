package org.fleet.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;

@Service
public class JwtService {

    private String getSecret() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null) {
            throw new IllegalStateException("JWT_SECRET environment variable not set");
        }
        return secret;
    }

    public Key getSignedKey(){
        return Keys.hmacShaKeyFor(getSecret().getBytes());
    }

    public String generateToken(String email, String role){
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .addClaims(claims)
                .signWith(getSignedKey())
                .compact();
    }

    public Claims verifySignatureAndExtractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSignedKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
