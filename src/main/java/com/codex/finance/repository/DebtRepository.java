package com.codex.finance.repository;

import com.codex.finance.entity.Debt;
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
public interface DebtRepository extends JpaRepository<Debt, UUID> {

	@Query(value = "SELECT d.id, d.user_id AS userId, d.name, "
			+ "d.remaining_balance AS principalBalance, d.fixed_payment AS installment, "
			+ "COALESCE(d.metadata->>'frequency', 'monthly') AS frequency, "
			+ "COALESCE((d.metadata->>'nextDueDate')::date, NULL) AS nextDueDate, "
			+ "COALESCE(d.metadata->>'notes', '') AS notes, d.created_at AS createdAt, "
			+ "d.updated_at AS updatedAt, d.deleted_at AS deletedAt, "
			+ "CASE WHEN d.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, "
			+ "COALESCE(d.row_version, 1) AS version "
			+ "FROM debts d WHERE d.user_id = :userId AND d.deleted_at IS NULL "
			+ "ORDER BY d.created_at DESC", nativeQuery = true)
	List<Object[]> listDebts(@Param("userId") UUID userId);

	@Modifying
	@Query(value = "INSERT INTO debts (user_id, name, debt_type, original_amount, remaining_balance, "
			+ "fixed_payment, start_date, metadata, created_at, updated_at, row_version) "
			+ "VALUES (:userId, :name, CAST('loan' AS public.debt_type), :principalBalance, :principalBalance, "
			+ ":installment, COALESCE(CAST(:nextDueDate AS date), CURRENT_DATE), "
			+ "jsonb_build_object('frequency', :frequency, 'nextDueDate', :nextDueDate, 'notes', :notes), "
			+ "NOW(), NOW(), 1) RETURNING id, user_id AS userId, name, "
			+ "remaining_balance AS principalBalance, fixed_payment AS installment, "
			+ "COALESCE(metadata->>'frequency', 'monthly') AS frequency, "
			+ "COALESCE((metadata->>'nextDueDate')::date, NULL) AS nextDueDate, "
			+ "COALESCE(metadata->>'notes', '') AS notes, created_at AS createdAt, "
			+ "updated_at AS updatedAt, deleted_at AS deletedAt, "
			+ "CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, "
			+ "COALESCE(row_version, 1) AS version", nativeQuery = true)
	Object[] createDebt(@Param("userId") UUID userId, @Param("name") String name,
			@Param("principalBalance") BigDecimal principalBalance, @Param("installment") BigDecimal installment,
			@Param("frequency") String frequency, @Param("nextDueDate") String nextDueDate,
			@Param("notes") String notes);

	@Modifying
	@Query(value = "UPDATE debts SET name = :name, remaining_balance = :principalBalance, "
			+ "fixed_payment = :installment, "
			+ "metadata = jsonb_build_object('frequency', :frequency, 'nextDueDate', :nextDueDate, 'notes', :notes), "
			+ "updated_at = NOW(), row_version = COALESCE(row_version, 0) + 1 "
			+ "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL "
			+ "RETURNING id, user_id AS userId, name, remaining_balance AS principalBalance, "
			+ "fixed_payment AS installment, COALESCE(metadata->>'frequency', 'monthly') AS frequency, "
			+ "COALESCE((metadata->>'nextDueDate')::date, NULL) AS nextDueDate, "
			+ "COALESCE(metadata->>'notes', '') AS notes, created_at AS createdAt, "
			+ "updated_at AS updatedAt, deleted_at AS deletedAt, "
			+ "CASE WHEN deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, "
			+ "COALESCE(row_version, 1) AS version", nativeQuery = true)
	Object[] updateDebt(@Param("id") UUID id, @Param("userId") UUID userId, @Param("name") String name,
			@Param("principalBalance") BigDecimal principalBalance, @Param("installment") BigDecimal installment,
			@Param("frequency") String frequency, @Param("nextDueDate") String nextDueDate,
			@Param("notes") String notes);

	@Modifying
	@Query(value = "UPDATE debts SET deleted_at = NOW(), updated_at = NOW(), "
			+ "row_version = COALESCE(row_version, 0) + 1 WHERE id = :id AND user_id = :userId", nativeQuery = true)
	int softDelete(@Param("id") UUID id, @Param("userId") UUID userId);

	@Query(value = "SELECT COUNT(1) FROM debts WHERE id = :id AND user_id = :userId", nativeQuery = true)
	int existsByUserAndId(@Param("id") UUID id, @Param("userId") UUID userId);

	@Query(value = "SELECT id, deleted_at AS deletedAt FROM debts WHERE user_id = :userId "
			+ "AND deleted_at IS NOT NULL AND deleted_at >= COALESCE(:since, deleted_at) "
			+ "ORDER BY deleted_at DESC", nativeQuery = true)
	List<Map<String, Object>> findDeleted(@Param("userId") UUID userId, @Param("since") Instant since);

	@Query(value = "SELECT d.id, d.name, " +
		       "COALESCE((d.metadata->>'nextDueDate')::date, CURRENT_DATE) AS dueDate, " +
		       "d.remaining_balance AS amount " +
		       "FROM debts d WHERE d.user_id = :userId AND d.deleted_at IS NULL " +
		       "ORDER BY dueDate ASC", nativeQuery = true)
		List<Object[]> getUpcomingDebts(@Param("userId") UUID userId);

	@Query(value = "SELECT d.id, d.user_id AS userId, d.name, "
			+ "d.remaining_balance AS principalBalance, d.fixed_payment AS installment, "
			+ "COALESCE(d.metadata->>'frequency', 'monthly') AS frequency, "
			+ "COALESCE((d.metadata->>'nextDueDate')::date, NULL) AS nextDueDate, "
			+ "COALESCE(d.metadata->>'notes', '') AS notes, d.created_at AS createdAt, "
			+ "d.updated_at AS updatedAt, d.deleted_at AS deletedAt "
			+ "FROM debts d WHERE d.user_id = :userId AND d.id = :id AND d.deleted_at IS NULL", nativeQuery = true)
	Object[] getDebtById(@Param("userId") UUID userId, @Param("id") UUID id);

	@Query(value = "SELECT d.id, d.name, " + "COALESCE(d.metadata->>'frequency', 'monthly') AS frequency, "
			+ "COALESCE((d.metadata->>'nextDueDate')::date, CURRENT_DATE) AS next_due_date, "
			+ "LEAST(COALESCE(d.fixed_payment, d.remaining_balance), d.remaining_balance) AS amount, "
			+ "d.remaining_balance AS remaining_balance " + "FROM debts d "
			+ "WHERE d.user_id = :userId AND d.deleted_at IS NULL "
			+ "AND (d.metadata->>'nextDueDate')::date BETWEEN :startDate AND :endDate "
			+ "ORDER BY next_due_date ASC", nativeQuery = true)
	List<Object[]> getUpcomingDebtsInRange(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);
}