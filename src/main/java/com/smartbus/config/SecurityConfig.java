package com.smartbus.config;

import com.smartbus.security.CustomAuthSuccessHandler;
import com.smartbus.security.CustomUserDetailsService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthSuccessHandler successHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService, CustomAuthSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new com.smartbus.security.SpaCsrfTokenRequestHandler())
                .ignoringRequestMatchers("/api/gps/location", "/ws/**", "/h2-console/**", "/assistant/nearby/reply")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(
                    "/", "/index", "/login", "/driver/login", "/register", "/guest/**",
                    "/user/search", "/user/nearby", "/user/tracking/**",
                    "/api/auth/**", "/api/gps/location", "/api/buses/search", "/api/buses/nearby",
                    "/assistant/nearby/reply",
                    "/api/stops/**", "/api/routes/**", "/ws/**",
                    "/css/**", "/js/**", "/images/**", "/favicon.ico"
                ).permitAll()
                .requestMatchers("/api/gps/simulate/**").hasAnyRole("BCU", "ADMIN")
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                .requestMatchers("/api/tickets/verify").hasAnyRole("DRIVER", "BCU", "ADMIN")
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/bcu/**", "/api/bcu/**").hasAnyRole("BCU", "ADMIN")
                .requestMatchers("/driver/**", "/api/driver/**").hasAnyRole("DRIVER", "ADMIN")
                .requestMatchers("/user/**", "/api/bookings/**", "/api/tickets/**").hasAnyRole("USER", "ADMIN", "BCU")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession())
            );

        return http.build();
    }
}
