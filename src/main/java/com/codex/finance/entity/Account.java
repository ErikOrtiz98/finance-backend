package com.codex.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;

	@Column(name = "user_id", columnDefinition = "uuid")
	private UUID userId;

	@Column(columnDefinition = "citext")
	private String name;

	@Column(name = "account_type", columnDefinition = "public.account_type")
	private String accountType;

	@Column(name = "bank_name", columnDefinition = "citext")
	private String bankName;

	@Column(name = "current_balance", precision = 18, scale = 2)
	private BigDecimal currentBalance;

	@Column(name = "credit_limit", precision = 18, scale = 2)
	private BigDecimal creditLimit;

	@Column(name = "statement_close_day", columnDefinition = "smallint")
	private Integer statementCloseDay;

	@Column(name = "payment_due_day", columnDefinition = "smallint")
	private Integer paymentDueDay;

	@Column(name = "is_active")
	private Boolean isActive;

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

	// Getters y Setters
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public BigDecimal getCurrentBalance() {
		return currentBalance;
	}

	public void setCurrentBalance(BigDecimal currentBalance) {
		this.currentBalance = currentBalance;
	}

	public BigDecimal getCreditLimit() {
		return creditLimit;
	}

	public void setCreditLimit(BigDecimal creditLimit) {
		this.creditLimit = creditLimit;
	}

	public Integer getStatementCloseDay() {
		return statementCloseDay;
	}

	public void setStatementCloseDay(Integer statementCloseDay) {
		this.statementCloseDay = statementCloseDay;
	}

	public Integer getPaymentDueDay() {
		return paymentDueDay;
	}

	public void setPaymentDueDay(Integer paymentDueDay) {
		this.paymentDueDay = paymentDueDay;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(OffsetDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}

	public Long getRowVersion() {
		return rowVersion;
	}

	public void setRowVersion(Long rowVersion) {
		this.rowVersion = rowVersion;
	}
}