package com.isfa.dsi.filmexplorer.auth;

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
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        log.info("Login endpoint called for email: {}", request.getEmail());

        try {
            AuthenticationResponse response = authenticationService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Authentication error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}