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

import com.codex.finance.entity.FinancialGoal;

@Repository
public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {
    
    @Query(value = "SELECT id, name, target_amount, current_progress, target_date, status, " +
           "created_at, updated_at, " +
           "ROUND((current_progress / NULLIF(target_amount, 0)) * 100, 2) as progress_percentage " +
           "FROM financial_goals WHERE user_id = :userId AND deleted_at IS NULL " +
           "ORDER BY target_date ASC NULLS LAST", nativeQuery = true)
    List<Object[]> listGoals(@Param("userId") UUID userId);
    
    @Query(value = "SELECT id, name, target_amount, current_progress, target_date, status, " +
           "created_at, updated_at, " +
           "ROUND((current_progress / NULLIF(target_amount, 0)) * 100, 2) as progress_percentage " +
           "FROM financial_goals WHERE user_id = :userId AND id = :id AND deleted_at IS NULL", nativeQuery = true)
    Object[] getGoalById(@Param("userId") UUID userId, @Param("id") UUID id);
    
    @Modifying
    @Query(value = "INSERT INTO financial_goals (id, user_id, name, target_amount, " +
           "current_progress, target_date, status, created_at, updated_at) " +
           "VALUES (gen_random_uuid(), :userId, :name, :targetAmount, " +
           "COALESCE(:currentProgress, 0), :targetDate, COALESCE(:status, 'active'), NOW(), NOW())", nativeQuery = true)
    void createGoal(@Param("userId") UUID userId, 
                    @Param("name") String name,
                    @Param("targetAmount") BigDecimal targetAmount,
                    @Param("currentProgress") BigDecimal currentProgress,
                    @Param("targetDate") LocalDate targetDate,
                    @Param("status") String status);

    @Modifying
    @Query(value = "UPDATE financial_goals SET " +
           "name = COALESCE(:name, name), " +
           "target_amount = COALESCE(:targetAmount, target_amount), " +
           "current_progress = COALESCE(:currentProgress, current_progress), " +
           "target_date = COALESCE(:targetDate, target_date), " +
           "status = COALESCE(:status, status), " +
           "updated_at = NOW() " +
           "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
    void updateGoal(@Param("userId") UUID userId,
                    @Param("id") UUID id,
                    @Param("name") String name,
                    @Param("targetAmount") BigDecimal targetAmount,
                    @Param("currentProgress") BigDecimal currentProgress,
                    @Param("targetDate") LocalDate targetDate,
                    @Param("status") String status);
    
    @Modifying
    @Query(value = "UPDATE financial_goals SET deleted_at = NOW(), updated_at = NOW() " +
           "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteGoal(@Param("userId") UUID userId, @Param("id") UUID id);
    
    @Query(value = "SELECT COALESCE(SUM(current_progress), 0) FROM financial_goals " +
           "WHERE user_id = :userId AND status = 'active' AND deleted_at IS NULL", nativeQuery = true)
    BigDecimal getTotalActiveGoalsProgress(@Param("userId") UUID userId);
}