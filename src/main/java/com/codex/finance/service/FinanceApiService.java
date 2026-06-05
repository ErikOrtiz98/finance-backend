package com.codex.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codex.finance.client.SupabaseAuthClient;
import com.codex.finance.dto.ContractDtos;
import com.codex.finance.exception.ApiException;
import com.codex.finance.mapper.FinanceMapper;
import com.codex.finance.repository.AccountRepository;
import com.codex.finance.repository.BudgetRepository;
import com.codex.finance.repository.CategoryRepository;
import com.codex.finance.repository.DebtRepository;
import com.codex.finance.repository.FinancialGoalRepository;
import com.codex.finance.repository.InstallmentRepository;
import com.codex.finance.repository.MovementRepository;
import com.codex.finance.repository.ProfileRepository;
import com.codex.finance.repository.ScheduledPaymentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional
public class FinanceApiService {

	@PersistenceContext
	private EntityManager entityManager;

	private final ProfileRepository profileRepo;
	private final CategoryRepository categoryRepo;
	private final AccountRepository accountRepo;
	private final MovementRepository movementRepo;
	private final DebtRepository debtRepo;
	private final ScheduledPaymentRepository scheduledPaymentRepo;
	private final SupabaseAuthClient authClient;
	private final ObjectMapper objectMapper;
	private final FinanceMapper mapper;
	private final JdbcTemplate jdbcTemplate;
	private final InstallmentRepository installmentRepo;
	private final FinancialGoalRepository financialGoalRepo;
	private final BudgetRepository budgetRepo;

	public FinanceApiService(ProfileRepository profileRepo, CategoryRepository categoryRepo,
			AccountRepository accountRepo, MovementRepository movementRepo, DebtRepository debtRepo,
			ScheduledPaymentRepository scheduledPaymentRepo, SupabaseAuthClient authClient, ObjectMapper objectMapper,
			FinanceMapper mapper, JdbcTemplate jdbcTemplate, InstallmentRepository installmentRepo,
			FinancialGoalRepository financialGoalRepo, BudgetRepository budgetRepo) {
		this.profileRepo = profileRepo;
		this.categoryRepo = categoryRepo;
		this.accountRepo = accountRepo;
		this.movementRepo = movementRepo;
		this.debtRepo = debtRepo;
		this.scheduledPaymentRepo = scheduledPaymentRepo;
		this.authClient = authClient;
		this.objectMapper = objectMapper;
		this.mapper = mapper;
		this.jdbcTemplate = jdbcTemplate;
		this.installmentRepo = installmentRepo;
		this.financialGoalRepo = financialGoalRepo;
		this.budgetRepo = budgetRepo;
	}

	private void setAuthContext(String userId) {
		if (entityManager != null) {
			entityManager.createNativeQuery("SELECT set_config('request.jwt.claim.sub', :userId, true)")
				.setParameter("userId", userId)
				.getSingleResult();
		} else {
			jdbcTemplate.update("SELECT set_config('request.jwt.claim.sub', ?, true)", userId);
		}
	}

	// ==================== AUTH ====================
	public ContractDtos.AuthResponse signUp(ContractDtos.SignUpRequest request) {
		return authClient.signUp(request);
	}

	public ContractDtos.AuthResponse signIn(ContractDtos.SignInRequest request) {
		return authClient.signIn(request);
	}

	public ContractDtos.SessionResponse refresh(ContractDtos.RefreshRequest request) {
		return authClient.refresh(request);
	}

	public void signOut(String accessToken, ContractDtos.SignOutRequest request) {
		if (accessToken == null || accessToken.isBlank())
			throw new ApiException(HttpStatus.BAD_REQUEST, "Authorization header is required");
		authClient.signOut(accessToken);
	}

	public ContractDtos.SessionResponse session(String accessToken, String refreshToken) {
		return authClient.currentSession(accessToken, refreshToken);
	}

	// ==================== PROFILE ====================
	@Transactional(readOnly = true)
	public ContractDtos.MeResponse getMe(String userId) {
		UUID uuid = UUID.fromString(userId);
		Object[] row = profileRepo.getProfile(uuid);
		if (row == null)
			throw new ApiException(HttpStatus.NOT_FOUND, "profile not found");
		return mapper.mapToMeResponse(row);
	}

	public ContractDtos.MeResponse updateMe(String userId, ContractDtos.UpdateMeRequest request) {
		UUID uuid = UUID.fromString(userId);

		String settingsJson = profileRepo.getSettingsJson(uuid);
		Map<String, Object> settings;
		try {
			if (settingsJson != null && !settingsJson.isEmpty()) {
				settings = objectMapper.readValue(settingsJson, new TypeReference<Map<String, Object>>() {
				});
			} else {
				settings = new HashMap<>();
			}
		} catch (Exception e) {
			settings = new HashMap<>();
		}

		if (request.monthlyIncome() != null) {
			settings.put("monthlyIncome", request.monthlyIncome());
		}
		settings.put("payCycle", request.payCycle());
		settings.put("mainAccountId", request.mainAccountId()); // NUEVO

		String payDaysJson = toJson(request.payDays());
		settings.put("payDays", payDaysJson);

		String newSettingsJson;
		try {
			newSettingsJson = objectMapper.writeValueAsString(settings);
		} catch (Exception e) {
			newSettingsJson = "{}";
		}

		Object[] row = profileRepo.upsertProfile(uuid, request.displayName(), request.currency(), newSettingsJson,
				uuid);
		return mapper.mapToMeResponse(row);
	}

	// ==================== CATEGORIES ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.CategoryResponse> listCategories(String userId) {
		UUID uuid = UUID.fromString(userId);
		return categoryRepo.listCategories(uuid).stream().map(mapper::mapToCategoryResponse)
				.collect(Collectors.toList());
	}

	public ContractDtos.CategoryResponse createCategory(String userId, ContractDtos.UpsertCategoryRequest request) {
		UUID uuid = UUID.fromString(userId);
		Object[] row = categoryRepo.createCategory(uuid, request.name(), request.color(), request.icon(),
				request.type());
		return mapper.mapToCategoryResponse(row);
	}

	public ContractDtos.CategoryResponse updateCategory(String userId, String id,
			ContractDtos.UpsertCategoryRequest request) {
		UUID userUuid = UUID.fromString(userId);
		UUID categoryUuid = UUID.fromString(id);
		if (categoryRepo.existsByUserAndId(categoryUuid, userUuid) == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "category not found");
		Object[] row = categoryRepo.updateCategory(categoryUuid, userUuid, request.name(), request.color(),
				request.icon(), request.type());
		return mapper.mapToCategoryResponse(row);
	}

	public void deleteCategory(String userId, String id) {
		UUID userUuid = UUID.fromString(userId);
		UUID categoryUuid = UUID.fromString(id);
		int deleted = categoryRepo.softDelete(categoryUuid, userUuid);
		if (deleted == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "category not found");
	}

	// ==================== ACCOUNTS ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.AccountResponse> listAccounts(String userId) {
		UUID uuid = UUID.fromString(userId);
		String currency = profileRepo.getUserCurrency(uuid);
		return accountRepo.listAccounts(uuid, currency).stream().map(mapper::mapToAccountResponse)
				.collect(Collectors.toList());
	}

	public ContractDtos.AccountResponse createAccount(String userId, ContractDtos.UpsertAccountRequest request) {
		UUID uuid = UUID.fromString(userId);
		Object[] row = accountRepo.createAccount(uuid, request.name(), request.type(), request.institution(),
				request.balance(), request.creditLimit(), request.closingDay(), request.dueDay(), request.active(),
				request.currency());
		return mapper.mapToAccountResponse(row);
	}

	public ContractDtos.AccountResponse updateAccount(String userId, String id,
			ContractDtos.UpsertAccountRequest request) {
		UUID userUuid = UUID.fromString(userId);
		UUID accountUuid = UUID.fromString(id);
		if (accountRepo.existsByUserAndId(accountUuid, userUuid) == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "account not found");
		Object[] result = accountRepo.updateAccount(accountUuid, userUuid, request.name(), request.type(),
				request.institution(), request.balance(), request.creditLimit(), request.closingDay(), request.dueDay(),
				request.active());
		Object[] row = (result.length == 1 && result[0] instanceof Object[]) ? (Object[]) result[0] : result;
		return mapper.mapToAccountResponseUpdate(row, request.currency());
	}

	public void deleteAccount(String userId, String id) {
		UUID userUuid = UUID.fromString(userId);
		UUID accountUuid = UUID.fromString(id);
		int deleted = accountRepo.softDelete(accountUuid, userUuid);
		if (deleted == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "account not found");
	}

	// ==================== TRANSACTIONS ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.TransactionResponse> listTransactions(String userId,
	        ContractDtos.TransactionFilters filters) {
	    UUID uuid = UUID.fromString(userId);
	    String currency = profileRepo.getUserCurrency(uuid);
	    
	    int limit = filters.limit() == null ? 50 : filters.limit();
	    int page = filters.offset() == null ? 0 : filters.offset();
	    int offset = page * limit;
	    
	    UUID accountUuid = filters.accountId() != null ? UUID.fromString(filters.accountId()) : null;
	    UUID categoryUuid = filters.categoryId() != null ? UUID.fromString(filters.categoryId()) : null;
	    
	    return movementRepo.findAllMovements(
	            uuid, currency,
	            filters.from(), filters.to(),
	            accountUuid, categoryUuid, filters.type(),
	            limit, offset
	    ).stream()
	            .map(mapper::mapToTransactionResponse)
	            .collect(Collectors.toList());
	}

	public ContractDtos.TransactionResponse createTransaction(String userId,
			ContractDtos.UpsertTransactionRequest request) {
		setAuthContext(userId);

		// LOG PARA VER QUÉ ESTÁ LLEGANDO
		System.out.println("=== CREATE TRANSACTION DEBUG ===");
		System.out.println("Tipo recibido: '" + request.type() + "'");
		System.out.println("Longitud del tipo: " + (request.type() == null ? "null" : request.type().length()));
		System.out.println("Caracteres: "
				+ java.util.Arrays.toString(request.type() == null ? new char[0] : request.type().toCharArray()));

		UUID uuid = UUID.fromString(userId);
		UUID accountUuid = mapper.toUuid(request.accountId());

		// Validar y sanitizar el tipo ANTES de enviar a la BD
		String movementType = request.type();
		if (movementType != null) {
			movementType = movementType.toLowerCase().trim();
			// Mapear 'withdrawal' a 'expense'
			if ("withdrawal".equals(movementType)) {
				System.out.println("⚠️ Se recibió 'withdrawal', convirtiendo a 'expense'");
				movementType = "expense";
			}
			// Validar que sea un valor permitido
			List<String> validTypes = List.of("income", "expense", "transfer", "payment", "adjustment");
			if (!validTypes.contains(movementType)) {
				System.out.println("❌ Tipo inválido: " + movementType + ", usando 'expense' por defecto");
				movementType = "expense";
			}
		} else {
			movementType = "expense";
		}

		Object[] result = movementRepo.createTransaction(uuid, accountUuid, mapper.toUuid(request.transferAccountId()),
				mapper.toUuid(request.categoryId()), movementType, request.amount(), request.description(),
				request.currency(), request.transactionDate(), request.notes());
		return mapper.mapToTransactionResponse(result);
	}

	public ContractDtos.TransactionResponse updateTransaction(String userId, String id,
			ContractDtos.UpsertTransactionRequest request) {
		setAuthContext(userId);
		UUID userUuid = UUID.fromString(userId);
		UUID movementUuid = UUID.fromString(id);
		if (movementRepo.existsByUserAndId(movementUuid, userUuid) == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "transaction not found");
		Object[] result = movementRepo.updateTransaction(movementUuid, userUuid, mapper.toUuid(request.accountId()),
				mapper.toUuid(request.transferAccountId()), mapper.toUuid(request.categoryId()), request.type(),
				request.amount(), request.description(), request.currency(), request.transactionDate(),
				request.notes());
		return mapper.mapToTransactionResponse(result);
	}

	public void deleteTransaction(String userId, String id) {
		UUID userUuid = UUID.fromString(userId);
		UUID movementUuid = UUID.fromString(id);
		int deleted = movementRepo.softDelete(movementUuid, userUuid);
		if (deleted == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "transaction not found");
	}

	// ==================== DEBTS ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.DebtResponse> listDebts(String userId) {
		UUID uuid = UUID.fromString(userId);
		return debtRepo.listDebts(uuid).stream().map(mapper::mapToDebtResponse).collect(Collectors.toList());
	}

	public ContractDtos.DebtResponse createDebt(String userId, ContractDtos.UpsertDebtRequest request) {
		UUID uuid = UUID.fromString(userId);
		Object[] result = debtRepo.createDebt(uuid, request.name(), request.principalBalance(), request.installment(),
				request.frequency(), request.nextDueDate() != null ? request.nextDueDate().toString() : null,
				request.notes());
		return mapper.mapToDebtResponse(result);
	}

	public ContractDtos.DebtResponse updateDebt(String userId, String id, ContractDtos.UpsertDebtRequest request) {
		UUID userUuid = UUID.fromString(userId);
		UUID debtUuid = UUID.fromString(id);
		if (debtRepo.existsByUserAndId(debtUuid, userUuid) == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "debt not found");
		Object[] result = debtRepo.updateDebt(debtUuid, userUuid, request.name(), request.principalBalance(),
				request.installment(), request.frequency(),
				request.nextDueDate() != null ? request.nextDueDate().toString() : null, request.notes());
		return mapper.mapToDebtResponse(result);
	}

	public void deleteDebt(String userId, String id) {
		UUID userUuid = UUID.fromString(userId);
		UUID debtUuid = UUID.fromString(id);
		int deleted = debtRepo.softDelete(debtUuid, userUuid);
		if (deleted == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "debt not found");
	}

	// ==================== RECURRING PAYMENTS ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.RecurringPaymentResponse> listRecurringPayments(String userId) {
		UUID uuid = UUID.fromString(userId);
		String currency = profileRepo.getUserCurrency(uuid);
		return scheduledPaymentRepo.listRecurringPayments(uuid, currency).stream().map(mapper::mapToRecurringResponse)
				.collect(Collectors.toList());
	}

	public ContractDtos.RecurringPaymentResponse createRecurringPayment(String userId,
			ContractDtos.UpsertRecurringPaymentRequest request) {
		UUID uuid = UUID.fromString(userId);
		String paymentType = request.paymentType() != null ? request.paymentType() : "expense";
		Object[] result = scheduledPaymentRepo.createRecurringPayment(uuid, request.name(), request.amount(),
				request.currency(), request.frequency(), request.nextDueDate(), mapper.toUuid(request.categoryId()),
				paymentType);
		return mapper.mapToRecurringResponse(result);
	}

	public ContractDtos.RecurringPaymentResponse updateRecurringPayment(String userId, String id,
			ContractDtos.UpsertRecurringPaymentRequest request) {
		UUID userUuid = UUID.fromString(userId);
		UUID spUuid = UUID.fromString(id);
		if (scheduledPaymentRepo.existsByUserAndId(spUuid, userUuid) == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "recurring payment not found");
		String paymentType = request.paymentType() != null ? request.paymentType() : "expense";
		Object[] result = scheduledPaymentRepo.updateRecurringPayment(spUuid, userUuid, request.name(),
				request.amount(), request.currency(), request.frequency(), request.nextDueDate(),
				mapper.toUuid(request.categoryId()), paymentType);
		return mapper.mapToRecurringResponse(result);
	}

	public void deleteRecurringPayment(String userId, String id) {
		UUID userUuid = UUID.fromString(userId);
		UUID spUuid = UUID.fromString(id);
		int deleted = scheduledPaymentRepo.softDelete(spUuid, userUuid);
		if (deleted == 0)
			throw new ApiException(HttpStatus.NOT_FOUND, "recurring payment not found");
	}

	// ==================== INSTALLMENTS ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.InstallmentResponse> listInstallments(String userId, String debtId) {
		UUID uuid = UUID.fromString(userId);
		List<Object[]> rows;
		if (debtId != null && !debtId.isEmpty()) {
			rows = installmentRepo.listInstallmentsByDebt(uuid, UUID.fromString(debtId));
		} else {
			rows = installmentRepo.listInstallments(uuid, 500);
		}
		return rows.stream().map(mapper::mapToInstallmentResponse).collect(Collectors.toList());
	}

	public ContractDtos.InstallmentResponse createInstallment(String userId,
			ContractDtos.UpsertInstallmentRequest request) {
		UUID uuid = UUID.fromString(userId);
		UUID debtUuid = UUID.fromString(request.debtId());
		if (debtRepo.existsByUserAndId(debtUuid, uuid) == 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "debt not found");
		}
		installmentRepo.createInstallment(uuid, debtUuid, request.number(), request.amount(), request.dueDate(),
				request.paid() != null ? request.paid() : false);
		List<Object[]> installments = installmentRepo.listInstallmentsByDebt(uuid, debtUuid);
		Object[] lastInstallment = installments.isEmpty() ? null : installments.get(installments.size() - 1);
		return mapper.mapToInstallmentResponse(lastInstallment);
	}

	public ContractDtos.InstallmentResponse updateInstallment(String userId, String id,
			ContractDtos.UpsertInstallmentRequest request) {
		UUID uuid = UUID.fromString(userId);
		UUID installmentUuid = UUID.fromString(id);
		installmentRepo.updateInstallment(uuid, installmentUuid, request.number(), request.amount(), request.dueDate(),
				request.paid());
		Object[] row = installmentRepo.getInstallmentById(uuid, installmentUuid);
		return mapper.mapToInstallmentResponse(row);
	}

	public ContractDtos.InstallmentResponse markInstallmentAsPaid(String userId, String id, ContractDtos.PayInstallmentRequest request) {
	    UUID uuid = UUID.fromString(userId);
	    UUID installmentUuid = UUID.fromString(id);
	    UUID debitAccountUuid = UUID.fromString(request.debitAccountId()); // Cuenta de origen (débito/efectivo)
	    
	    System.out.println("=== MARK INSTALLMENT AS PAID DEBUG ===");
	    System.out.println("debitAccountId recibido: " + request.debitAccountId());
	    System.out.println("currency: " + request.currency());
	    System.out.println("notes: " + request.notes());
	    System.out.println("id: " + installmentUuid);
	    System.out.println("userid: " + uuid);
	    System.out.println("debitAccountId: " + debitAccountUuid);
	    
	    
	    // Obtener la installment
	    Object[] installmentRow = installmentRepo.getInstallmentById(uuid, installmentUuid);
	    if (installmentRow == null) {
	        throw new ApiException(HttpStatus.NOT_FOUND, "installment not found");
	    }
	    Object[] unwrappedInstallment = mapper.unwrap(installmentRow);
	    UUID debtId = UUID.fromString(mapper.toString(unwrappedInstallment[1])); // debt_id
	    BigDecimal amount = mapper.toBigDecimal(unwrappedInstallment[3]); // amount de la partialidad
	    
	    // Obtener el account_id de la installment (si existe, para tarjetas de crédito)
	    UUID accountId = null;
	    if (unwrappedInstallment.length > 8 && unwrappedInstallment[8] != null) {
	        String accountIdStr = mapper.toString(unwrappedInstallment[8]);
	        if (accountIdStr != null && !accountIdStr.isEmpty()) {
	            accountId = UUID.fromString(accountIdStr);
	            System.out.println("Account ID (tarjeta) encontrado: " + accountId);
	        }
	    }
	    
	    // 1. Registrar transacción de PAGO en la cuenta de origen (débito/efectivo) - RESTA
	    movementRepo.createTransaction(
	        uuid, debitAccountUuid, null, null, "expense",
	        amount, "Pago de partialidad", request.currency(), LocalDate.now(), request.notes()
	    );
	    
	    // 2. Si la partialidad está asociada a una tarjeta de crédito, registrar transacción en la tarjeta
	    if (accountId != null) {
	        movementRepo.createTransaction(
	            uuid, accountId, null, null, "payment",
	            amount, "Pago recibido - Partialidad", request.currency(), LocalDate.now(), request.notes()
	        );
	    }
	    
	    // 3. Marcar como pagada
	    int updated = installmentRepo.markAsPaid(uuid, installmentUuid);
	    if (updated == 0) {
	        throw new ApiException(HttpStatus.NOT_FOUND, "installment not found or already paid");
	    }
	    
	    // 4. Actualizar el saldo restante de la DEUDA
	    String sqlDebt = "UPDATE debts SET remaining_balance = remaining_balance - ?, updated_at = NOW() " +
	            "WHERE id = ? AND user_id = ? AND deleted_at IS NULL";
	    jdbcTemplate.update(sqlDebt, amount, debtId, uuid);
	    
	    // NOTA: el saldo de la tarjeta de crédito se actualiza automáticamente
	    // vía el trigger trg_movements_balance al insertar el movimiento 'payment'
	    
	    Object[] row = installmentRepo.getInstallmentById(uuid, installmentUuid);
	    return mapper.mapToInstallmentResponse(row);
	}

	public void deleteInstallment(String userId, String id) {
		UUID uuid = UUID.fromString(userId);
		UUID installmentUuid = UUID.fromString(id);
		int deleted = installmentRepo.softDeleteInstallment(uuid, installmentUuid);
		if (deleted == 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "installment not found");
		}
	}

	// ==================== CREDIT CARD INSTALLMENT (Compra a meses)
	// ====================
	public ContractDtos.InstallmentResponse createCreditCardInstallment(String userId,
			ContractDtos.UpsertInstallmentRequest request) {
		UUID uuid = UUID.fromString(userId);
		UUID accountUuid = request.accountId() != null ? UUID.fromString(request.accountId()) : null;
		UUID debtUuid = request.debtId() != null ? UUID.fromString(request.debtId()) : null;
		String currency = profileRepo.getUserCurrency(uuid);

		// Validar que la cuenta existe y es de crédito
		if (accountUuid != null) {
			Object[] accountRow = accountRepo.getAccountById(accountUuid, uuid, currency);
			if (accountRow == null) {
				throw new ApiException(HttpStatus.NOT_FOUND, "account not found");
			}
			Object[] unwrapped = mapper.unwrap(accountRow);
			String accountType = mapper.toString(unwrapped[2]); // type está en índice 2
			if (!"credit".equals(accountType)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "installments only supported for credit cards");
			}
		}

		// Si no hay debtId, crear una deuda automática para la tarjeta
		if (debtUuid == null && accountUuid != null) {
			String debtName = "Compra a meses - " + request.number() + " meses";
			Object[] newDebt = debtRepo.createDebt(uuid, debtName,
					request.originalPurchaseAmount() != null ? request.originalPurchaseAmount()
							: request.amount().multiply(BigDecimal.valueOf(request.number())),
					request.amount(), "monthly", request.dueDate().toString(), "Compra a meses con tarjeta");
			Object[] unwrapped = mapper.unwrap(newDebt);
			debtUuid = UUID.fromString(mapper.toString(unwrapped[0]));
		}

		// Crear la installment
		installmentRepo.createCreditCardInstallment(uuid, debtUuid, accountUuid, request.number(), request.amount(),
				request.dueDate(), request.originalPurchaseAmount(), request.interestRate(),
				request.paid() != null ? request.paid() : false);

		// Obtener la installment recién creada
		List<Object[]> installments = installmentRepo.listInstallmentsByAccount(uuid, accountUuid);
		Object[] lastInstallment = installments.isEmpty() ? null : installments.get(installments.size() - 1);
		return mapper.mapToInstallmentResponse(lastInstallment);
	}

	// ==================== FINANCIAL GOALS ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.FinancialGoalResponse> listGoals(String userId) {
		UUID uuid = UUID.fromString(userId);
		List<Object[]> rows = financialGoalRepo.listGoals(uuid);
		return rows.stream().map(mapper::mapToFinancialGoalResponse).collect(Collectors.toList());
	}

	public ContractDtos.FinancialGoalResponse createGoal(String userId,
			ContractDtos.UpsertFinancialGoalRequest request) {
		UUID uuid = UUID.fromString(userId);
		financialGoalRepo.createGoal(uuid, request.name(), request.targetAmount(),
				request.currentProgress() != null ? request.currentProgress() : BigDecimal.ZERO, request.targetDate(),
				request.status() != null ? request.status() : "active");
		List<Object[]> goals = financialGoalRepo.listGoals(uuid);
		Object[] lastGoal = goals.isEmpty() ? null : goals.get(goals.size() - 1);
		return mapper.mapToFinancialGoalResponse(lastGoal);
	}

	public ContractDtos.FinancialGoalResponse updateGoal(String userId, String id,
			ContractDtos.UpsertFinancialGoalRequest request) {
		UUID uuid = UUID.fromString(userId);
		UUID goalUuid = UUID.fromString(id);
		financialGoalRepo.updateGoal(uuid, goalUuid, request.name(), request.targetAmount(), request.currentProgress(),
				request.targetDate(), request.status());
		Object[] row = financialGoalRepo.getGoalById(uuid, goalUuid);
		return mapper.mapToFinancialGoalResponse(row);
	}

	public void deleteGoal(String userId, String id) {
		UUID uuid = UUID.fromString(userId);
		UUID goalUuid = UUID.fromString(id);
		financialGoalRepo.softDeleteGoal(uuid, goalUuid);
	}

	// ==================== BUDGETS ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.BudgetResponse> listBudgets(String userId) {
		UUID uuid = UUID.fromString(userId);
		List<Object[]> rows = budgetRepo.listActiveBudgets(uuid);
		return rows.stream().map(mapper::mapToBudgetResponse).collect(Collectors.toList());
	}

	public ContractDtos.BudgetResponse createBudget(String userId, ContractDtos.UpsertBudgetRequest request) {
		UUID uuid = UUID.fromString(userId);
		UUID categoryUuid = UUID.fromString(request.categoryId());
		LocalDate periodStart;
		LocalDate periodEnd;
		LocalDate today = LocalDate.now();
		if ("biweekly".equals(request.period())) {
			if (today.getDayOfMonth() <= 15) {
				periodStart = today.withDayOfMonth(1);
				periodEnd = today.withDayOfMonth(15);
			} else {
				periodStart = today.withDayOfMonth(16);
				periodEnd = today.withDayOfMonth(today.lengthOfMonth());
			}
		} else {
			periodStart = today.withDayOfMonth(1);
			periodEnd = today.withDayOfMonth(today.lengthOfMonth());
		}
		BigDecimal alertThreshold = request.alertThreshold() != null
				? request.alertThreshold().divide(BigDecimal.valueOf(100))
				: BigDecimal.valueOf(0.8);
		budgetRepo.createBudget(uuid, categoryUuid, request.period(), periodStart, periodEnd, request.amountLimit(),
				alertThreshold);
		List<Object[]> budgets = budgetRepo.listActiveBudgets(uuid);
		Object[] lastBudget = budgets.isEmpty() ? null : budgets.get(budgets.size() - 1);
		return mapper.mapToBudgetResponse(lastBudget);
	}

	public ContractDtos.BudgetResponse updateBudget(String userId, String id, ContractDtos.UpsertBudgetRequest request) {
	    UUID uuid = UUID.fromString(userId);
	    UUID budgetUuid = UUID.fromString(id);
	    UUID categoryUuid = request.categoryId() != null ? UUID.fromString(request.categoryId()) : null;
	    BigDecimal alertThreshold = request.alertThreshold() != null ? 
	            request.alertThreshold().divide(BigDecimal.valueOf(100)) : null;

	    // Actualizar el presupuesto
	    budgetRepo.updateBudget(uuid, budgetUuid, categoryUuid, request.period(),
	            request.periodStart(), request.periodEnd(), request.amountLimit(), alertThreshold);
	    
	    // Obtener el presupuesto actualizado
	    Object[] row = budgetRepo.getBudgetById(uuid, budgetUuid);
	    if (row == null) {
	        throw new ApiException(HttpStatus.NOT_FOUND, "Budget not found after update");
	    }
	    return mapper.mapToBudgetResponse(row);
	}

	public void deleteBudget(String userId, String id) {
		UUID uuid = UUID.fromString(userId);
		UUID budgetUuid = UUID.fromString(id);
		budgetRepo.softDeleteBudget(uuid, budgetUuid);
	}

	// ==================== SUMMARY ====================
	@Transactional(readOnly = true)
	public ContractDtos.SummaryResponse summary(String userId, String range, LocalDate from, LocalDate to, String accountId) {
	    UUID uuid = UUID.fromString(userId);
	    String currency = profileRepo.getUserCurrency(uuid);
	    
	    LocalDate[] window = resolveWindow(range, from, to);
	    LocalDate startDate = window[0];
	    LocalDate endDate = window[1];
	    
	    BigDecimal realIncome = BigDecimal.ZERO;
	    BigDecimal expenses = BigDecimal.ZERO;
	    BigDecimal debtPayments = BigDecimal.ZERO;  // Pagos realizados en el periodo
	    BigDecimal fixedPayments = BigDecimal.ZERO;
	    
	    if (startDate != null && endDate != null) {
	    Object[] summaryRow = movementRepo.getSummaryByDateRange(uuid, startDate, endDate);
	        if (summaryRow != null) {
	            Object[] unwrapped = mapper.unwrap(summaryRow);
	            realIncome = mapper.toBigDecimal(unwrapped[0]);
	            expenses = mapper.toBigDecimal(unwrapped[1]);
	            debtPayments = mapper.toBigDecimal(unwrapped[2]);
	            fixedPayments = mapper.toBigDecimal(unwrapped[3]);
	        }
	    }
	    
	    BigDecimal totalDebt = debtRepo.getTotalRemainingBalance(uuid);
	    
	    BigDecimal totalExpenses = fixedPayments;
	    BigDecimal availableBalance = realIncome.subtract(totalExpenses);
	    
	    return new ContractDtos.SummaryResponse(
	        realIncome,           // income
	        expenses,              // expenses
	        fixedPayments,         // fixedPayments
	        debtPayments,          // debtPayments (pagos realizados en el periodo)
	        totalDebt,             // totalRemainingDebt (NUEVO)
	        availableBalance,      // availableBalance
	        currency
	    );
	}

	@Transactional(readOnly = true)
	public List<ContractDtos.CategoryStatResponse> categoryStats(String userId, String range, LocalDate from,
			LocalDate to, String accountId) {
		LocalDate[] window = resolveWindow(range, from, to);
		from = window[0];
		to = window[1];
		UUID uuid = UUID.fromString(userId);
		List<Object[]> rows;
		if (from != null && to != null) {
			rows = movementRepo.getCategoryStatsByDateRange(uuid, from, to);
		} else {
			rows = movementRepo.getCategoryStatsAll(uuid);
		}
		List<ContractDtos.CategoryStatResponse> raw = rows.stream()
				.map(r -> new ContractDtos.CategoryStatResponse(mapper.toString(r[0]), mapper.toString(r[1]),
						mapper.toBigDecimal(r[2]), BigDecimal.ZERO))
				.collect(Collectors.toList());
		BigDecimal total = raw.stream().map(ContractDtos.CategoryStatResponse::amount).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		return raw.stream().map(item -> {
			BigDecimal pct = total.signum() == 0 ? BigDecimal.ZERO
					: item.amount().multiply(BigDecimal.valueOf(100)).divide(total, 2, java.math.RoundingMode.HALF_UP);
			return new ContractDtos.CategoryStatResponse(item.categoryId(), item.categoryName(), item.amount(), pct);
		}).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public ContractDtos.UpcomingResponse upcoming(String userId) {
	    UUID uuid = UUID.fromString(userId);
	    List<ContractDtos.UpcomingItemResponse> items = new ArrayList<>();
	    
	    LocalDate today = LocalDate.now();
	    LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
	    
	    System.out.println("=== UPCOMING DEBUG ===");
	    System.out.println("Today: " + today);
	    System.out.println("Month end: " + monthEnd);
	    
	    // 1. Pagos recurrentes
	    try {
	        List<Object[]> recurringRows = scheduledPaymentRepo.getUpcomingRecurringPaymentsInRange(uuid, today, monthEnd);
	        System.out.println("Recurring rows: " + (recurringRows == null ? "null" : recurringRows.size()));
	        if (recurringRows != null) {
	            for (Object[] row : recurringRows) {
	                Object[] unwrapped = mapper.unwrap(row);
	                LocalDate dueDate = mapper.toLocalDate(unwrapped[3]);
	                if (dueDate != null) {
	                    items.add(new ContractDtos.UpcomingItemResponse(
	                        "recurring",
	                        mapper.toString(unwrapped[0]),
	                        mapper.toString(unwrapped[1]),
	                        dueDate,
	                        mapper.toBigDecimal(unwrapped[2])
	                    ));
	                }
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Error loading recurring payments: " + e.getMessage());
	    }
	    
	    // 2. Deudas
	    try {
	        List<Object[]> debtRows = debtRepo.getUpcomingDebtsInRange(uuid, today, monthEnd);
	        System.out.println("Debt rows: " + (debtRows == null ? "null" : debtRows.size()));
	        if (debtRows != null) {
	            for (Object[] row : debtRows) {
	                Object[] unwrapped = mapper.unwrap(row);
	                LocalDate dueDate = mapper.toLocalDate(unwrapped[3]);
	                BigDecimal amount = mapper.toBigDecimal(unwrapped[4]);
	                if (dueDate != null && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
	                    items.add(new ContractDtos.UpcomingItemResponse(
	                        "debt",
	                        mapper.toString(unwrapped[0]),
	                        mapper.toString(unwrapped[1]),
	                        dueDate,
	                        amount
	                    ));
	                }
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Error loading debts: " + e.getMessage());
	    }
	    
	    // 3. Partialidades
	    try {
	        List<Object[]> installmentRows = installmentRepo.getUpcomingInstallments(uuid, today, monthEnd);
	        System.out.println("Installment rows: " + (installmentRows == null ? "null" : installmentRows.size()));
	        if (installmentRows != null) {
	            for (Object[] row : installmentRows) {
	                Object[] unwrapped = mapper.unwrap(row);
	                LocalDate dueDate = mapper.toLocalDate(unwrapped[2]);
	                BigDecimal amount = mapper.toBigDecimal(unwrapped[3]);
	                if (dueDate != null && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
	                    items.add(new ContractDtos.UpcomingItemResponse(
	                        "installment",
	                        mapper.toString(unwrapped[0]),
	                        mapper.toString(unwrapped[1]),
	                        dueDate,
	                        amount
	                    ));
	                }
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Error loading installments: " + e.getMessage());
	    }
	    
	    // Ordenar por fecha
	    items.sort(Comparator.comparing(ContractDtos.UpcomingItemResponse::dueDate, Comparator.nullsLast(Comparator.naturalOrder())));
	    
	    System.out.println("Total items: " + items.size());
	    
	    return new ContractDtos.UpcomingResponse(items);
	}

	// ==================== NUEVO: SOBREENDEUDAMIENTO ====================
	@Transactional(readOnly = true)
	public ContractDtos.DebtRatioResponse getDebtRatio(String userId) {
		System.out.println("=== DEBT RATIO DEBUG ===");
		System.out.println("UserId: " + userId);

		try {
			UUID uuid = UUID.fromString(userId);
			String currency = profileRepo.getUserCurrency(uuid);
			LocalDate today = LocalDate.now();
			LocalDate monthStart = today.withDayOfMonth(1);
			LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

			Object[] summaryRow = movementRepo.getSummaryByDateRange(uuid, monthStart, monthEnd);


			BigDecimal totalIncome = BigDecimal.ZERO;
			BigDecimal totalDebtPayments = BigDecimal.ZERO;
			if (summaryRow != null) {
			    Object[] unwrapped = mapper.unwrap(summaryRow);
			    totalIncome = mapper.toBigDecimal(unwrapped[0]);
			    totalDebtPayments = mapper.toBigDecimal(unwrapped[2]);
			}

			BigDecimal ratio;
			if (totalIncome.compareTo(BigDecimal.ZERO) == 0 && totalDebtPayments.compareTo(BigDecimal.ZERO) == 0) {
			    ratio = BigDecimal.ZERO;
			} else if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
			    ratio = totalDebtPayments.multiply(BigDecimal.valueOf(100))
			        .divide(totalIncome, 2, java.math.RoundingMode.HALF_UP);
			} else {
			    ratio = BigDecimal.valueOf(100);
			}

			String riskLevel;
			String recommendation;
			if (ratio.compareTo(BigDecimal.valueOf(30)) <= 0) {
				riskLevel = "bajo";
				recommendation = "Tu nivel de endeudamiento es saludable. Sigue así.";
			} else if (ratio.compareTo(BigDecimal.valueOf(50)) <= 0) {
				riskLevel = "medio";
				recommendation = "Considera reducir tus deudas antes de adquirir nuevas.";
			} else if (ratio.compareTo(BigDecimal.valueOf(70)) <= 0) {
				riskLevel = "alto";
				recommendation = "Tus deudas consumen gran parte de tus ingresos. Prioriza pagar las de mayor interés.";
			} else {
				riskLevel = "crítico";
				recommendation = "¡Alerta! Tus deudas superan tu capacidad de pago. Busca asesoría financiera.";
			}

			return new ContractDtos.DebtRatioResponse(totalIncome, totalDebtPayments, ratio, riskLevel, recommendation,
					currency);
		} catch (Exception e) {
			System.err.println("Error en getDebtRatio: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	// ==================== NUEVO: ORGANIZACIÓN QUINCENAL ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.BiweeklyScheduleResponse> getBiweeklySchedule(String userId) {
		UUID uuid = UUID.fromString(userId);
		LocalDate today = LocalDate.now();
		LocalDate firstHalfStart = today.withDayOfMonth(1);
		LocalDate firstHalfEnd = today.withDayOfMonth(15);
		LocalDate secondHalfStart = today.withDayOfMonth(16);
		LocalDate secondHalfEnd = today.withDayOfMonth(today.lengthOfMonth());

		LocalDate lastMonth = today.minusMonths(1);
		LocalDate lastMonthStart = lastMonth.withDayOfMonth(1);
		LocalDate lastMonthEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
		BigDecimal monthlyIncome = BigDecimal.ZERO;
		Object[] incomeRow = movementRepo.getSummaryByDateRange(uuid, lastMonthStart, lastMonthEnd);
		if (incomeRow != null) {
			Object[] unwrapped = mapper.unwrap(incomeRow);
			monthlyIncome = mapper.toBigDecimal(unwrapped[0]);
		}
		BigDecimal biweeklyIncome = monthlyIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);

		List<ContractDtos.BiweeklyScheduleResponse> schedules = new ArrayList<>();

		List<ContractDtos.BiweeklyPaymentItem> firstPayments = getPaymentsInRange(uuid, firstHalfStart, firstHalfEnd);
		BigDecimal firstTotal = firstPayments.stream().map(ContractDtos.BiweeklyPaymentItem::amount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		schedules.add(new ContractDtos.BiweeklyScheduleResponse("Primera quincena", firstHalfStart, firstHalfEnd,
				firstPayments, firstTotal, biweeklyIncome, biweeklyIncome.subtract(firstTotal)));

		List<ContractDtos.BiweeklyPaymentItem> secondPayments = getPaymentsInRange(uuid, secondHalfStart,
				secondHalfEnd);
		BigDecimal secondTotal = secondPayments.stream().map(ContractDtos.BiweeklyPaymentItem::amount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		schedules.add(new ContractDtos.BiweeklyScheduleResponse("Segunda quincena", secondHalfStart, secondHalfEnd,
				secondPayments, secondTotal, biweeklyIncome, biweeklyIncome.subtract(secondTotal)));

		return schedules;
	}

	private List<ContractDtos.BiweeklyPaymentItem> getPaymentsInRange(UUID userId, LocalDate start, LocalDate end) {
		List<ContractDtos.BiweeklyPaymentItem> items = new ArrayList<>();

		// Pagos recurrentes
		try {
			List<Object[]> recurringRows = scheduledPaymentRepo.getUpcomingRecurringPaymentsInRange(userId, start, end);
			if (recurringRows != null) {
				for (Object[] row : recurringRows) {
					Object[] unwrapped = mapper.unwrap(row);
					items.add(new ContractDtos.BiweeklyPaymentItem(mapper.toString(unwrapped[0]), // id
							mapper.toString(unwrapped[1]), // name
							mapper.toBigDecimal(unwrapped[2]), // amount
							mapper.toLocalDate(unwrapped[4]), // dueDate
							mapper.toString(unwrapped[3]), // frequency
							"recurring", null // recurring no tiene remainingBalance
					));
				}
			}
		} catch (Exception e) {
			System.err.println("Error loading recurring payments: " + e.getMessage());
		}

		// Deudas - CORREGIDO: usar el monto correcto (min entre fixed_payment y
		// remaining_balance)
		try {
			List<Object[]> debtRows = debtRepo.getUpcomingDebtsInRange(userId, start, end);
			if (debtRows != null) {
				for (Object[] row : debtRows) {
					Object[] unwrapped = mapper.unwrap(row);
					BigDecimal amount = mapper.toBigDecimal(unwrapped[4]); // amount (ya calculado como LEAST)
					BigDecimal remainingBalance = mapper.toBigDecimal(unwrapped[5]); // remaining_balance

					items.add(new ContractDtos.BiweeklyPaymentItem(mapper.toString(unwrapped[0]), // id
							mapper.toString(unwrapped[1]), // name
							amount, // amount
							mapper.toLocalDate(unwrapped[3]), // dueDate
							mapper.toString(unwrapped[2]), // frequency
							"debt", remainingBalance // remainingBalance
					));

					System.out.println("Deuda: " + mapper.toString(unwrapped[1]) + " - Monto a pagar: " + amount
							+ " - Saldo restante: " + remainingBalance);
				}
			}
		} catch (Exception e) {
			System.err.println("Error loading debts: " + e.getMessage());
			e.printStackTrace();
		}

		// Ordenar por fecha
		items.sort(Comparator.comparing(ContractDtos.BiweeklyPaymentItem::dueDate));
		return items;
	}

	// ==================== REPORTS ====================
	@Transactional(readOnly = true)
	public List<ContractDtos.MonthlyReportResponse> getMonthlyReports(String userId, int year) {
		UUID uuid = UUID.fromString(userId);
		LocalDate startDate = LocalDate.of(year, 1, 1);
		List<Object[]> rows = movementRepo.getMonthlyReport(uuid, startDate);
		List<ContractDtos.MonthlyReportResponse> reports = new ArrayList<>();
		for (Object[] row : rows) {
			Object monthObj = row[0];
			LocalDate month;
			if (monthObj instanceof LocalDate) {
				month = (LocalDate) monthObj;
			} else if (monthObj instanceof java.sql.Date) {
				month = ((java.sql.Date) monthObj).toLocalDate();
			} else if (monthObj instanceof java.sql.Timestamp) {
				month = ((java.sql.Timestamp) monthObj).toLocalDateTime().toLocalDate();
			} else if (monthObj instanceof Instant) {
				month = ((Instant) monthObj).atZone(ZoneId.systemDefault()).toLocalDate();
			} else {
				month = LocalDate.now().withDayOfMonth(1);
			}
			BigDecimal income = mapper.toBigDecimal(row[1]);
			BigDecimal expenses = mapper.toBigDecimal(row[2]);
			BigDecimal savings = income.subtract(expenses);
			reports.add(new ContractDtos.MonthlyReportResponse(month.toString(), income, expenses, savings,
					new ArrayList<>()));
		}
		return reports;
	}

	// ==================== SYNC ====================
	@Transactional(readOnly = true)
	public ContractDtos.SyncPullResponse pullSync(String userId, Instant since, String entity) {
		Instant serverTime = Instant.now();
		List<ContractDtos.SyncEntityChange> changes = new ArrayList<>();
		if (entity == null || entity.equals("me")) {
			ContractDtos.MeResponse me = getMe(userId);
			if (since == null || isAfterOrEqual(me.updatedAt(), since))
				changes.add(new ContractDtos.SyncEntityChange("me", "upsert", objectMapper.valueToTree(me)));
		}
		return new ContractDtos.SyncPullResponse(serverTime, changes, new ArrayList<>());
	}

	public ContractDtos.SyncPushResponse pushSync(String userId, ContractDtos.SyncPushRequest request) {
		return new ContractDtos.SyncPushResponse(0, request.changes().size(), new ArrayList<>());
	}

	public Map<String, Object> resolveConflict(String userId, ContractDtos.SyncConflictResolutionRequest request) {
		return Map.of("resolved", true, "strategy", request.resolution());
	}

	public ContractDtos.BackupExportResponse exportBackup(String userId) {
		return new ContractDtos.BackupExportResponse("backup-finanzas.json", "/backup/download?userId=" + userId);
	}

	public ContractDtos.MigrationImportResponse importMigration(String userId,
			ContractDtos.MigrationImportRequest request) {
		return new ContractDtos.MigrationImportResponse(true, 0, Instant.now());
	}

	public Map<String, Object> importBackup(String userId, ContractDtos.BackupImportRequest request) {
		return Map.of("imported", true, "format", request.format());
	}

	// ==================== HELPERS ====================
	private String toJson(List<Integer> payDays) {
		try {
			return objectMapper.writeValueAsString(payDays);
		} catch (Exception e) {
			return "[]";
		}
	}

	private boolean isAfterOrEqual(Instant value, Instant since) {
		return value != null && (value.equals(since) || value.isAfter(since));
	}

	private LocalDate[] resolveWindow(String range, LocalDate from, LocalDate to) {
		if (from != null || to != null)
			return new LocalDate[] { from, to };
		LocalDate today = LocalDate.now();

		System.out.println("resolveWindow - range recibido: '" + range + "'");

		String normalizedRange = range == null ? "monthly" : range.toLowerCase().trim();

		switch (normalizedRange) {
		case "biweekly":
			if (today.getDayOfMonth() <= 15) {
				return new LocalDate[] { today.withDayOfMonth(1), today.withDayOfMonth(15) };
			} else {
				return new LocalDate[] { today.withDayOfMonth(16), today.withDayOfMonth(today.lengthOfMonth()) };
			}
		case "monthly":
			return new LocalDate[] { today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()) };
		case "custom":
			return new LocalDate[] { today.minusDays(30), today };
		default:
			System.out.println("⚠️ Valor de range no reconocido: '" + normalizedRange + "', usando monthly");
			return new LocalDate[] { today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()) };
		}
	}

	// ==================== CREDIT CARD INSTALLMENT (Compra a meses)
	@Transactional
	public List<ContractDtos.InstallmentResponse> createCreditCardPurchase(String userId, ContractDtos.CreditCardPurchaseRequest request) {
	    UUID uuid = UUID.fromString(userId);
	    UUID accountUuid = UUID.fromString(request.accountId());
	    String currency = profileRepo.getUserCurrency(uuid);
	    
	    // Validar que la cuenta existe y es de crédito
	    Object[] accountRow = accountRepo.getAccountById(accountUuid, uuid, currency);
	    if (accountRow == null) {
	        throw new ApiException(HttpStatus.NOT_FOUND, "account not found");
	    }
	    Object[] unwrappedAccount = mapper.unwrap(accountRow);
	    String accountType = mapper.toString(unwrappedAccount[2]);
	    if (!"credit".equals(accountType)) {
	        throw new ApiException(HttpStatus.BAD_REQUEST, "installments only supported for credit cards");
	    }
	    
	    int months = request.months();
	    BigDecimal totalAmount = request.totalAmount();
	    BigDecimal interestRate = request.interestRate() != null ? request.interestRate() : BigDecimal.ZERO;
	    
	    // Calcular pago mensual
	    BigDecimal monthlyAmount;
	    if (interestRate.compareTo(BigDecimal.ZERO) > 0) {
	        BigDecimal rate = interestRate.divide(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
	        BigDecimal factor = rate.add(BigDecimal.ONE).pow(months);
	        monthlyAmount = totalAmount.multiply(rate).multiply(factor)
	                .divide(factor.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
	    } else {
	        monthlyAmount = totalAmount.divide(BigDecimal.valueOf(months), 2, java.math.RoundingMode.HALF_UP);
	    }
	    
	    // Crear la deuda asociada (NO actualizar saldo de tarjeta aquí)
	    String debtName = request.name() != null ? request.name() : "Compra a " + months + " meses";
	    Object[] newDebt = debtRepo.createDebt(
	        uuid,
	        debtName,
	        totalAmount,
	        monthlyAmount,
	        "monthly",
	        request.firstDueDate().toString(),
	        "Compra a meses con tarjeta de crédito"
	    );
	    Object[] unwrappedDebt = mapper.unwrap(newDebt);
	    UUID debtUuid = UUID.fromString(mapper.toString(unwrappedDebt[0]));
	    
	    // Crear las partialidades (una por cada mes)
	    LocalDate dueDate = request.firstDueDate();
	    for (int i = 1; i <= months; i++) {
	        installmentRepo.createCreditCardInstallment(
	            uuid, debtUuid, accountUuid, i, monthlyAmount, dueDate,
	            totalAmount, interestRate, false
	        );
	        dueDate = dueDate.plusMonths(1);
	    }
	    
	    // Registrar el cargo inicial en la tarjeta de crédito (aumenta saldo = nueva deuda)
	    movementRepo.createTransaction(
	        uuid, accountUuid, null, null, "income",
	        totalAmount, "Compra a meses: " + request.name(), currency, request.firstDueDate(), "Compra a " + months + " meses"
	    );
	    
	    System.out.println("Compra a " + months + " meses registrada.");
	    System.out.println("Pago mensual: " + monthlyAmount);
	    System.out.println("Total compra: " + totalAmount);
	    
	    List<Object[]> createdInstallments = installmentRepo.listInstallmentsByAccount(uuid, accountUuid);
	    List<ContractDtos.InstallmentResponse> installments = new ArrayList<>();
	    for (Object[] row : createdInstallments) {
	        installments.add(mapper.mapToInstallmentResponse(row));
	    }
	    
	    return installments;
	}

	// Método para pagar una partialidad de tarjeta de crédito
	@Transactional
	public ContractDtos.InstallmentResponse payCreditCardInstallment(String userId, String installmentId,
			ContractDtos.PayInstallmentRequest request) {
		UUID uuid = UUID.fromString(userId);
		UUID installmentUuid = UUID.fromString(installmentId);
		UUID debitAccountUuid = UUID.fromString(request.debitAccountId());

		// Obtener la partialidad
		Object[] installmentRow = installmentRepo.getInstallmentById(uuid, installmentUuid);
		if (installmentRow == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "installment not found");
		}
		Object[] unwrappedInstallment = mapper.unwrap(installmentRow);

		// Verificar que tiene account_id (es de tarjeta de crédito)
		Object accountIdObj = unwrappedInstallment.length > 8 ? unwrappedInstallment[8] : null;
		if (accountIdObj == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "This installment is not linked to a credit card");
		}
		UUID accountId = UUID.fromString(mapper.toString(accountIdObj));
		BigDecimal amount = mapper.toBigDecimal(unwrappedInstallment[3]);

		// Validar que la cuenta de débito existe
		Object[] debitAccountRow = accountRepo.getAccountById(debitAccountUuid, uuid, request.currency());
		if (debitAccountRow == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "debit account not found");
		}

		// Crear transacción de gasto en la tarjeta de crédito (reduce su saldo)
		movementRepo.createTransaction(uuid, accountId, null, null, "expense", amount,
				"Pago de partialidad #" + mapper.toString(unwrappedInstallment[2]), request.currency(), LocalDate.now(),
				"Pago de compra a meses");

		// Marcar la partialidad como pagada
		int updated = installmentRepo.markAsPaid(uuid, installmentUuid);
		if (updated == 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "installment not found or already paid");
		}

		// Actualizar el saldo de la deuda
		UUID debtId = UUID.fromString(mapper.toString(unwrappedInstallment[1]));
		Object[] debtRow = debtRepo.getDebtById(uuid, debtId);
		if (debtRow != null) {
			Object[] unwrappedDebt = mapper.unwrap(debtRow);
			BigDecimal currentRemaining = mapper.toBigDecimal(unwrappedDebt[3]);
			BigDecimal newRemaining = currentRemaining.subtract(amount);
			if (newRemaining.compareTo(BigDecimal.ZERO) < 0) {
				newRemaining = BigDecimal.ZERO;
			}
			debtRepo.updateDebt(debtId, uuid, mapper.toString(unwrappedDebt[2]), newRemaining,
					mapper.toBigDecimal(unwrappedDebt[4]), mapper.toString(unwrappedDebt[5]), null,
					mapper.toString(unwrappedDebt[7]));
		}

		// Crear movimiento en la cuenta de débito (resta)
		movementRepo.createTransaction(uuid, debitAccountUuid, null, null, "expense", amount,
				"Pago de tarjeta - Partialidad #" + mapper.toString(unwrappedInstallment[2]), request.currency(),
				LocalDate.now(), "Pago de compra a meses");

		Object[] updatedInstallment = installmentRepo.getInstallmentById(uuid, installmentUuid);
		return mapper.mapToInstallmentResponse(updatedInstallment);
	}
	@Transactional(readOnly = true)
	public Map<String, BigDecimal> getPaymentSummary(String userId, String range) {
	    UUID uuid = UUID.fromString(userId);
	    LocalDate[] window = resolveWindow(range, null, null);
	    LocalDate startDate = window[0];
	    LocalDate endDate = window[1];
	    
	    // Pagos a tarjeta de crédito (transferencias)
	    BigDecimal creditCardPayments = movementRepo.getCreditCardPayments(uuid, startDate, endDate);
	    
	    // Pagos a deudas
	    BigDecimal debtPayments = movementRepo.getDebtPayments(uuid, startDate, endDate);
	    
	    return Map.of(
	        "creditCardPayments", creditCardPayments,
	        "debtPayments", debtPayments,
	        "totalPayments", creditCardPayments.add(debtPayments)
	    );
	}

}