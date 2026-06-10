package com.aidevplanner.backend.auth;

import com.aidevplanner.backend.user.User;
import com.aidevplanner.backend.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthService(
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = normalizeOptional(request.email());
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUserException("Username is already registered.");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new DuplicateUserException("Email is already registered.");
        }

        User user = userRepository.save(new User(
                username,
                email,
                passwordEncoder.encode(request.password())
        ));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password.");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(
                jwtService.createToken(user),
                "Bearer",
                user.getId(),
                user.getUsername()
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
