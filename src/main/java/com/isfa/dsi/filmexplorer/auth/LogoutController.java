package com.isfa.dsi.filmexplorer.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.isfa.dsi.filmexplorer.services.TokenBlacklistService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class LogoutController {

    @Value("${spring.security.jwt.secret-key}")
    private String secretKey;

    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("🚪 Logout request received");

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(new HashMap<String, Object>() {{
                    put("success", false);
                    put("error", "Missing Authorization header");
                }});
            }

            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Long expirationTime = claims.getExpiration().getTime();
            tokenBlacklistService.blacklistToken(token, expirationTime);

            String userEmail = claims.getSubject();
            log.info("✅ User {} logged out and token blacklisted", userEmail);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "✅ Logged out successfully");
                put("info", "Your token has been invalidated and cannot be reused");
            }});

        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(401).body(new HashMap<String, Object>() {{
                put("success", false);
                put("error", "Invalid token");
            }});
        }
    }
}