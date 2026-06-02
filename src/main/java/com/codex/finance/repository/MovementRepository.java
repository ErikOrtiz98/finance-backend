package com.codex.finance.repository;

import com.codex.finance.entity.Movement;
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
public interface MovementRepository extends JpaRepository<Movement, UUID> {
    
	@Query(value = "SELECT m.id, m.user_id AS userId, m.account_id AS accountId, " +
		       "m.transfer_account_id AS transferAccountId, m.category_id AS categoryId, " +
		       "m.movement_type AS type, m.description, m.amount, " +
		       "COALESCE(m.metadata->>'currency', :currency) AS currency, " +
		       "m.movement_date AS transactionDate, COALESCE(m.metadata->>'notes', '') AS notes, " +
		       "m.created_at AS createdAt, m.updated_at AS updatedAt, m.deleted_at AS deletedAt, " +
		       "CASE WHEN m.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
		       "COALESCE(m.row_version, 1) AS version " +
		       "FROM movements m WHERE m.user_id = :userId AND m.deleted_at IS NULL " +
		       "ORDER BY m.movement_date DESC, m.created_at DESC LIMIT :limit", nativeQuery = true)
		List<Object[]> findAllMovements(@Param("userId") UUID userId, 
		                                @Param("currency") String currency,
		                                @Param("limit") Integer limit);
    
    @Modifying
    @Query(value = "INSERT INTO movements (user_id, account_id, transfer_account_id, category_id, " +
           "movement_type, amount, description, movement_date, tags, metadata, " +
           "created_at, updated_at, row_version) VALUES (:userId, :accountId, " +
           ":transferAccountId, :categoryId, CAST(:type AS public.movement_type), :amount, :description, " +
           ":transactionDate, '{}'::text[], " +
           "jsonb_build_object('currency', :currency, 'notes', :notes), NOW(), NOW(), 1) " +
           "RETURNING id, user_id AS userId, account_id AS accountId, " +
           "transfer_account_id AS transferAccountId, category_id AS categoryId, " +
           "movement_type AS type, description, amount, " +
           "COALESCE(metadata->>'currency', :currency) AS currency, " +
           "movement_date AS transactionDate, COALESCE(metadata->>'notes', '') AS notes, " +
           "created_at AS createdAt, updated_at AS updatedAt, deleted_at AS deletedAt, " +
           "CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(row_version, 1) AS version", nativeQuery = true)
    Object[] createTransaction(@Param("userId") UUID userId, @Param("accountId") UUID accountId,
                              @Param("transferAccountId") UUID transferAccountId,
                              @Param("categoryId") UUID categoryId, @Param("type") String type,
                              @Param("amount") BigDecimal amount, @Param("description") String description,
                              @Param("currency") String currency,
                              @Param("transactionDate") LocalDate transactionDate,
                              @Param("notes") String notes);
    
    @Modifying
    @Query(value = "UPDATE movements SET account_id = :accountId, " +
           "transfer_account_id = :transferAccountId, category_id = :categoryId, " +
           "movement_type = CAST(:type AS public.movement_type), amount = :amount, " +
           "description = :description, movement_date = :transactionDate, " +
           "metadata = jsonb_build_object('currency', :currency, 'notes', :notes), " +
           "updated_at = NOW(), row_version = COALESCE(row_version, 0) + 1 " +
           "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL " +
           "RETURNING id, user_id AS userId, account_id AS accountId, " +
           "transfer_account_id AS transferAccountId, category_id AS categoryId, " +
           "movement_type AS type, description, amount, " +
           "COALESCE(metadata->>'currency', :currency) AS currency, " +
           "movement_date AS transactionDate, COALESCE(metadata->>'notes', '') AS notes, " +
           "created_at AS createdAt, updated_at AS updatedAt, deleted_at AS deletedAt, " +
           "CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(row_version, 1) AS version", nativeQuery = true)
    Object[] updateTransaction(@Param("id") UUID id, @Param("userId") UUID userId,
                              @Param("accountId") UUID accountId,
                              @Param("transferAccountId") UUID transferAccountId,
                              @Param("categoryId") UUID categoryId, @Param("type") String type,
                              @Param("amount") BigDecimal amount,
                              @Param("description") String description,
                              @Param("currency") String currency,
                              @Param("transactionDate") LocalDate transactionDate,
                              @Param("notes") String notes);
    
    @Modifying
    @Query(value = "UPDATE movements SET deleted_at = NOW(), updated_at = NOW(), " +
           "row_version = COALESCE(row_version, 0) + 1 WHERE id = :id AND user_id = :userId", 
           nativeQuery = true)
    int softDelete(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query(value = "SELECT COUNT(1) FROM movements WHERE id = :id AND user_id = :userId", 
           nativeQuery = true)
    int existsByUserAndId(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query(value = "SELECT COALESCE(SUM(CASE WHEN movement_type = 'income' THEN amount ELSE 0 END), 0) AS income, " +
           "COALESCE(SUM(CASE WHEN movement_type = 'expense' THEN amount ELSE 0 END), 0) AS expenses, " +
           "COALESCE(SUM(CASE WHEN movement_type = 'payment' THEN amount ELSE 0 END), 0) AS debtPayments, " +
           "COALESCE(SUM(CASE WHEN movement_type IN ('expense', 'payment') THEN amount ELSE 0 END), 0) AS fixedPayments " +
           "FROM movements WHERE user_id = :userId AND deleted_at IS NULL " +
           "AND (:from IS NULL OR movement_date >= CAST(:from AS date)) " +
           "AND (:to IS NULL OR movement_date <= CAST(:to AS date)) " +
           "AND (:accountId IS NULL OR account_id = CAST(:accountId AS uuid))", nativeQuery = true)
    Object[] getSummary(@Param("userId") UUID userId, @Param("from") LocalDate from,
                       @Param("to") LocalDate to, @Param("accountId") UUID accountId);
    
     @Query(value = "SELECT c.id AS categoryId, c.name AS categoryName, " +
    	       "COALESCE(SUM(m.amount), 0) AS amount FROM categories c " +
    	       "LEFT JOIN movements m ON m.category_id = c.id AND m.user_id = :userId " +
    	       "AND m.deleted_at IS NULL AND m.movement_type IN ('expense', 'payment') " +
    	       "WHERE c.user_id = :userId AND c.deleted_at IS NULL " +
    	       "GROUP BY c.id, c.name ORDER BY amount DESC", nativeQuery = true)
    	List<Object[]> getCategoryStatsAll(@Param("userId") UUID userId);

    	@Query(value = "SELECT c.id AS categoryId, c.name AS categoryName, " +
    	       "COALESCE(SUM(m.amount), 0) AS amount FROM categories c " +
    	       "LEFT JOIN movements m ON m.category_id = c.id AND m.user_id = :userId " +
    	       "AND m.deleted_at IS NULL AND m.movement_type IN ('expense', 'payment') " +
    	       "AND m.movement_date >= :from AND m.movement_date <= :to " +
    	       "WHERE c.user_id = :userId AND c.deleted_at IS NULL " +
    	       "GROUP BY c.id, c.name ORDER BY amount DESC", nativeQuery = true)
    	List<Object[]> getCategoryStatsByDateRange(@Param("userId") UUID userId, 
    	                                           @Param("from") LocalDate from, 
    	                                           @Param("to") LocalDate to);
    
    @Query(value = "SELECT id, deleted_at AS deletedAt FROM movements WHERE user_id = :userId " +
           "AND deleted_at IS NOT NULL AND deleted_at >= COALESCE(:since, deleted_at) " +
           "ORDER BY deleted_at DESC", nativeQuery = true)
    List<Map<String, Object>> findDeleted(@Param("userId") UUID userId, @Param("since") Instant since);
    @Query(value = "SELECT COALESCE(SUM(CASE WHEN movement_type = 'income' THEN amount ELSE 0 END), 0) AS income, " +
    	       "COALESCE(SUM(CASE WHEN movement_type = 'expense' THEN amount ELSE 0 END), 0) AS expenses, " +
    	       "COALESCE(SUM(CASE WHEN movement_type = 'payment' THEN amount ELSE 0 END), 0) AS debtPayments, " +
    	       "COALESCE(SUM(CASE WHEN movement_type IN ('expense', 'payment') THEN amount ELSE 0 END), 0) AS fixedPayments " +
    	       "FROM movements WHERE user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
    	Object[] getSummaryAll(@Param("userId") UUID userId);

    @Query(value = "SELECT " +
    	       "COALESCE(SUM(CASE WHEN movement_type = 'income' THEN amount ELSE 0 END), 0) AS income, " +
    	       "COALESCE(SUM(CASE WHEN movement_type IN ('expense', 'payment') THEN amount ELSE 0 END), 0) AS expenses, " +
    	       "COALESCE(SUM(CASE WHEN movement_type = 'payment' THEN amount ELSE 0 END), 0) AS debtPayments, " +
    	       "0 AS fixedPayments " +
    	       "FROM movements WHERE user_id = :userId AND deleted_at IS NULL " +
    	       "AND movement_date >= :from AND movement_date <= :to", nativeQuery = true)
    	Object[] getSummaryByDateRange(@Param("userId") UUID userId, 
    	                               @Param("from") LocalDate from, 
    	                               @Param("to") LocalDate to);

    	@Query(value = "SELECT COALESCE(SUM(CASE WHEN movement_type = 'income' THEN amount ELSE 0 END), 0) AS income, " +
    	       "COALESCE(SUM(CASE WHEN movement_type = 'expense' THEN amount ELSE 0 END), 0) AS expenses, " +
    	       "COALESCE(SUM(CASE WHEN movement_type = 'payment' THEN amount ELSE 0 END), 0) AS debtPayments, " +
    	       "COALESCE(SUM(CASE WHEN movement_type IN ('expense', 'payment') THEN amount ELSE 0 END), 0) AS fixedPayments " +
    	       "FROM movements WHERE user_id = :userId AND deleted_at IS NULL " +
    	       "AND account_id = :accountId", nativeQuery = true)
    	Object[] getSummaryByAccount(@Param("userId") UUID userId, @Param("accountId") UUID accountId);

    	@Query(value = "SELECT COALESCE(SUM(CASE WHEN movement_type = 'income' THEN amount ELSE 0 END), 0) AS income, " +
    	       "COALESCE(SUM(CASE WHEN movement_type = 'expense' THEN amount ELSE 0 END), 0) AS expenses, " +
    	       "COALESCE(SUM(CASE WHEN movement_type = 'payment' THEN amount ELSE 0 END), 0) AS debtPayments, " +
    	       "COALESCE(SUM(CASE WHEN movement_type IN ('expense', 'payment') THEN amount ELSE 0 END), 0) AS fixedPayments " +
    	       "FROM movements WHERE user_id = :userId AND deleted_at IS NULL " +
    	       "AND movement_date >= :from AND movement_date <= :to AND account_id = :accountId", nativeQuery = true)
    	Object[] getSummaryFull(@Param("userId") UUID userId, 
    	                        @Param("from") LocalDate from, 
    	                        @Param("to") LocalDate to, 
    	                        @Param("accountId") UUID accountId);
    	@Query(value = "SELECT DATE_TRUNC('month', movement_date) as month, " +
    		       "SUM(CASE WHEN movement_type = 'income' THEN amount ELSE 0 END) as income, " +
    		       "SUM(CASE WHEN movement_type IN ('expense', 'payment') THEN amount ELSE 0 END) as expenses " +
    		       "FROM movements WHERE user_id = :userId AND deleted_at IS NULL " +
    		       "AND movement_date >= :startDate " +
    		       "GROUP BY DATE_TRUNC('month', movement_date) " +
    		       "ORDER BY month DESC", nativeQuery = true)
    	List<Object[]> getMonthlyReport(@Param("userId") UUID userId, 
    		                            @Param("startDate") LocalDate startDate);
    	// Agregar estos métodos al MovementRepository existente

        @Query(value = "SELECT COALESCE(SUM(CASE WHEN m.movement_type = 'income' THEN m.amount ELSE 0 END), 0) as total_income, " +
               "COALESCE(SUM(CASE WHEN m.movement_type IN ('expense', 'payment') THEN m.amount ELSE 0 END), 0) as total_expenses, " +
               "COALESCE(SUM(CASE WHEN m.movement_type = 'payment' THEN m.amount ELSE 0 END), 0) as debt_payments, " +
               "COALESCE(SUM(CASE WHEN m.movement_type = 'fixed_payment' THEN m.amount ELSE 0 END), 0) as fixed_payments " +
               "FROM movements m " +
               "WHERE m.user_id = :userId " +
               "AND m.movement_date BETWEEN :startDate AND :endDate " +
               "AND m.deleted_at IS NULL " +
               "AND m.account_id IN (SELECT id FROM accounts WHERE user_id = :userId AND type = 'debit')", nativeQuery = true)
        Object[] getSummaryByDateRangeFromDebitAccount(@Param("userId") UUID userId, 
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);
        
        @Query(value = "SELECT COALESCE(SUM(CASE WHEN m.movement_type = 'income' THEN m.amount ELSE 0 END), 0) as total_income, " +
               "COALESCE(SUM(CASE WHEN m.movement_type IN ('expense', 'payment') THEN m.amount ELSE 0 END), 0) as total_expenses, " +
               "COALESCE(SUM(CASE WHEN m.movement_type = 'payment' THEN m.amount ELSE 0 END), 0) as debt_payments, " +
               "COALESCE(SUM(CASE WHEN m.movement_type = 'fixed_payment' THEN m.amount ELSE 0 END), 0) as fixed_payments " +
               "FROM movements m " +
               "WHERE m.user_id = :userId " +
               "AND m.movement_date BETWEEN :startDate AND :endDate " +
               "AND m.deleted_at IS NULL " +
               "AND m.account_id = :accountId", nativeQuery = true)
        Object[] getSummaryByDateRangeAndAccount(@Param("userId") UUID userId, 
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("accountId") UUID accountId);
}