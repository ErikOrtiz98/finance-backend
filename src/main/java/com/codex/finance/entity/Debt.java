package com.codex.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "debts")
public class Debt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;
    
    @Column(name = "account_id", columnDefinition = "uuid")
    private UUID accountId;
    
    @Column(columnDefinition = "citext")
    private String name;
    
    @Column(name = "debt_type", columnDefinition = "public.debt_type")
    private String debtType;
    
    @Column(name = "original_amount", precision = 18, scale = 2)
    private BigDecimal originalAmount;
    
    @Column(name = "remaining_balance", precision = 18, scale = 2)
    private BigDecimal remainingBalance;
    
    @Column(name = "interest_rate", precision = 7, scale = 4)
    private BigDecimal interestRate;
    
    @Column(name = "fixed_payment", precision = 18, scale = 2)
    private BigDecimal fixedPayment;
    
    @Column(name = "minimum_payment", precision = 18, scale = 2)
    private BigDecimal minimumPayment;
    
    @Column(name = "payment_to_avoid_interest", precision = 18, scale = 2)
    private BigDecimal paymentToAvoidInterest;
    
    @Column(name = "total_installments")
    private Integer totalInstallments;
    
    @Column(name = "remaining_installments")
    private Integer remainingInstallments;
    
    @Column(name = "statement_close_day", columnDefinition = "smallint")
    private Integer statementCloseDay;
    
    @Column(name = "due_day", columnDefinition = "smallint")
    private Integer dueDay;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    
    @Column(name = "row_version")
    private Long rowVersion;

    // Getters y Setters (todos los campos)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDebtType() { return debtType; }
    public void setDebtType(String debtType) { this.debtType = debtType; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public BigDecimal getFixedPayment() { return fixedPayment; }
    public void setFixedPayment(BigDecimal fixedPayment) { this.fixedPayment = fixedPayment; }
    public BigDecimal getMinimumPayment() { return minimumPayment; }
    public void setMinimumPayment(BigDecimal minimumPayment) { this.minimumPayment = minimumPayment; }
    public BigDecimal getPaymentToAvoidInterest() { return paymentToAvoidInterest; }
    public void setPaymentToAvoidInterest(BigDecimal paymentToAvoidInterest) { this.paymentToAvoidInterest = paymentToAvoidInterest; }
    public Integer getTotalInstallments() { return totalInstallments; }
    public void setTotalInstallments(Integer totalInstallments) { this.totalInstallments = totalInstallments; }
    public Integer getRemainingInstallments() { return remainingInstallments; }
    public void setRemainingInstallments(Integer remainingInstallments) { this.remainingInstallments = remainingInstallments; }
    public Integer getStatementCloseDay() { return statementCloseDay; }
    public void setStatementCloseDay(Integer statementCloseDay) { this.statementCloseDay = statementCloseDay; }
    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Long getRowVersion() { return rowVersion; }
    public void setRowVersion(Long rowVersion) { this.rowVersion = rowVersion; }
}