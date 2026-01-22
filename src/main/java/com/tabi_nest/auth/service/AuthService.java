package com.tabi_nest.auth.service;

import com.tabi_nest.auth.dto.LoginRequest;
import com.tabi_nest.auth.dto.SignupRequest;
import com.tabi_nest.user.domain.User;
import com.tabi_nest.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User signup(SignupRequest req) {
        String email = (req.getEmail() == null) ? null : req.getEmail().trim().toLowerCase();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already exists");
        }

        User u = new User();
        u.setEmail(email);
        u.setName(req.getName().trim());
        u.setPhone(req.getPhone());
        u.setRole("USER");
        u.setStatus("A");
        u.setProvider("L");
        u.setPassword(encoder.encode(req.getPassword()));
        return userRepository.save(u);
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest req) {
        String email = (req.getEmail() == null) ? null : req.getEmail().trim().toLowerCase();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (u.getPassword() == null || !encoder.matches(req.getPassword(), u.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (u.getStatus() != null && !"A".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalStateException("User is not active");
        }
        return u;
    }
}
