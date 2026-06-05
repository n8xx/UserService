package com.innowise.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Base class for controller integration tests.
 * Uses the singleton containers pattern so Postgres and Redis are started once
 * per JVM and shared between test classes — this avoids
 * "connection refused" errors when Spring reuses the application context
 * but the underlying container has been recreated for the next test class.
 * Provides real JWT tokens signed with the same secret that is injected
 * into the application context via {@code jwt.secret}.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractControllerIntegrationTest {

    protected static final String TEST_JWT_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWxvbmctZW5vdWdoLWZvci1obWFjLXNoYTI1Ni1hbGdvcml0aG0tbWluaW11bQ==";
    protected static final String ROLE_ADMIN = "ADMIN";
    protected static final String ROLE_USER = "USER";
    protected static final Long ADMIN_USER_ID = 1L;
    protected static final Long REGULAR_USER_ID = 2L;

    private static final Duration TOKEN_TTL = Duration.ofHours(1);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int REDIS_PORT = 6379;

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine");
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(REDIS_PORT);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
        registry.add("jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

  
    protected String adminToken() {
        return bearer(generateToken(ADMIN_USER_ID, ROLE_ADMIN));
    }


    protected String userToken(Long userId) {
        return bearer(generateToken(userId, ROLE_USER));
    }

    protected Long extractId(String response) throws Exception {
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String generateToken(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_TTL)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_JWT_SECRET)))
                .compact();
    }

    private String bearer(String token) {
        return BEARER_PREFIX + token;
    }
}