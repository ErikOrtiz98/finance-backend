package com.codex.finance.client;

import com.codex.finance.config.AppProperties;
import com.codex.finance.dto.ContractDtos.AuthResponse;
import com.codex.finance.dto.ContractDtos.RefreshRequest;
import com.codex.finance.dto.ContractDtos.SignInRequest;
import com.codex.finance.dto.ContractDtos.SignUpRequest;
import com.codex.finance.dto.ContractDtos.SessionDto;
import com.codex.finance.dto.ContractDtos.SessionResponse;
import com.codex.finance.dto.ContractDtos.UserDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Component
public class SupabaseAuthClient {
    private final WebClient webClient;
    private final AppProperties properties;

    public SupabaseAuthClient(WebClient webClient, AppProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public AuthResponse signUp(SignUpRequest request) {
        JsonNode payload = webClient.post()
                .uri(properties.url() + "/auth/v1/signup")
                .header("apikey", properties.anonKey())
                .header("Authorization", "Bearer " + properties.anonKey()) // <--- AGREGADO
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of(
                        "email", request.email(),
                        "password", request.password(),
                        "data", java.util.Map.of("display_name", request.displayName())
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return toAuthResponse(payload);
    }

    public AuthResponse signIn(SignInRequest request) {
        JsonNode payload = webClient.post()
                .uri(properties.url() + "/auth/v1/token?grant_type=password")
                .header("apikey", properties.anonKey())
                .header("Authorization", "Bearer " + properties.anonKey()) // <--- AGREGADO
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of(
                        "email", request.email(),
                        "password", request.password()
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return toAuthResponse(payload);
    }

    public SessionResponse refresh(RefreshRequest request) {
        JsonNode payload = webClient.post()
                .uri(properties.url() + "/auth/v1/token?grant_type=refresh_token")
                .header("apikey", properties.anonKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("refresh_token", request.refreshToken()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return toSessionResponse(payload);
    }

    public void signOut(String accessToken) {
        webClient.post()
                .uri(properties.url() + "/auth/v1/logout")
                .header("apikey", properties.anonKey())
                .header("Authorization", normalizeBearer(accessToken))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public SessionResponse currentSession(String accessToken, String refreshToken) {
        return new SessionResponse(
                new UserDto("unknown", null, null),
                new SessionDto(accessToken, refreshToken, Instant.now().plusSeconds(3600))
        );
    }

    private AuthResponse toAuthResponse(JsonNode payload) {
        if (payload == null) {
            return new AuthResponse(
                    new UserDto("unknown", null, null),
                    new SessionDto("unknown", "unknown", Instant.now().plusSeconds(3600))
            );
        }
        JsonNode user = payload.path("user");
        return new AuthResponse(
                new UserDto(
                        user.path("id").asText("unknown"),
                        user.path("email").asText(null),
                        user.path("user_metadata").path("display_name").asText(null)
                ),
                new SessionDto(
                        payload.path("access_token").asText(null),
                        payload.path("refresh_token").asText(null),
                        Instant.ofEpochSecond(payload.path("expires_at").asLong(Instant.now().plusSeconds(3600).getEpochSecond()))
                )
        );
    }

    private SessionResponse toSessionResponse(JsonNode payload) {
        if (payload == null) {
            return new SessionResponse(new UserDto("unknown", null, null), new SessionDto(null, null, Instant.now()));
        }
        JsonNode user = payload.path("user");
        return new SessionResponse(
                new UserDto(
                        user.path("id").asText("unknown"),
                        user.path("email").asText(null),
                        user.path("user_metadata").path("display_name").asText(null)
                ),
                new SessionDto(
                        payload.path("access_token").asText(null),
                        payload.path("refresh_token").asText(null),
                        Instant.ofEpochSecond(payload.path("expires_at").asLong(Instant.now().plusSeconds(3600).getEpochSecond()))
                )
        );
    }

    private String normalizeBearer(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken is required");
        }
        return accessToken.regionMatches(true, 0, "Bearer ", 0, 7)
                ? accessToken
                : "Bearer " + accessToken;
    }
}
