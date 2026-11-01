package com.smartbus.controller;

import com.smartbus.dto.AuthResponse;
import com.smartbus.dto.RegisterRequest;
import com.smartbus.entity.PassengerProfile;
import com.smartbus.entity.User;
import com.smartbus.repository.PassengerProfileRepository;
import com.smartbus.repository.UserRepository;
import com.smartbus.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PassengerProfileRepository profileRepository;

    public AuthController(AuthService authService, UserRepository userRepository, PassengerProfileRepository profileRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        try {
            User user = authService.registerUser(req);
            PassengerProfile profile = profileRepository.findByUser(user).orElse(null);
            String pid = profile != null ? profile.getPassengerId() : "";

            return ResponseEntity.ok(new AuthResponse(true, "Registration successful! Passenger ID: " + pid,
                    user.getUsername(), user.getRole().name(), pid, "/user/dashboard"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, e.getMessage(), null, null, null, null));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        if (auth == null || !auth.isAuthenticated()) {
            res.put("authenticated", false);
            return ResponseEntity.ok(res);
        }
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user != null) {
            res.put("authenticated", true);
            res.put("username", user.getUsername());
            res.put("role", user.getRole().name());
            profileRepository.findByUser(user).ifPresent(p -> res.put("passengerId", p.getPassengerId()));
        }
        return ResponseEntity.ok(res);
    }
}
