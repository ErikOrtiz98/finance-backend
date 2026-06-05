package com.codex.finance.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codex.finance.entity.Budget;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

	@Query(value = "SELECT b.id, b.category_id, c.name as category_name, b.budget_period as period, "
			+ "b.period_start, b.period_end, b.amount_limit, b.alert_threshold, "
			+ "COALESCE(SUM(m.amount), 0) as spent_amount, "
			+ "ROUND((COALESCE(SUM(m.amount), 0) / NULLIF(b.amount_limit, 0)) * 100, 2) as usage_percent, "
			+ "CASE WHEN COALESCE(SUM(m.amount), 0) / NULLIF(b.amount_limit, 0) >= b.alert_threshold THEN true ELSE false END as is_alert "
			+ "FROM budgets b " + "LEFT JOIN movements m ON m.category_id = b.category_id "
			+ "AND m.movement_type IN ('expense', 'payment') "
			+ "AND m.movement_date BETWEEN b.period_start AND b.period_end " + "AND m.deleted_at IS NULL "
			+ "LEFT JOIN categories c ON c.id = b.category_id " + "WHERE b.user_id = :userId AND b.deleted_at IS NULL "
			+ "AND b.period_start <= CURRENT_DATE AND b.period_end >= CURRENT_DATE "
			+ "GROUP BY b.id, c.name, b.category_id, b.budget_period, b.period_start, b.period_end, b.amount_limit, b.alert_threshold", nativeQuery = true)
	List<Object[]> listActiveBudgets(@Param("userId") UUID userId);

	@Query(value = "SELECT b.id, b.category_id, c.name as category_name, b.budget_period as period, "
			+ "b.period_start, b.period_end, b.amount_limit, b.alert_threshold " + "FROM budgets b "
			+ "LEFT JOIN categories c ON c.id = b.category_id " + "WHERE b.user_id = :userId AND b.deleted_at IS NULL "
			+ "ORDER BY b.period_start DESC", nativeQuery = true)
	List<Object[]> listAllBudgets(@Param("userId") UUID userId);

	@Query(value = "SELECT b.id, b.category_id, c.name as category_name, b.budget_period as period, " +
		       "b.period_start, b.period_end, b.amount_limit, b.alert_threshold, " +
		       "COALESCE(SUM(m.amount), 0) as spent_amount, " +
		       "ROUND((COALESCE(SUM(m.amount), 0) / NULLIF(b.amount_limit, 0)) * 100, 2) as usage_percent, " +
		       "CASE WHEN COALESCE(SUM(m.amount), 0) / NULLIF(b.amount_limit, 0) >= b.alert_threshold THEN true ELSE false END as is_alert " +
		       "FROM budgets b " +
		       "LEFT JOIN movements m ON m.category_id = b.category_id " +
		       "AND m.movement_type IN ('expense', 'payment') " +
		       "AND m.movement_date BETWEEN b.period_start AND b.period_end " +
		       "AND m.deleted_at IS NULL " +
		       "LEFT JOIN categories c ON c.id = b.category_id " +
		       "WHERE b.user_id = :userId AND b.id = :id AND b.deleted_at IS NULL " +
		       "GROUP BY b.id, c.name, b.budget_period, b.period_start, b.period_end, b.amount_limit, b.alert_threshold", nativeQuery = true)
		Object[] getBudgetById(@Param("userId") UUID userId, @Param("id") UUID id);

	@Modifying
	@Query(value = "INSERT INTO budgets (id, user_id, category_id, budget_period, period_start, period_end, amount_limit, alert_threshold, created_at, updated_at) "
			+ "VALUES (gen_random_uuid(), :userId, :categoryId, CAST(:period AS public.budget_period), :periodStart, :periodEnd, :amountLimit, :alertThreshold, NOW(), NOW())", nativeQuery = true)
	void createBudget(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId,
			@Param("period") String period, @Param("periodStart") LocalDate periodStart,
			@Param("periodEnd") LocalDate periodEnd, @Param("amountLimit") BigDecimal amountLimit,
			@Param("alertThreshold") BigDecimal alertThreshold);

	@Modifying
	@Query(value = "UPDATE budgets SET " +
	       "category_id = COALESCE(:categoryId, category_id), " +
	       "budget_period = COALESCE(CAST(:period AS public.budget_period), budget_period), " +
	       "period_start = COALESCE(:periodStart, period_start), " +
	       "period_end = COALESCE(:periodEnd, period_end), " +
	       "amount_limit = COALESCE(:amountLimit, amount_limit), " +
	       "alert_threshold = COALESCE(:alertThreshold, alert_threshold), " +
	       "updated_at = NOW() " +
	       "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
	void updateBudget(@Param("userId") UUID userId,
	                  @Param("id") UUID id,
	                  @Param("categoryId") UUID categoryId,
	                  @Param("period") String period,
	                  @Param("periodStart") LocalDate periodStart,
	                  @Param("periodEnd") LocalDate periodEnd,
	                  @Param("amountLimit") BigDecimal amountLimit,
	                  @Param("alertThreshold") BigDecimal alertThreshold);

	@Modifying
	@Query(value = "UPDATE budgets SET deleted_at = NOW(), updated_at = NOW() "
			+ "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
	int softDeleteBudget(@Param("userId") UUID userId, @Param("id") UUID id);

	@Query(value = "SELECT COUNT(*) FROM budgets "
			+ "WHERE user_id = :userId AND category_id = :categoryId AND deleted_at IS NULL "
			+ "AND period_start <= CURRENT_DATE AND period_end >= CURRENT_DATE", nativeQuery = true)
	int countActiveBudgetForCategory(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId);
}