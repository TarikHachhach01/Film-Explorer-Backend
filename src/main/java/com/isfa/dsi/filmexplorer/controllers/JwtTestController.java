package com.isfa.dsi.filmexplorer.controllers;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jwt-test")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class JwtTestController {

    @Value("${spring.security.jwt.secret-key}")
    private String secretKey;

    /**
     *  ENDPOINT VULNÉRABLE - Accepte alg=none
     */
    @PostMapping("/vulnerable")
    public ResponseEntity<?> testVulnerable(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        try {
            String[] parts = token.split("\\.");

            // Allow 2 or 3 parts (3 is normal, 2 is alg=none with empty signature)
            if (parts.length < 2 || parts.length > 3) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("success", false);
                    put("vulnerable", false);
                    put("error", "Invalid token format");
                }});
            }


            String payload = parts[1];

            int padding = 4 - (payload.length() % 4);
            if (padding != 4) {
                payload += "=".repeat(padding);
            }

            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(payload));
            java.util.Map<String, Object> payloadMap = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payloadJson, java.util.Map.class);

            Object roleObj = payloadMap.get("role");
            String role = (roleObj != null) ? roleObj.toString() : "USER";

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("vulnerable", true);
                put("message", " TOKEN ACCEPTED - PRIVILEGE ESCALATION SUCCESSFUL!");
                put("role", role);
                put("algorithm", "none");
            }});

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("vulnerable", false);
                put("error", e.getMessage());
            }});
        }
    }
    /**
     * ENDPOINT SÉCURISÉ - Rejette alg=none
     */
    @PostMapping("/secure")
    public ResponseEntity<?> testSecure(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            // BON : Vérifie que l'algorithme est HS256
            // En essayant de parser avec la clé, on rejette automatiquement alg=none
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Si le token a alg=none, la signature sera vide et on aura une exception
            // Sinon, c'est un token valide avec HS256

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Token valide (PROTÉGÉ)");
                put("role", claims.get("role"));
            }});

        } catch (SignatureException e) {
            // Signature exception = token a alg=none ou signature invalide
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", " REJETÉ (PROTÉGÉ!)");
                put("reason", "InvalidSignatureException");
                put("error", "Signature invalide ou algorithme non autorisé");
                put("protection", "Endpoint sécurisé - alg=none rejeté");
            }});
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", " REJETÉ (PROTÉGÉ!)");
                put("error", e.getMessage());
            }});
        }
    }
}