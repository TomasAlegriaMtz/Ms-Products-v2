package com.ids.ms_products.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // habilita @PreAuthorize en controllers
public class SecurityConfig {
    private static final String ADMIN = "ADMIN";
    private static final String EP_BASE= "/api/products/**";
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(cus -> cus.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/import").hasAnyRole("CLIENT", ADMIN)
                        .requestMatchers(HttpMethod.POST,EP_BASE).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, EP_BASE).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, EP_BASE).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, EP_BASE).hasRole(ADMIN)
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
