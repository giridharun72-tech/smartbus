package com.smartbus.controller;

import com.smartbus.dto.NearbyAssistantRequest;
import com.smartbus.dto.NearbyAssistantResponse;
import com.smartbus.service.NearbyAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assistant/nearby")
public class NearbyAssistantController {
    private final NearbyAssistantService assistantService;

    public NearbyAssistantController(NearbyAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/reply")
    public ResponseEntity<NearbyAssistantResponse> reply(
            @Valid @RequestBody NearbyAssistantRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(assistantService.reply(request, currentRole(authentication)));
    }

    private String currentRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "GUEST";
        }

        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .filter(role -> switch (role) {
                    case "ADMIN", "BCU", "DRIVER", "USER" -> true;
                    default -> false;
                })
                .findFirst()
                .orElse("GUEST");
    }
}
