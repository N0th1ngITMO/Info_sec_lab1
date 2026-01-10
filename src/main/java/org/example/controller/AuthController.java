package org.example.controller;

import org.example.config.JwtTokenProvider;
import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.entity.User;
import org.example.service.SecurityService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityService securityService;

    @Autowired
    public AuthController(JwtTokenProvider jwtTokenProvider,
                          SecurityService securityService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityService = securityService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        logger.info("Login attempt for user: {}", authRequest.getUsername());

        try {
            Optional<User> userOptional = securityService.authenticate(
                    authRequest.getUsername(),
                    authRequest.getPassword()
            );

            if (userOptional.isEmpty()) {
                logger.warn("Bad credentials for user: {}", authRequest.getUsername());
                return ResponseEntity.status(401).body(new AuthResponse(
                        null,
                        "Invalid username or password",
                        false
                ));
            }

            User user = userOptional.get();
            logger.info("Authentication successful for user: {}", user.getUsername());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            String jwt = jwtTokenProvider.generateToken(authentication);

            logger.debug("JWT generated for user: {}", user.getUsername());

            return ResponseEntity.ok(new AuthResponse(
                    jwt,
                    "Authentication successful",
                    true
            ));

        } catch (Exception e) {
            logger.error("Login failed for user: {} - {}",
                    authRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(401).body(new AuthResponse(
                    null,
                    "Authentication failed: " + e.getMessage(),
                    false
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest authRequest) {
        logger.info("Registration attempt for user: {}", authRequest.getUsername());

        try {
            if (securityService.userExists(authRequest.getUsername())) {
                logger.warn("User already exists: {}", authRequest.getUsername());
                return ResponseEntity.badRequest().body(new AuthResponse(
                        null,
                        "User already exists",
                        false
                ));
            }

            User user = securityService.createUser(
                    authRequest.getUsername(),
                    authRequest.getUsername() + "@example.com",
                    authRequest.getPassword()
            );

            logger.info("User created successfully: {}", authRequest.getUsername());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            String jwt = jwtTokenProvider.generateToken(authentication);

            logger.info("Registration and auto-login successful for user: {}",
                    authRequest.getUsername());

            return ResponseEntity.ok(new AuthResponse(
                    jwt,
                    "Registration successful",
                    true
            ));

        } catch (Exception e) {
            logger.error("Registration failed for user: {} - {}",
                    authRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(new AuthResponse(
                    null,
                    "Registration failed: " + e.getMessage(),
                    false
            ));
        }
    }
}