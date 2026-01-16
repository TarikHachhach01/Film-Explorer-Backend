package com.isfa.dsi.filmexplorer.auth;

import com.isfa.dsi.filmexplorer.config.JwtService;
import com.isfa.dsi.filmexplorer.user.Role;
import com.isfa.dsi.filmexplorer.user.User;
import com.isfa.dsi.filmexplorer.user.UserRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Enregistrer un nouvel utilisateur
     */
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Determine role - default to USER if not provided
        Role userRole = Role.USER;
        if (request.getRole() != null) {
            try {
                userRole = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                userRole = Role.USER;
            }
        }

        // Créer le nouvel utilisateur
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .accountLocked(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Sauvegarder l'utilisateur
        var savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Générer les tokens
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Se connecter
     * ✅ MODIFIÉ: Ajoute HttpServletResponse pour envoyer les cookies
     */
    public AuthenticationResponse authenticate(
            AuthenticationRequest request,
            HttpServletResponse response) {  // ✅ NOUVEAU PARAMÈTRE

        log.info("Authenticating user with email: {}", request.getEmail());

        try {
            // Authentifier l'utilisateur
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Récupérer l'utilisateur
            var user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            log.info("User authenticated successfully: {}", user.getEmail());

            // Générer les tokens
            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);

            // ✅ NOUVEAU: Envoyer les tokens comme HttpOnly Cookies
            setTokenCookie(response, "accessToken", jwtToken, 15 * 60);     // 15 minutes
            setTokenCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60); // 7 jours

            log.info("Tokens set as HttpOnly Cookies");

            // ✅ Retourner vide (tokens maintenant dans les cookies)
            return AuthenticationResponse.builder()
                    .accessToken(null)
                    .refreshToken(null)
                    .build();

        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid credentials");
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE: Définir un HttpOnly Cookie
     */
    private void setTokenCookie(
            HttpServletResponse response,
            String name,
            String value,
            int maxAgeSeconds) {

        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);

        log.debug("Cookie {} set with maxAge {} seconds", name, maxAgeSeconds);
    }
}