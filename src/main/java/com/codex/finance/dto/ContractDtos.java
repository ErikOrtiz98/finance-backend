package com.codex.finance.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ContractDtos {
	private ContractDtos() {
	}

	public enum SyncStatus {
		pending, synced, conflict, failed, deleted
	}

	public record UserDto(String id, String email, String displayName) {
	}

	public record SessionDto(String accessToken, String refreshToken, Instant expiresAt) {
	}

	public record AuthResponse(UserDto user, SessionDto session) {
	}

	public record SignUpRequest(@Email @NotBlank String email, @NotBlank String password,
			@NotBlank String displayName) {
	}

	public record SignInRequest(@Email @NotBlank String email, @NotBlank String password) {
	}

	public record RefreshRequest(@NotBlank String refreshToken) {
	}

	public record SignOutRequest(@NotBlank String refreshToken) {
	}

	public record SessionResponse(UserDto user, SessionDto session) {
	}

	public record MeResponse(String id, String userId, String displayName, String currency, String payCycle,
			List<Integer> payDays, BigDecimal monthlyIncome, String mainAccountId, Instant createdAt, Instant updatedAt,
			Instant deletedAt, SyncStatus syncStatus, long version) {
	}

	public record UpdateMeRequest(@NotBlank String displayName, @NotBlank String currency, @NotBlank String payCycle,
			@NotEmpty List<Integer> payDays, BigDecimal monthlyIncome, String mainAccountId) {
	}

	public record CategoryResponse(String id, String userId, String name, String color, String icon, String type,
			Instant createdAt, Instant updatedAt, Instant deletedAt, SyncStatus syncStatus, long version) {
	}

	public record UpsertCategoryRequest(@NotBlank String name, String color, String icon, @NotBlank String type) {
	}

	public record AccountResponse(String id, String userId, String type, String name, String institution,
			String currency, BigDecimal balance, BigDecimal creditLimit, Integer closingDay, Integer dueDay,
			boolean active, Instant createdAt, Instant updatedAt, Instant deletedAt, SyncStatus syncStatus,
			long version) {
	}

	public record UpsertAccountRequest(@NotBlank String type, @NotBlank String name, String institution,
			@NotBlank String currency, BigDecimal balance, BigDecimal creditLimit, Integer closingDay, Integer dueDay,
			Boolean active) {
	}

	public record TransactionResponse(String id, String userId, String accountId, String transferAccountId,
			String categoryId, String type, String description, BigDecimal amount, String currency,
			LocalDate transactionDate, String notes, Instant createdAt, Instant updatedAt, Instant deletedAt,
			SyncStatus syncStatus, long version) {
	}

	public record UpsertTransactionRequest(@NotBlank String accountId, String transferAccountId, String categoryId,
			String debtId, @NotBlank String type, String description, @NotNull BigDecimal amount,
			@NotBlank String currency, @NotNull LocalDate transactionDate, String notes) {
	}

	public record TransactionFilters(LocalDate from, LocalDate to, String accountId, String categoryId, String type,
			Integer limit, Integer offset, String cursor) {
	}

	public record DebtResponse(String id, String userId, String name, BigDecimal principalBalance,  BigDecimal remainingBalance,
			BigDecimal installment, String frequency, LocalDate nextDueDate, String notes, Instant createdAt,
			Instant updatedAt, Instant deletedAt, SyncStatus syncStatus, long version) {
	}

	public record UpsertDebtRequest(@NotBlank String name, @NotNull BigDecimal principalBalance, BigDecimal installment,
			@NotBlank String frequency, LocalDate nextDueDate, String notes) {
	}

	public record RecurringPaymentResponse(String id, String userId, String name, BigDecimal amount, String currency,
			String frequency, LocalDate nextDueDate, String categoryId, Instant createdAt, Instant updatedAt,
			Instant deletedAt, SyncStatus syncStatus, long version) {
	}

	public record SyncEntityChange(@NotBlank String entity, @NotBlank String op, JsonNode record) {
	}

	public record SyncPullResponse(Instant serverTime, List<SyncEntityChange> changes,
			List<Map<String, Object>> deleted) {
	}

	public record SyncPushRequest(@NotBlank String deviceId, @NotEmpty List<SyncEntityChange> changes) {
	}

	public record SyncPushResponse(int accepted, int rejected, List<Map<String, Object>> conflicts) {
	}

	public record SyncConflictResolutionRequest(@NotBlank String entity, @NotBlank String id,
			@NotBlank String resolution) {
	}

	public record SummaryResponse(BigDecimal income, BigDecimal expenses, BigDecimal fixedPayments,
			BigDecimal debtPayments,BigDecimal totalRemainingDebt, BigDecimal availableBalance, String currency) {
	}

	public record CategoryStatResponse(String categoryId, String categoryName, BigDecimal amount,
			BigDecimal percentage) {
	}

	public record UpcomingItemResponse(String type, String id, String name, LocalDate dueDate, BigDecimal amount) {
	}

	public record UpcomingResponse(List<UpcomingItemResponse> next7Days) {
	}

	public record BackupExportResponse(String fileName, String downloadUrl) {
	}

	public record BackupImportRequest(@NotBlank String format, boolean encrypted, JsonNode payload) {
	}

	public record MigrationImportRequest(@NotBlank String source, int schemaVersion, JsonNode payload) {
	}

	public record MigrationImportResponse(boolean imported, int recordsCreated, Instant migrationCompletedAt) {
	}

	// List Installments Request
	public record InstallmentFilters(String debtId, Boolean paid, LocalDate dueDateFrom, LocalDate dueDateTo,
			Integer limit, String cursor) {
	}

	// Financial Goal Response
	public record FinancialGoalResponse(String id, String name, BigDecimal targetAmount, BigDecimal currentProgress,
			LocalDate targetDate, String status, Instant createdAt, Instant updatedAt, BigDecimal progressPercentage) {
	}

	// Upsert Financial Goal Request
	public record UpsertFinancialGoalRequest(@NotBlank String name, @NotNull BigDecimal targetAmount,
			BigDecimal currentProgress, LocalDate targetDate, String status) {
	}

	// Budget Response
	public record BudgetResponse(String id, String categoryId, String categoryName, String period,
			LocalDate periodStart, LocalDate periodEnd, BigDecimal amountLimit, BigDecimal alertThreshold,
			BigDecimal spentAmount, BigDecimal usagePercentage, boolean isAlert) {
	}

	// Upsert Budget Request
	public record UpsertBudgetRequest(@NotBlank String categoryId, @NotBlank String period,
			@NotNull LocalDate periodStart, @NotNull LocalDate periodEnd, @NotNull BigDecimal amountLimit,
			BigDecimal alertThreshold) {
	}

	// Report Response
	public record MonthlyReportResponse(String yearMonth, BigDecimal totalIncome, BigDecimal totalExpenses,
			BigDecimal totalSavings, List<CategoryStatResponse> topExpenses) {
	}
	// Agregar estos records al final de ContractDtos.java

	// Para recurrencias con fecha fin
	public record UpsertRecurringPaymentRequest(String name, BigDecimal amount, String currency, String frequency,
			LocalDate nextDueDate, LocalDate endDate, String categoryId, String paymentType // "expense" o "income"
	) {
	}

	// Para sobreendeudamiento
	public record DebtRatioResponse(BigDecimal totalIncome, BigDecimal totalDebtPayments, BigDecimal debtToIncomeRatio, // porcentaje
			String riskLevel, // "bajo", "medio", "alto", "crítico"
			String recommendation, String currency) {
	}

	// Para organización quincenal
	public record BiweeklyPaymentItem(String paymentId, String name, BigDecimal amount, LocalDate dueDate,
			String frequency, String paymentType, BigDecimal remainingBalance) {
	}

	public record BiweeklyScheduleResponse(String periodName, // "Primera quincena" o "Segunda quincena"
			LocalDate startDate, LocalDate endDate, List<BiweeklyPaymentItem> payments, BigDecimal totalAmount,
			BigDecimal availableIncome, BigDecimal remainingAfterPayments) {
	}

	// Para installments con tarjeta de crédito
	public record UpsertInstallmentRequest(String debtId, String accountId, // nuevo: para tarjetas de crédito
			Integer number, BigDecimal amount, LocalDate dueDate, Boolean paid, BigDecimal originalPurchaseAmount,
			BigDecimal interestRate) {
	}

	// En ContractDtos.java, reemplazar el InstallmentResponse existente:

	public record InstallmentResponse(String id, String debtId, Integer number, BigDecimal amount, LocalDate dueDate,
			Boolean paid, Instant paidAt, String paymentMovementId, Instant createdAt, Instant updatedAt,
			Instant deletedAt, SyncStatus syncStatus, long version) {
	}

	public record CreditCardPurchaseRequest(@NotBlank String accountId, // ID de la tarjeta de crédito
			@NotBlank String name, // Nombre de la compra
			@NotNull BigDecimal totalAmount, // Monto total de la compra
			@NotNull Integer months, // Número de meses (3, 6, 12, etc.)
			BigDecimal interestRate, // Tasa de interés (0 = sin intereses)
			@NotNull LocalDate firstDueDate, // Fecha del primer pago
			String categoryId // Categoría opcional
	) {
	}

	public record PayInstallmentRequest(
		    @NotBlank String debitAccountId,
		    @NotBlank String currency,
		    String notes
		) {}
}
