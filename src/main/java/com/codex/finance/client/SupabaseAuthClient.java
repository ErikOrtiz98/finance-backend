package com.codex.finance.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(SupabaseAuthClient.class);

	private final WebClient webClient;
	private final AppProperties properties;

	public SupabaseAuthClient(WebClient webClient, AppProperties properties) {
		this.webClient = webClient;
		this.properties = properties;
	}

//    public AuthResponse signUp(SignUpRequest request) {
//        JsonNode payload = webClient.post()
//                .uri(properties.url() + "/auth/v1/signup")
//                .header("apikey", properties.anonKey())
//                .header("Authorization", "Bearer " + properties.anonKey())
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(java.util.Map.of(
//                        "email", request.email(),
//                        "password", request.password(),
//                        "data", java.util.Map.of("display_name", request.displayName())
//                ))
//                .retrieve()
//                .bodyToMono(JsonNode.class)
//                .block();
//        return toAuthResponse(payload);
//    }
	public AuthResponse signUp(SignUpRequest request) {
		try {
			JsonNode payload = webClient.post().uri(properties.url() + "/auth/v1/signup")
					.header("apikey", properties.anonKey()).header("Authorization", "Bearer " + properties.anonKey())
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(java.util.Map.of("email", request.email(), "password", request.password(), "data",
							java.util.Map.of("full_name", request.displayName())))
					.retrieve().bodyToMono(JsonNode.class).block();
			return toAuthResponse(payload);
		} catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
			log.error("DETALLE DEL ERROR SIGNUP: {}", e.getResponseBodyAsString());
			throw e;
		}
	}

//    public AuthResponse signIn(SignInRequest request) {
//        JsonNode payload = webClient.post()
//                .uri(properties.url() + "/auth/v1/token?grant_type=password")
//                .header("apikey", properties.anonKey())
//                .header("Authorization", "Bearer " + properties.anonKey()) // Añadido
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(java.util.Map.of(
//                        "grant_type", "password", // Añadido al body
//                        "email", request.email(),
//                        "password", request.password()
//                ))
//                .retrieve()
//                .bodyToMono(JsonNode.class)
//                .block();
//        return toAuthResponse(payload);
//    }
	public AuthResponse signIn(SignInRequest request) {
		try {
			JsonNode payload = webClient.post().uri(properties.url() + "/auth/v1/token?grant_type=password")
					.header("apikey", properties.anonKey()).header("Authorization", "Bearer " + properties.anonKey())
					.contentType(MediaType.APPLICATION_JSON).bodyValue(java.util.Map.of("grant_type", "password",
							"email", request.email(), "password", request.password()))
					.retrieve().bodyToMono(JsonNode.class).block();
			return toAuthResponse(payload);
		} catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
			// AQUÍ VEREMOS EL ERROR REAL
			log.error("Error de Supabase: {}", e.getResponseBodyAsString());
			throw e;
		}
	}

	public SessionResponse refresh(RefreshRequest request) {
		JsonNode payload = webClient.post().uri(properties.url() + "/auth/v1/token?grant_type=refresh_token")
				.header("apikey", properties.anonKey()).header("Authorization", "Bearer " + properties.anonKey()) // Añadido
				.contentType(MediaType.APPLICATION_JSON).bodyValue(java.util.Map.of("grant_type", "refresh_token", // Añadido
																													// al
																													// body
						"refresh_token", request.refreshToken()))
				.retrieve().bodyToMono(JsonNode.class).block();
		return toSessionResponse(payload);
	}

	public void signOut(String accessToken) {
		webClient.post().uri(properties.url() + "/auth/v1/logout").header("apikey", properties.anonKey())
				.header("Authorization", normalizeBearer(accessToken)).retrieve().toBodilessEntity().block();
	}

	public SessionResponse currentSession(String accessToken, String refreshToken) {
		try {
			// Intentar obtener el usuario con el token actual
			JsonNode payload = webClient.get().uri(properties.url() + "/auth/v1/user")
					.header("apikey", properties.anonKey()).header("Authorization", "Bearer " + accessToken).retrieve()
					.bodyToMono(JsonNode.class).block();

			return toSessionResponse(payload);

		} catch (org.springframework.web.reactive.function.client.WebClientResponseException.Forbidden e) {
			// SI FALLA POR 403 (Forbidden), significa que el token expiró.
			// 1. Ejecutamos el refresh
			log.info("Token expirado, intentando refrescar...");
			SessionResponse newSession = refresh(new RefreshRequest(refreshToken));

			// 2. Intentamos de nuevo con el nuevo token
			return newSession;
		}
	}

	private AuthResponse toAuthResponse(JsonNode payload) {
		if (payload == null) {
			return new AuthResponse(new UserDto("unknown", null, null),
					new SessionDto("unknown", "unknown", Instant.now().plusSeconds(3600)));
		}
		JsonNode user = payload.path("user");
		return new AuthResponse(
				new UserDto(user.path("id").asText("unknown"), user.path("email").asText(null),
						user.path("user_metadata").path("full_name").asText(null)),
				new SessionDto(payload.path("access_token").asText(null), payload.path("refresh_token").asText(null),
						Instant.ofEpochSecond(
								payload.path("expires_at").asLong(Instant.now().plusSeconds(3600).getEpochSecond()))));
	}

	private SessionResponse toSessionResponse(JsonNode payload) {
		if (payload == null) {
			return new SessionResponse(new UserDto("unknown", null, null), new SessionDto(null, null, Instant.now()));
		}
		JsonNode user = payload.path("user");
		return new SessionResponse(
				new UserDto(user.path("id").asText("unknown"), user.path("email").asText(null),
						user.path("user_metadata").path("full_name").asText(null)),
				new SessionDto(payload.path("access_token").asText(null), payload.path("refresh_token").asText(null),
						Instant.ofEpochSecond(
								payload.path("expires_at").asLong(Instant.now().plusSeconds(3600).getEpochSecond()))));
	}

	private String normalizeBearer(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			throw new IllegalArgumentException("accessToken is required");
		}
		return accessToken.regionMatches(true, 0, "Bearer ", 0, 7) ? accessToken : "Bearer " + accessToken;
	}
}
