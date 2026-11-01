package com.smartbus.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Pattern;

@Component
public class StompSubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern BUS_LOCATION_DESTINATION = Pattern.compile("^/topic/bus-location(?:/\\d+)?$");
    private static final Pattern ROUTE_CHANGE_DESTINATION = Pattern.compile("^/topic/route-changes/\\d+$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.SEND) {
            throw new AccessDeniedException("Client-to-server messaging is not allowed.");
        }
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("A subscription destination is required.");
        }
        if (BUS_LOCATION_DESTINATION.matcher(destination).matches()
                || ROUTE_CHANGE_DESTINATION.matcher(destination).matches()
                || destination.equals("/topic/notifications")) {
            return message;
        }
        if (destination.equals("/topic/bcu-alerts") || destination.equals("/topic/fleet-updates")) {
            requireRole(accessor.getUser(), "ROLE_BCU", "ROLE_ADMIN");
            return message;
        }
        if (destination.equals("/user/queue/notifications")) {
            requireAuthenticated(accessor.getUser());
            return message;
        }
        throw new AccessDeniedException("Subscription to this destination is not allowed.");
    }

    private void requireRole(Principal principal, String... roles) {
        Authentication authentication = authenticated(principal);
        boolean authorized = authentication.getAuthorities().stream()
                .anyMatch(authority -> java.util.Arrays.asList(roles).contains(authority.getAuthority()));
        if (!authorized) {
            throw new AccessDeniedException("A staff role is required for this subscription.");
        }
    }

    private void requireAuthenticated(Principal principal) {
        authenticated(principal);
    }

    private Authentication authenticated(Principal principal) {
        if (!(principal instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication is required for this subscription.");
        }
        return authentication;
    }
}
