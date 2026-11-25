package org.smorodin.infrastructure.web.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.smorodin.core.service.PlayerService;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.auth.JwtService;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BackendSecurityIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private AuthService authService;
    @Autowired private PlayerService playerService;
    @Autowired private JwtService jwtService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private AuthResponse user() {
        return authService.register(
                "secure_" + UUID.randomUUID().toString().replace("-", ""),
                "TestPassword123!"
        );
    }

    private String bearer(AuthResponse user) {
        return "Bearer " + user.getToken();
    }

    @Test
    void removedExpAndStatEndpointsMustNotChangePlayerState() throws Exception {
        AuthResponse user = user();
        Long id = user.getUserId();
        long beforeExp = playerService.getPlayer(id).getExp();
        int beforeStrength = playerService.getPlayer(id).getStats().getStrength();

        mvc.perform(post("/api/player/exp")
                        .header("Authorization", bearer(user))
                        .param("amount", "500000"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/player/stat")
                        .header("Authorization", bearer(user))
                        .param("stat", "STRENGTH")
                        .param("amount", "100"))
                .andExpect(status().isForbidden());

        assertEquals(beforeExp, playerService.getPlayer(id).getExp());
        assertEquals(beforeStrength, playerService.getPlayer(id).getStats().getStrength());
        assertEquals(0, playerService.getStatProgress(id)
                .get(org.smorodin.core.model.enums.StatType.STRENGTH).currentXp());

        // Legitimate quest APIs remain accessible to the same user.
        mvc.perform(get("/api/quests/daily")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());
    }

    @Test
    void missingMalformedForgedExpiredAndNoExpiryTokensMustBeRejected() throws Exception {
        AuthResponse user = user();

        mvc.perform(get("/api/player"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
        mvc.perform(get("/api/player").header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/player").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());

        String[] parts = user.getToken().split("\\.");
        String signature = parts[2];
        String changedSignature = (signature.startsWith("A") ? "B" : "A") + signature.substring(1);
        String forged = parts[0] + "." + parts[1] + "." + changedSignature;
        mvc.perform(get("/api/player").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
        assertFalse(jwtService.validateToken(forged));

        long now = System.currentTimeMillis();
        String expired = Jwts.builder()
                .setSubject(user.getUserId().toString())
                .setIssuedAt(new Date(now - 60000))
                .setExpiration(new Date(now - 1000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
        mvc.perform(get("/api/player").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
        assertFalse(jwtService.validateToken(expired));

        String noExpiry = Jwts.builder()
                .setSubject(user.getUserId().toString())
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
        mvc.perform(get("/api/player").header("Authorization", "Bearer " + noExpiry))
                .andExpect(status().isUnauthorized());
        assertFalse(jwtService.validateToken(noExpiry));

        mvc.perform(get("/api/player").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statProgress.strength.currentXp").value(0));
    }

    @Test
    void shouldKeepLocalizedAuthorizationErrors() throws Exception {
        mvc.perform(get("/api/player").header("Accept-Language", "ru"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Необходима авторизация"));

        mvc.perform(get("/api/player").header("Accept-Language", "ru")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Необходима авторизация"));
    }

    @Test
    void invalidRegistrationMustReturnSafeLocalized400() throws Exception {
        String shortName = "abc";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "ru")
                        .content(json(shortName, "TestPassword123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Имя пользователя должно содержать 6–100 символов без пробелов по краям"));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("valid_username", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Password must be at least 8 characters and no more than 72 UTF-8 bytes"));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("valid_username", "x".repeat(73))))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("x".repeat(101), "TestPassword123!")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"TestPassword123!\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"valid_username\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    void loginMustNotExposeWhetherAccountExists() throws Exception {
        AuthResponse user = user();

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(user.getUsername(), "wrong-password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("unknown_user_" + UUID.randomUUID(), "wrong-password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"valid_username\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(user.getUsername(), "TestPassword123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void missingPlayerMustReturn404AndNot401InBothLanguages() throws Exception {
        AuthResponse user = user();
        long missingId = Long.MAX_VALUE;

        mvc.perform(get("/api/player/{id}", missingId)
                        .header("Authorization", bearer(user))
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Player not found"));

        mvc.perform(get("/api/player/{id}", missingId)
                        .header("Authorization", bearer(user))
                        .header("Accept-Language", "ru"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Игрок не найден"));
    }

    @Test
    void corsMustAllowConfiguredLocalOriginButRejectUnknownOrigin() throws Exception {
        mvc.perform(options("/api/player")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mvc.perform(options("/api/player")
                        .header("Origin", "https://untrusted.invalid")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private static String json(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }
}
