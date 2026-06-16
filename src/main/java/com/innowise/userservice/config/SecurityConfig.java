package com.innowise.userservice.config;

import com.innowise.userservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@SuppressWarnings({ "java:S1075"})
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String USERS_PATH = "/api/v1/users";
    private static final String USER_ID_PATH = "/{id}";
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, USERS_PATH).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST,USERS_PATH).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, USERS_PATH+ USER_ID_PATH).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, USERS_PATH+ USER_ID_PATH).hasRole(ADMIN)
                        .requestMatchers(USERS_PATH + USER_ID_PATH+"/activate").hasRole(ADMIN)
                        .requestMatchers(USERS_PATH+ USER_ID_PATH+"/deactivate").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, USERS_PATH + USER_ID_PATH).authenticated()
                        .requestMatchers(USERS_PATH + USER_ID_PATH+"/cards/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}