package com.codex.finance.repository;

import com.codex.finance.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    
    @Query(value = "SELECT p.user_id AS userId, p.user_id AS id, " +
           "COALESCE(p.full_name, '') AS displayName, " +
           "COALESCE(p.currency_code, 'MXN') AS currency, " +
           "COALESCE(p.settings->>'payCycle', 'quincenal') AS payCycle, " +
           "p.settings->'payDays' AS payDays, " +
           "p.created_at AS createdAt, p.updated_at AS updatedAt, p.deleted_at AS deletedAt, " +
           "CASE WHEN p.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(p.row_version, 1) AS version " +
           "FROM profiles p WHERE p.user_id = :userId", nativeQuery = true)
    Object[] getProfile(@Param("userId") UUID userId);
    
    @Modifying
    @Query(value = "INSERT INTO profiles (user_id, full_name, currency_code, settings, preferences, " +
           "created_at, updated_at, row_version) VALUES (:userId, :displayName, :currency, " +
           "jsonb_build_object('payCycle', CAST(:payCycle AS text), 'payDays', CAST(:payDays AS jsonb)), " +
           "COALESCE((SELECT preferences FROM profiles WHERE user_id = :userId2), CAST('{}' AS jsonb)), " +
           "NOW(), NOW(), 1) ON CONFLICT (user_id) DO UPDATE SET " +
           "full_name = EXCLUDED.full_name, currency_code = EXCLUDED.currency_code, " +
           "settings = EXCLUDED.settings, updated_at = NOW(), " +
           "row_version = COALESCE(profiles.row_version, 0) + 1 " +
           "RETURNING user_id AS userId, user_id AS id, COALESCE(full_name, '') AS displayName, " +
           "COALESCE(currency_code, 'MXN') AS currency, " +
           "COALESCE(settings->>'payCycle', 'quincenal') AS payCycle, " +
           "settings->'payDays' AS payDays, created_at AS createdAt, updated_at AS updatedAt, " +
           "deleted_at AS deletedAt, CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(row_version, 1) AS version", nativeQuery = true)
    Object[] upsertProfile(@Param("userId") UUID userId, @Param("displayName") String displayName,
                          @Param("currency") String currency, @Param("payCycle") String payCycle,
                          @Param("payDays") String payDays, @Param("userId2") UUID userId2);
    
    @Query(value = "SELECT COALESCE(currency_code, 'MXN') FROM profiles WHERE user_id = :userId", 
           nativeQuery = true)
    String getUserCurrency(@Param("userId") UUID userId);
}