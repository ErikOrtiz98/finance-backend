package com.codex.finance.repository;

import com.codex.finance.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    
    @Query(value = "SELECT a.id, a.user_id AS userId, a.account_type AS type, a.name, " +
           "a.bank_name AS institution, COALESCE(a.metadata->>'currency', :currency) AS currency, " +
           "a.current_balance AS balance, a.credit_limit AS creditLimit, " +
           "a.statement_close_day AS closingDay, a.payment_due_day AS dueDay, " +
           "a.is_active AS active, a.created_at AS createdAt, a.updated_at AS updatedAt, " +
           "a.deleted_at AS deletedAt, " +
           "CASE WHEN a.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(a.row_version, 1) AS version " +
           "FROM accounts a WHERE a.user_id = :userId AND a.deleted_at IS NULL " +
           "ORDER BY a.created_at DESC", nativeQuery = true)
    List<Object[]> listAccounts(@Param("userId") UUID userId, @Param("currency") String currency);
    
    @Modifying
    @Query(value = "INSERT INTO accounts (user_id, name, account_type, bank_name, current_balance, " +
           "credit_limit, statement_close_day, payment_due_day, is_active, metadata, " +
           "created_at, updated_at, row_version) VALUES (:userId, :name, " +
           "CAST(:type AS public.account_type), :institution, COALESCE(:balance, 0), :creditLimit, " +
           ":closingDay, :dueDay, COALESCE(:active, true), " +
           "jsonb_build_object('currency', :currency), NOW(), NOW(), 1) " +
           "RETURNING id, user_id AS userId, name, account_type AS type, bank_name AS institution, " +
           "COALESCE(metadata->>'currency', :currency) AS currency, current_balance AS balance, " +
           "credit_limit AS creditLimit, statement_close_day AS closingDay, " +
           "payment_due_day AS dueDay, is_active AS active, created_at AS createdAt, " +
           "updated_at AS updatedAt, deleted_at AS deletedAt, " +
           "CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(row_version, 1) AS version", nativeQuery = true)
    Object[] createAccount(@Param("userId") UUID userId, @Param("name") String name,
                          @Param("type") String type, @Param("institution") String institution,
                          @Param("balance") BigDecimal balance, @Param("creditLimit") BigDecimal creditLimit,
                          @Param("closingDay") Integer closingDay, @Param("dueDay") Integer dueDay,
                          @Param("active") Boolean active, @Param("currency") String currency);
    
    @Modifying
    @Query(value = "UPDATE accounts SET name = :name, " +
           "account_type = CAST(:type AS public.account_type), bank_name = :institution, " +
           "current_balance = COALESCE(:balance, current_balance), credit_limit = :creditLimit, " +
           "statement_close_day = :closingDay, payment_due_day = :dueDay, " +
           "is_active = COALESCE(:active, is_active), " +
           "updated_at = NOW(), row_version = COALESCE(row_version, 0) + 1 " +
           "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL " +
           "RETURNING id, user_id AS userId, name, account_type AS type, bank_name AS institution, " +
           "current_balance AS balance, credit_limit AS creditLimit, " +
           "statement_close_day AS closingDay, payment_due_day AS dueDay, is_active AS active, " +
           "created_at AS createdAt, updated_at AS updatedAt, deleted_at AS deletedAt, " +
           "CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(row_version, 1) AS version", nativeQuery = true)
    Object[] updateAccount(@Param("id") UUID id, @Param("userId") UUID userId,
                          @Param("name") String name, @Param("type") String type,
                          @Param("institution") String institution, @Param("balance") BigDecimal balance,
                          @Param("creditLimit") BigDecimal creditLimit, @Param("closingDay") Integer closingDay,
                          @Param("dueDay") Integer dueDay, @Param("active") Boolean active);
    
    @Modifying
    @Query(value = "UPDATE accounts SET deleted_at = NOW(), updated_at = NOW(), " +
           "row_version = COALESCE(row_version, 0) + 1 WHERE id = :id AND user_id = :userId", 
           nativeQuery = true)
    int softDelete(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query(value = "SELECT COUNT(1) FROM accounts WHERE id = :id AND user_id = :userId", 
           nativeQuery = true)
    int existsByUserAndId(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query(value = "SELECT COALESCE(SUM(current_balance), 0) FROM accounts " +
           "WHERE user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
    BigDecimal getTotalBalance(@Param("userId") UUID userId);
    
    @Query(value = "SELECT COALESCE(current_balance, 0) FROM accounts " +
           "WHERE user_id = :userId AND id = :accountId", nativeQuery = true)
    BigDecimal getAccountBalance(@Param("userId") UUID userId, @Param("accountId") UUID accountId);
    
    @Query(value = "SELECT id, deleted_at AS deletedAt FROM accounts WHERE user_id = :userId " +
           "AND deleted_at IS NOT NULL AND deleted_at >= COALESCE(:since, deleted_at) " +
           "ORDER BY deleted_at DESC", nativeQuery = true)
    List<Map<String, Object>> findDeleted(@Param("userId") UUID userId, @Param("since") Instant since);
    
    @Modifying
    @Query(value = "UPDATE accounts SET current_balance = :newBalance, updated_at = NOW() " +
           "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
    int updateBalance(@Param("id") UUID id, @Param("userId") UUID userId, @Param("newBalance") BigDecimal newBalance);
    
    @Query(value = "SELECT a.id, a.user_id, a.account_type, a.name, a.bank_name, " +
    	       "a.current_balance, a.credit_limit, a.statement_close_day, a.payment_due_day, " +
    	       "a.is_active, COALESCE(a.metadata->>'currency', 'MXN') as currency, " +
    	       "a.created_at, a.updated_at, a.deleted_at, " +
    	       "CASE WHEN a.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus " +
    	       "FROM accounts a " +
    	       "WHERE a.user_id = :userId AND a.id = :id AND a.deleted_at IS NULL", nativeQuery = true)
    	Object[] getAccountById(@Param("id") UUID id, @Param("userId") UUID userId);
    
}