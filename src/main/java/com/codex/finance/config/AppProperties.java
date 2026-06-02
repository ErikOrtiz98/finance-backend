package com.codex.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record AppProperties(String url, String anonKey, String serviceRoleKey, String jwkSetUri) {
}