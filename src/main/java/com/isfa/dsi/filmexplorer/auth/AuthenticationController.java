package com.isfa.dsi.filmexplorer.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Enregistrer un nouvel utilisateur
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        log.info("Register endpoint called for email: {}", request.getEmail());

        try {
            AuthenticationResponse response = authenticationService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Se connecter
     * POST /api/auth/login
     * ✅ MODIFIÉ: Ajoute HttpServletResponse pour les cookies
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request,
            HttpServletResponse response) {  // ✅ NOUVEAU PARAMÈTRE

        log.info("Login endpoint called for email: {}", request.getEmail());

        try {
            // ✅ Passer la response au service
            AuthenticationResponse authResponse = authenticationService.authenticate(request, response);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            log.error("Authentication error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}