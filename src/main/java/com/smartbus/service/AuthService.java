package com.smartbus.service;

import com.smartbus.dto.RegisterRequest;
import com.smartbus.entity.*;
import com.smartbus.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern STRONG_PASSWORD =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{12,72}$");

    private final UserRepository userRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    public AuthService(UserRepository userRepository,
                       PassengerProfileRepository passengerProfileRepository,
                       PasswordEncoder passwordEncoder,
                       AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passengerProfileRepository = passengerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public User registerUser(RegisterRequest req) {
        if (req == null || req.getUsername() == null || req.getEmail() == null || req.getPassword() == null) {
            throw new IllegalArgumentException("Username, email, and password are required.");
        }
        String username = req.getUsername().trim();
        String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!username.matches("^[A-Za-z0-9._-]{3,60}$")) {
            throw new IllegalArgumentException("Username must be 3-60 characters and use letters, numbers, dot, underscore, or hyphen.");
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 100) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!STRONG_PASSWORD.matcher(req.getPassword()).matches()) {
            throw new IllegalArgumentException("Password must be 12-72 characters and include an uppercase letter, lowercase letter, number, and symbol.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User(
                username,
                email,
                passwordEncoder.encode(req.getPassword()),
                Role.ROLE_USER
        );
        User savedUser = userRepository.save(user);

        // Auto-generate permanent unique Passenger ID: e.g. PB2026001847
        String passengerId = generateUniquePassengerId();
        PassengerProfile profile = new PassengerProfile(
                savedUser,
                passengerId,
                req.getFullName() != null && !req.getFullName().isBlank() ? req.getFullName() : req.getUsername(),
                req.getPhone(),
                req.getAddress(),
                req.getEmergencyContact()
        );
        passengerProfileRepository.save(profile);

        auditLogRepository.save(new AuditLog(
                savedUser.getUsername(),
                Role.ROLE_USER.name(),
                "USER_REGISTERED",
                "New user registered with Passenger ID " + passengerId,
                "127.0.0.1"
        ));

        return savedUser;
    }

    private String generateUniquePassengerId() {
        int year = Year.now().getValue();
        Random random = new Random();
        String id;
        do {
            int seq = 100000 + random.nextInt(900000);
            id = "PB" + year + seq;
        } while (passengerProfileRepository.existsByPassengerId(id));
        return id;
    }
}
