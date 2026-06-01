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

import com.codex.finance.entity.Installment;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, UUID> {
    
    @Query(value = "SELECT i.id, i.debt_id, i.number, i.amount, i.due_date, i.paid, i.paid_at, " +
           "i.payment_movement_id, i.created_at, i.updated_at, i.deleted_at, " +
           "CASE WHEN i.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(i.row_version, 1) AS version " +
           "FROM installments i " +
           "WHERE i.user_id = :userId " +
           "AND i.deleted_at IS NULL " +
           "ORDER BY i.due_date ASC " +
           "LIMIT :limit", nativeQuery = true)
    List<Object[]> listInstallments(@Param("userId") UUID userId, @Param("limit") int limit);
    
    @Query(value = "SELECT i.id, i.debt_id, i.number, i.amount, i.due_date, i.paid, i.paid_at, " +
           "i.payment_movement_id, i.created_at, i.updated_at, i.deleted_at, " +
           "CASE WHEN i.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(i.row_version, 1) AS version " +
           "FROM installments i " +
           "WHERE i.user_id = :userId " +
           "AND i.debt_id = :debtId " +
           "AND i.deleted_at IS NULL " +
           "ORDER BY i.number ASC", nativeQuery = true)
    List<Object[]> listInstallmentsByDebt(@Param("userId") UUID userId, @Param("debtId") UUID debtId);
    
    @Query(value = "SELECT i.id, i.debt_id, i.number, i.amount, i.due_date, i.paid, i.paid_at, " +
           "i.payment_movement_id, i.created_at, i.updated_at, i.deleted_at, " +
           "CASE WHEN i.deleted_at IS NULL THEN 'synced' ELSE 'deleted' END AS syncStatus, " +
           "COALESCE(i.row_version, 1) AS version " +
           "FROM installments i " +
           "WHERE i.user_id = :userId " +
           "AND i.id = :id " +
           "AND i.deleted_at IS NULL", nativeQuery = true)
    Object[] getInstallmentById(@Param("userId") UUID userId, @Param("id") UUID id);
    
    @Modifying
    @Query(value = "INSERT INTO installments (id, debt_id, user_id, number, amount, due_date, paid, created_at, updated_at, row_version) " +
           "VALUES (gen_random_uuid(), :debtId, :userId, :number, :amount, :dueDate, :paid, NOW(), NOW(), 1) " +
           "RETURNING id", nativeQuery = true)
    void createInstallment(@Param("userId") UUID userId, 
                          @Param("debtId") UUID debtId,
                          @Param("number") Integer number,
                          @Param("amount") BigDecimal amount,
                          @Param("dueDate") LocalDate dueDate,
                          @Param("paid") Boolean paid);
    
    @Modifying
    @Query(value = "UPDATE installments SET " +
           "number = COALESCE(:number, number), " +
           "amount = COALESCE(:amount, amount), " +
           "due_date = COALESCE(:dueDate, due_date), " +
           "paid = COALESCE(:paid, paid), " +
           "updated_at = NOW(), " +
           "row_version = row_version + 1 " +
           "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
    void updateInstallment(@Param("userId") UUID userId,
                           @Param("id") UUID id,
                           @Param("number") Integer number,
                           @Param("amount") BigDecimal amount,
                           @Param("dueDate") LocalDate dueDate,
                           @Param("paid") Boolean paid);
    
    @Modifying
    @Query(value = "UPDATE installments SET paid = true, paid_at = NOW(), updated_at = NOW() " +
           "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL AND paid = false", nativeQuery = true)
    int markAsPaid(@Param("userId") UUID userId, @Param("id") UUID id);
    
    @Modifying
    @Query(value = "UPDATE installments SET deleted_at = NOW(), updated_at = NOW() " +
           "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteInstallment(@Param("userId") UUID userId, @Param("id") UUID id);
    
    @Query(value = "SELECT COUNT(*) FROM installments " +
           "WHERE debt_id = :debtId AND user_id = :userId AND paid = false AND deleted_at IS NULL", nativeQuery = true)
    int countUnpaidInstallments(@Param("userId") UUID userId, @Param("debtId") UUID debtId);
}