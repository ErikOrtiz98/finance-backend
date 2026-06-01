package com.codex.finance.repository;

import com.codex.finance.entity.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, UUID> {
    
    @Query(value = "SELECT sp.id, sp.user_id AS userId, sp.name, sp.amount, " +
           "COALESCE(sp.metadata->>'currency', :currency) AS currency, sp.frequency, " +
           "sp.next_date AS nextDueDate, sp.category_id AS categoryId, " +
           "sp.created_at AS createdAt, sp.updated_at AS updatedAt, sp.deleted_at AS deletedAt, " +
           "CASE WHEN sp.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(sp.row_version, 1) AS version " +
           "FROM scheduled_payments sp WHERE sp.user_id = :userId AND sp.deleted_at IS NULL " +
           "ORDER BY sp.next_date ASC", nativeQuery = true)
    List<Object[]> listRecurringPayments(@Param("userId") UUID userId, @Param("currency") String currency);
    
    @Modifying
    @Query(value = "INSERT INTO scheduled_payments (user_id, name, amount, frequency, next_date, " +
           "category_id, metadata, created_at, updated_at, row_version) " +
           "VALUES (:userId, :name, :amount, CAST(:frequency AS public.payment_frequency), " +
           "CAST(:nextDueDate AS date), :categoryId, " +
           "jsonb_build_object('currency', :currency), NOW(), NOW(), 1) " +
           "RETURNING id, user_id AS userId, name, amount, " +
           "COALESCE(metadata->>'currency', :currency) AS currency, frequency, " +
           "next_date AS nextDueDate, category_id AS categoryId, " +
           "created_at AS createdAt, updated_at AS updatedAt, deleted_at AS deletedAt, " +
           "CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(row_version, 1) AS version", nativeQuery = true)
    Object[] createRecurringPayment(@Param("userId") UUID userId, @Param("name") String name,
                                   @Param("amount") BigDecimal amount,
                                   @Param("currency") String currency,
                                   @Param("frequency") String frequency,
                                   @Param("nextDueDate") LocalDate nextDueDate,
                                   @Param("categoryId") UUID categoryId);
    
    @Modifying
    @Query(value = "UPDATE scheduled_payments SET name = :name, amount = :amount, " +
           "frequency = CAST(:frequency AS public.payment_frequency), " +
           "next_date = CAST(:nextDueDate AS date), " +
           "category_id = :categoryId, metadata = jsonb_build_object('currency', :currency), " +
           "updated_at = NOW(), row_version = COALESCE(row_version, 0) + 1 " +
           "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL " +
           "RETURNING id, user_id AS userId, name, amount, " +
           "COALESCE(metadata->>'currency', :currency) AS currency, frequency, " +
           "next_date AS nextDueDate, category_id AS categoryId, " +
           "created_at AS createdAt, updated_at AS updatedAt, deleted_at AS deletedAt, " +
           "CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(row_version, 1) AS version", nativeQuery = true)
    Object[] updateRecurringPayment(@Param("id") UUID id, @Param("userId") UUID userId,
                                   @Param("name") String name, @Param("amount") BigDecimal amount,
                                   @Param("currency") String currency,
                                   @Param("frequency") String frequency,
                                   @Param("nextDueDate") LocalDate nextDueDate,
                                   @Param("categoryId") UUID categoryId);
    
    @Modifying
    @Query(value = "UPDATE scheduled_payments SET deleted_at = NOW(), updated_at = NOW(), " +
           "row_version = COALESCE(row_version, 0) + 1 WHERE id = :id AND user_id = :userId", 
           nativeQuery = true)
    int softDelete(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query(value = "SELECT COUNT(1) FROM scheduled_payments WHERE id = :id AND user_id = :userId", 
           nativeQuery = true)
    int existsByUserAndId(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query(value = "SELECT id, deleted_at AS deletedAt FROM scheduled_payments WHERE user_id = :userId " +
           "AND deleted_at IS NOT NULL AND deleted_at >= COALESCE(:since, deleted_at) " +
           "ORDER BY deleted_at DESC", nativeQuery = true)
    List<Map<String, Object>> findDeleted(@Param("userId") UUID userId, @Param("since") Instant since);
    
    @Query(value = "SELECT 'recurring' AS type, sp.id, sp.name, sp.next_date AS dueDate, sp.amount " +
           "FROM scheduled_payments sp WHERE sp.user_id = :userId AND sp.deleted_at IS NULL " +
           "AND sp.next_date BETWEEN CURRENT_DATE AND CURRENT_DATE + 7 " +
           "ORDER BY sp.next_date ASC", nativeQuery = true)
    List<Object[]> getUpcomingRecurringPayments(@Param("userId") UUID userId);
}