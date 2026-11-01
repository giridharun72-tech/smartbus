package com.smartbus.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StompSubscriptionAuthorizationInterceptorTest {

    private final StompSubscriptionAuthorizationInterceptor interceptor =
            new StompSubscriptionAuthorizationInterceptor();

    @Test
    void allowsAnonymousBusLocationSubscriptions() {
        Message<byte[]> message = subscription("/topic/bus-location/12", null);

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    @Test
    void deniesAnonymousStaffSubscriptions() {
        Message<byte[]> message = subscription("/topic/bcu-alerts", null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void allowsStaffSubscriptionsToFleetUpdates() {
        Principal bcu = UsernamePasswordAuthenticationToken.authenticated(
                "bcu", "", List.of(new SimpleGrantedAuthority("ROLE_BCU")));
        Message<byte[]> message = subscription("/topic/fleet-updates", bcu);

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    @Test
    void requiresAuthenticationForUserNotifications() {
        Message<byte[]> anonymous = subscription("/user/queue/notifications", null);
        Principal passenger = UsernamePasswordAuthenticationToken.authenticated(
                "passenger", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Message<byte[]> authenticated = subscription("/user/queue/notifications", passenger);

        assertThatThrownBy(() -> interceptor.preSend(anonymous, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(interceptor.preSend(authenticated, null)).isSameAs(authenticated);
    }

    @Test
    void rejectsUnknownSubscriptionsAndClientMessages() {
        Message<byte[]> unknown = subscription("/topic/user-notifications/1", null);
        Message<byte[]> send = message(StompCommand.SEND, "/app/anything", null);

        assertThatThrownBy(() -> interceptor.preSend(unknown, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(send, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Message<byte[]> subscription(String destination, Principal principal) {
        return message(StompCommand.SUBSCRIBE, destination, principal);
    }

    private Message<byte[]> message(StompCommand command, String destination, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
