package com.codex.finance.service;

import com.codex.finance.client.SupabaseAuthClient;
import com.codex.finance.dto.ContractDtos;
import com.codex.finance.exception.ApiException;
import com.codex.finance.mapper.FinanceMapper;
import com.codex.finance.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FinanceApiService {
    
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

    public FinanceApiService(ProfileRepository profileRepo, CategoryRepository categoryRepo,
                            AccountRepository accountRepo, MovementRepository movementRepo,
                            DebtRepository debtRepo, ScheduledPaymentRepository scheduledPaymentRepo,
                            SupabaseAuthClient authClient, ObjectMapper objectMapper, 
                            FinanceMapper mapper,JdbcTemplate jdbcTemplate,InstallmentRepository installmentRepo) {
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
    }
    private void setAuthContext(String userId) {
        jdbcTemplate.execute("SELECT set_config('request.jwt.claim.sub', '" + userId + "', true)");
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
        if (row == null) throw new ApiException(HttpStatus.NOT_FOUND, "profile not found");
        return mapper.mapToMeResponse(row);
    }

    public ContractDtos.MeResponse updateMe(String userId, ContractDtos.UpdateMeRequest request) {
        UUID uuid = UUID.fromString(userId);
        
        // Obtener settings actuales del perfil
        String settingsJson = profileRepo.getSettingsJson(uuid);
        Map<String, Object> settings;
        try {
            if (settingsJson != null && !settingsJson.isEmpty()) {
                settings = objectMapper.readValue(settingsJson, new TypeReference<Map<String, Object>>() {});
            } else {
                settings = new HashMap<>();
            }
        } catch (Exception e) {
            settings = new HashMap<>();
        }
        
        // Actualizar settings con los nuevos valores
        settings.put("monthlyIncome", request.monthlyIncome() != null ? request.monthlyIncome() : 0);
        settings.put("payCycle", request.payCycle());
        
        String payDaysJson = toJson(request.payDays());
        settings.put("payDays", payDaysJson);
        
        // Convertir settings a JSON string
        String newSettingsJson;
        try {
            newSettingsJson = objectMapper.writeValueAsString(settings);
        } catch (Exception e) {
            newSettingsJson = "{}";
        }
        
        // Guardar en la base de datos
        Object[] row = profileRepo.upsertProfile(
            uuid, 
            request.displayName(), 
            request.currency(),
            newSettingsJson,
            uuid
        );
        
        return mapper.mapToMeResponse(row);
    }

    // ==================== CATEGORIES ====================
    @Transactional(readOnly = true)
    public List<ContractDtos.CategoryResponse> listCategories(String userId) {
        UUID uuid = UUID.fromString(userId);
        return categoryRepo.listCategories(uuid).stream()
            .map(mapper::mapToCategoryResponse)
            .collect(Collectors.toList());
    }

    public ContractDtos.CategoryResponse createCategory(String userId, ContractDtos.UpsertCategoryRequest request) {
        UUID uuid = UUID.fromString(userId);
        Object[] row = categoryRepo.createCategory(uuid, request.name(), request.color(), request.icon(), request.type());
        return mapper.mapToCategoryResponse(row);
    }

    public ContractDtos.CategoryResponse updateCategory(String userId, String id, ContractDtos.UpsertCategoryRequest request) {
        UUID userUuid = UUID.fromString(userId);
        UUID categoryUuid = UUID.fromString(id);
        if (categoryRepo.existsByUserAndId(categoryUuid, userUuid) == 0)
            throw new ApiException(HttpStatus.NOT_FOUND, "category not found");
        Object[] row = categoryRepo.updateCategory(categoryUuid, userUuid, request.name(), request.color(), request.icon(), request.type());
        return mapper.mapToCategoryResponse(row);
    }

    public void deleteCategory(String userId, String id) {
        UUID userUuid = UUID.fromString(userId);
        UUID categoryUuid = UUID.fromString(id);
        int deleted = categoryRepo.softDelete(categoryUuid, userUuid);
        if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "category not found");
    }

    // ==================== ACCOUNTS ====================
    @Transactional(readOnly = true)
    public List<ContractDtos.AccountResponse> listAccounts(String userId) {
        UUID uuid = UUID.fromString(userId);
        String currency = profileRepo.getUserCurrency(uuid);
        return accountRepo.listAccounts(uuid, currency).stream()
            .map(mapper::mapToAccountResponse)
            .collect(Collectors.toList());
    }

    public ContractDtos.AccountResponse createAccount(String userId, ContractDtos.UpsertAccountRequest request) {
        UUID uuid = UUID.fromString(userId);
        Object[] row = accountRepo.createAccount(uuid, request.name(), request.type(), request.institution(),
            request.balance(), request.creditLimit(), request.closingDay(), request.dueDay(),
            request.active(), request.currency());
        return mapper.mapToAccountResponse(row); // <-- usa el mismo mapper
    }

    public ContractDtos.AccountResponse updateAccount(String userId, String id, ContractDtos.UpsertAccountRequest request) {
        UUID userUuid = UUID.fromString(userId);
        UUID accountUuid = UUID.fromString(id);
        if (accountRepo.existsByUserAndId(accountUuid, userUuid) == 0)
            throw new ApiException(HttpStatus.NOT_FOUND, "account not found");
        Object[] result = accountRepo.updateAccount(accountUuid, userUuid, request.name(), request.type(),
            request.institution(), request.balance(), request.creditLimit(), request.closingDay(),
            request.dueDay(), request.active());
        
        Object[] row = (result.length == 1 && result[0] instanceof Object[]) ? (Object[]) result[0] : result;
        
        return mapper.mapToAccountResponseUpdate(row, request.currency()); // <-- Pasa el currency del request
    }

    public void deleteAccount(String userId, String id) {
        UUID userUuid = UUID.fromString(userId);
        UUID accountUuid = UUID.fromString(id);
        int deleted = accountRepo.softDelete(accountUuid, userUuid);
        if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "account not found");
    }

    // ==================== TRANSACTIONS ====================
    @Transactional(readOnly = true)
    public List<ContractDtos.TransactionResponse> listTransactions(String userId, ContractDtos.TransactionFilters filters) {
        UUID uuid = UUID.fromString(userId);
        String currency = profileRepo.getUserCurrency(uuid);
        
        return movementRepo.findAllMovements(uuid, currency, 
            filters.limit() == null ? 100 : filters.limit()).stream()
            .map(mapper::mapToTransactionResponse)
            .collect(Collectors.toList());
    }

    public ContractDtos.TransactionResponse createTransaction(String userId, ContractDtos.UpsertTransactionRequest request) {
        setAuthContext(userId); // <-- AGREGA ESTO
        
        UUID uuid = UUID.fromString(userId);
        Object[] result = movementRepo.createTransaction(uuid, 
            mapper.toUuid(request.accountId()),
            mapper.toUuid(request.transferAccountId()), 
            mapper.toUuid(request.categoryId()),
            request.type(), request.amount(), request.description(),
            request.currency(), request.transactionDate(), request.notes());
        return mapper.mapToTransactionResponse(result);
    }

    public ContractDtos.TransactionResponse updateTransaction(String userId, String id, ContractDtos.UpsertTransactionRequest request) {
        setAuthContext(userId); // <-- AGREGA ESTO
        
        UUID userUuid = UUID.fromString(userId);
        UUID movementUuid = UUID.fromString(id);
        if (movementRepo.existsByUserAndId(movementUuid, userUuid) == 0)
            throw new ApiException(HttpStatus.NOT_FOUND, "transaction not found");
        Object[] result = movementRepo.updateTransaction(movementUuid, userUuid, 
            mapper.toUuid(request.accountId()),
            mapper.toUuid(request.transferAccountId()), 
            mapper.toUuid(request.categoryId()),
            request.type(), request.amount(), request.description(),
            request.currency(), request.transactionDate(), request.notes());
        return mapper.mapToTransactionResponse(result);
    }

    public void deleteTransaction(String userId, String id) {
        UUID userUuid = UUID.fromString(userId);
        UUID movementUuid = UUID.fromString(id);
        int deleted = movementRepo.softDelete(movementUuid, userUuid);
        if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "transaction not found");
    }

    // ==================== DEBTS ====================
    @Transactional(readOnly = true)
    public List<ContractDtos.DebtResponse> listDebts(String userId) {
        UUID uuid = UUID.fromString(userId);
        return debtRepo.listDebts(uuid).stream()
            .map(mapper::mapToDebtResponse)
            .collect(Collectors.toList());
    }

    public ContractDtos.DebtResponse createDebt(String userId, ContractDtos.UpsertDebtRequest request) {
        UUID uuid = UUID.fromString(userId);
        Object[] result = debtRepo.createDebt(uuid, request.name(), request.principalBalance(),
            request.installment(), request.frequency(),
            request.nextDueDate() != null ? request.nextDueDate().toString() : null, request.notes());
        return mapper.mapToDebtResponse(result); // mapper ya hace unwrap
    }

    public ContractDtos.DebtResponse updateDebt(String userId, String id, ContractDtos.UpsertDebtRequest request) {
        UUID userUuid = UUID.fromString(userId);
        UUID debtUuid = UUID.fromString(id);
        if (debtRepo.existsByUserAndId(debtUuid, userUuid) == 0)
            throw new ApiException(HttpStatus.NOT_FOUND, "debt not found");
        Object[] result = debtRepo.updateDebt(debtUuid, userUuid, request.name(), request.principalBalance(),
            request.installment(), request.frequency(),
            request.nextDueDate() != null ? request.nextDueDate().toString() : null, request.notes());
        return mapper.mapToDebtResponse(result); // mapper ya hace unwrap
    }

    public void deleteDebt(String userId, String id) {
        UUID userUuid = UUID.fromString(userId);
        UUID debtUuid = UUID.fromString(id);
        int deleted = debtRepo.softDelete(debtUuid, userUuid);
        if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "debt not found");
    }

    // ==================== RECURRING PAYMENTS ====================
    @Transactional(readOnly = true)
    public List<ContractDtos.RecurringPaymentResponse> listRecurringPayments(String userId) {
        UUID uuid = UUID.fromString(userId);
        String currency = profileRepo.getUserCurrency(uuid);
        return scheduledPaymentRepo.listRecurringPayments(uuid, currency).stream()
            .map(mapper::mapToRecurringResponse)
            .collect(Collectors.toList());
    }

    public ContractDtos.RecurringPaymentResponse createRecurringPayment(String userId, ContractDtos.UpsertRecurringPaymentRequest request) {
        UUID uuid = UUID.fromString(userId);
        Object[] result = scheduledPaymentRepo.createRecurringPayment(uuid, 
            request.name(), request.amount(),
            request.currency(), request.frequency(), request.nextDueDate(),
            mapper.toUuid(request.categoryId()));
        return mapper.mapToRecurringResponse(result); // mapper ya hace unwrap
    }

    public ContractDtos.RecurringPaymentResponse updateRecurringPayment(String userId, String id, ContractDtos.UpsertRecurringPaymentRequest request) {
        UUID userUuid = UUID.fromString(userId);
        UUID spUuid = UUID.fromString(id);
        if (scheduledPaymentRepo.existsByUserAndId(spUuid, userUuid) == 0)
            throw new ApiException(HttpStatus.NOT_FOUND, "recurring payment not found");
        Object[] result = scheduledPaymentRepo.updateRecurringPayment(spUuid, userUuid, 
            request.name(), request.amount(),
            request.currency(), request.frequency(), request.nextDueDate(),
            mapper.toUuid(request.categoryId()));
        return mapper.mapToRecurringResponse(result); // mapper ya hace unwrap
    }

    public void deleteRecurringPayment(String userId, String id) {
        UUID userUuid = UUID.fromString(userId);
        UUID spUuid = UUID.fromString(id);
        int deleted = scheduledPaymentRepo.softDelete(spUuid, userUuid);
        if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "recurring payment not found");
    }

    // ==================== SYNC ====================
    @Transactional(readOnly = true)
    public ContractDtos.SyncPullResponse pullSync(String userId, Instant since, String entity) {
        Instant serverTime = Instant.now();
        List<ContractDtos.SyncEntityChange> changes = new ArrayList<>();
        UUID uuid = UUID.fromString(userId);

        if (entity == null || entity.equals("me")) {
            ContractDtos.MeResponse me = getMe(userId);
            if (since == null || isAfterOrEqual(me.updatedAt(), since))
                changes.add(new ContractDtos.SyncEntityChange("me", "upsert", objectMapper.valueToTree(me)));
        }
        if (entity == null || entity.equals("categories")) {
            listCategories(userId).stream().filter(r -> since == null || isAfterOrEqual(r.updatedAt(), since))
                .forEach(r -> changes.add(new ContractDtos.SyncEntityChange("category", "upsert", objectMapper.valueToTree(r))));
        }
        if (entity == null || entity.equals("accounts")) {
            listAccounts(userId).stream().filter(r -> since == null || isAfterOrEqual(r.updatedAt(), since))
                .forEach(r -> changes.add(new ContractDtos.SyncEntityChange("account", "upsert", objectMapper.valueToTree(r))));
        }
        if (entity == null || entity.equals("transactions")) {
            listTransactions(userId, new ContractDtos.TransactionFilters(null, null, null, null, null, 500, null))
                .stream().filter(r -> since == null || isAfterOrEqual(r.updatedAt(), since))
                .forEach(r -> changes.add(new ContractDtos.SyncEntityChange("transaction", "upsert", objectMapper.valueToTree(r))));
        }
        if (entity == null || entity.equals("debts")) {
            listDebts(userId).stream().filter(r -> since == null || isAfterOrEqual(r.updatedAt(), since))
                .forEach(r -> changes.add(new ContractDtos.SyncEntityChange("debt", "upsert", objectMapper.valueToTree(r))));
        }
        if (entity == null || entity.equals("recurring-payments")) {
            listRecurringPayments(userId).stream().filter(r -> since == null || isAfterOrEqual(r.updatedAt(), since))
                .forEach(r -> changes.add(new ContractDtos.SyncEntityChange("recurringPayment", "upsert", objectMapper.valueToTree(r))));
        }

        List<Map<String, Object>> deleted = new ArrayList<>();
        if (entity == null || entity.equals("categories")) deleted.addAll(categoryRepo.findDeleted(uuid, since));
        if (entity == null || entity.equals("accounts")) deleted.addAll(accountRepo.findDeleted(uuid, since));
        if (entity == null || entity.equals("transactions")) deleted.addAll(movementRepo.findDeleted(uuid, since));
        if (entity == null || entity.equals("debts")) deleted.addAll(debtRepo.findDeleted(uuid, since));
        if (entity == null || entity.equals("recurring-payments")) deleted.addAll(scheduledPaymentRepo.findDeleted(uuid, since));

        return new ContractDtos.SyncPullResponse(serverTime, changes, deleted);
    }

    public ContractDtos.SyncPushResponse pushSync(String userId, ContractDtos.SyncPushRequest request) {
        int accepted = 0, rejected = 0;
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (ContractDtos.SyncEntityChange change : request.changes()) {
            try { applySyncChange(userId, change, false); accepted++; }
            catch (ApiException ex) { rejected++; conflicts.add(Map.of("entity", change.entity(), "op", change.op(), "error", ex.getMessage(), "record", change.record())); }
        }
        return new ContractDtos.SyncPushResponse(accepted, rejected, conflicts);
    }

    public Map<String, Object> resolveConflict(String userId, ContractDtos.SyncConflictResolutionRequest request) {
        if ("server_wins".equals(request.resolution())) return Map.of("resolved", true, "strategy", "server_wins");
        JsonNode node = objectMapper.valueToTree(Map.of("id", request.id()));
        applySyncChange(userId, new ContractDtos.SyncEntityChange(request.entity(), "upsert", node), true);
        return Map.of("resolved", true, "strategy", request.resolution());
    }

    // ==================== SUMMARY ====================
    @Transactional(readOnly = true)
    public ContractDtos.SummaryResponse summary(String userId, String range, LocalDate from, LocalDate to, String accountId) {
        UUID uuid = UUID.fromString(userId);
        String currency = profileRepo.getUserCurrency(uuid);
        
        // Obtener el rango de fechas correcto
        LocalDate[] window = resolveWindow(range, from, to);
        LocalDate startDate = window[0];
        LocalDate endDate = window[1];
        
        // Obtener ingresos reales de transacciones en el rango
        BigDecimal realIncome = BigDecimal.ZERO;
        if (startDate != null && endDate != null) {
            Object[] incomeRow = movementRepo.getSummaryByDateRange(uuid, startDate, endDate);
            if (incomeRow != null) {
                incomeRow = mapper.unwrap(incomeRow);
                realIncome = mapper.toBigDecimal(incomeRow[0]);
            }
        }
        
        // Obtener monthlyIncome del perfil
        BigDecimal monthlyIncome = BigDecimal.ZERO;
        Object[] profileRow = profileRepo.getProfile(uuid);
        if (profileRow != null) {
            profileRow = mapper.unwrap(profileRow);
            // monthlyIncome está en la posición 6 del array
            if (profileRow.length > 6 && profileRow[6] != null) {
                monthlyIncome = mapper.toBigDecimal(profileRow[6]);
            }
        }
        
        // Calcular el ingreso proporcional según el rango
        BigDecimal finalIncome = realIncome;
        if (realIncome == null || realIncome.compareTo(BigDecimal.ZERO) == 0) {
            // Si no hay transacciones, usar monthlyIncome proporcional
            if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
                if ("biweekly".equals(range)) {
                    // Para quincena, la mitad del ingreso mensual
                    finalIncome = monthlyIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
                } else {
                    finalIncome = monthlyIncome;
                }
            } else {
                finalIncome = BigDecimal.ZERO;
            }
        }
        
        // Obtener gastos, pagos fijos y pagos de deudas en el rango
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal fixedPayments = BigDecimal.ZERO;
        BigDecimal debtPayments = BigDecimal.ZERO;
        
        if (startDate != null && endDate != null) {
            Object[] summaryRow = movementRepo.getSummaryByDateRange(uuid, startDate, endDate);
            if (summaryRow != null) {
                summaryRow = mapper.unwrap(summaryRow);
                expenses = mapper.toBigDecimal(summaryRow[1]);
                debtPayments = mapper.toBigDecimal(summaryRow[2]);
                fixedPayments = mapper.toBigDecimal(summaryRow[3]);
            }
        }
        
        // Calcular balance disponible
        BigDecimal availableBalance = finalIncome.subtract(expenses)
            .subtract(fixedPayments).subtract(debtPayments);
        
        return new ContractDtos.SummaryResponse(
            finalIncome,      // income
            expenses,         // expenses
            fixedPayments,    // fixedPayments
            debtPayments,     // debtPayments
            availableBalance, // availableBalance
            currency
        );
    }

    @Transactional(readOnly = true)
    public List<ContractDtos.CategoryStatResponse> categoryStats(String userId, String range, LocalDate from, LocalDate to, String accountId) {
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
            .map(r -> new ContractDtos.CategoryStatResponse(
                mapper.toString(r[0]), mapper.toString(r[1]), mapper.toBigDecimal(r[2]), BigDecimal.ZERO))
            .collect(Collectors.toList());
            
        BigDecimal total = raw.stream().map(ContractDtos.CategoryStatResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return raw.stream().map(item -> {
            BigDecimal pct = total.signum() == 0 ? BigDecimal.ZERO : 
                item.amount().multiply(BigDecimal.valueOf(100)).divide(total, 2, java.math.RoundingMode.HALF_UP);
            return new ContractDtos.CategoryStatResponse(item.categoryId(), item.categoryName(), item.amount(), pct);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractDtos.UpcomingResponse upcoming(String userId) {
        UUID uuid = UUID.fromString(userId);
        List<ContractDtos.UpcomingItemResponse> items = new ArrayList<>();
        items.addAll(scheduledPaymentRepo.getUpcomingRecurringPayments(uuid).stream()
            .map(r -> new ContractDtos.UpcomingItemResponse(mapper.toString(r[0]), mapper.toString(r[1]), mapper.toString(r[2]), mapper.toLocalDate(r[3]), mapper.toBigDecimal(r[4])))
            .collect(Collectors.toList()));
        items.addAll(debtRepo.getUpcomingDebts(uuid).stream()
            .map(r -> new ContractDtos.UpcomingItemResponse(mapper.toString(r[0]), mapper.toString(r[1]), mapper.toString(r[2]), mapper.toLocalDate(r[3]), mapper.toBigDecimal(r[4])))
            .collect(Collectors.toList()));
        return new ContractDtos.UpcomingResponse(items);
    }

    // ==================== BACKUP/IMPORT ====================
    public ContractDtos.BackupExportResponse exportBackup(String userId) {
        return new ContractDtos.BackupExportResponse("backup-finanzas.json", "/backup/download?userId=" + userId);
    }
    public ContractDtos.MigrationImportResponse importMigration(String userId, ContractDtos.MigrationImportRequest request) {
        return new ContractDtos.MigrationImportResponse(true, 0, Instant.now());
    }
    public Map<String, Object> importBackup(String userId, ContractDtos.BackupImportRequest request) {
        return Map.of("imported", true, "format", request.format());
    }

    // ==================== SYNC HELPERS Y LOGICA INTERNA ====================
    private void applySyncChange(String userId, ContractDtos.SyncEntityChange change, boolean force) {
        Map<String, Object> record = objectMapper.convertValue(change.record(), new TypeReference<>() {});
        String entity = change.entity();
        String id = string(record, "id");
        
        switch (entity) {
            case "category", "categories" -> {
                if ("delete".equals(change.op())) deleteCategory(userId, id);
                else if (categoryRepo.existsByUserAndId(UUID.fromString(id), UUID.fromString(userId)) > 0)
                    updateCategory(userId, id, objectMapper.convertValue(record, ContractDtos.UpsertCategoryRequest.class));
                else createCategory(userId, objectMapper.convertValue(record, ContractDtos.UpsertCategoryRequest.class));
            }
            case "account", "accounts" -> {
                if ("delete".equals(change.op())) deleteAccount(userId, id);
                else if (accountRepo.existsByUserAndId(UUID.fromString(id), UUID.fromString(userId)) > 0)
                    updateAccount(userId, id, objectMapper.convertValue(record, ContractDtos.UpsertAccountRequest.class));
                else createAccount(userId, objectMapper.convertValue(record, ContractDtos.UpsertAccountRequest.class));
            }
            case "transaction", "transactions" -> {
                if ("delete".equals(change.op())) deleteTransaction(userId, id);
                else if (movementRepo.existsByUserAndId(UUID.fromString(id), UUID.fromString(userId)) > 0)
                    updateTransaction(userId, id, objectMapper.convertValue(record, ContractDtos.UpsertTransactionRequest.class));
                else createTransaction(userId, objectMapper.convertValue(record, ContractDtos.UpsertTransactionRequest.class));
            }
            case "debt", "debts" -> {
                if ("delete".equals(change.op())) deleteDebt(userId, id);
                else if (debtRepo.existsByUserAndId(UUID.fromString(id), UUID.fromString(userId)) > 0)
                    updateDebt(userId, id, objectMapper.convertValue(record, ContractDtos.UpsertDebtRequest.class));
                else createDebt(userId, objectMapper.convertValue(record, ContractDtos.UpsertDebtRequest.class));
            }
            case "recurringPayment", "recurring-payments", "scheduled_payments" -> {
                if ("delete".equals(change.op())) deleteRecurringPayment(userId, id);
                else if (scheduledPaymentRepo.existsByUserAndId(UUID.fromString(id), UUID.fromString(userId)) > 0)
                    updateRecurringPayment(userId, id, objectMapper.convertValue(record, ContractDtos.UpsertRecurringPaymentRequest.class));
                else createRecurringPayment(userId, objectMapper.convertValue(record, ContractDtos.UpsertRecurringPaymentRequest.class));
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "unsupported entity: " + entity);
        }
    }

    private static String string(Map<String, Object> map, String key) { 
        Object v = map.get(key); 
        return v == null ? null : v.toString(); 
    }
    
    private String toJson(List<Integer> payDays) { 
        try { return objectMapper.writeValueAsString(payDays); } 
        catch (Exception e) { return "[]"; } 
    }
    
    private boolean isAfterOrEqual(Instant value, Instant since) { 
        return value != null && (value.equals(since) || value.isAfter(since)); 
    }
    
    private LocalDate[] resolveWindow(String range, LocalDate from, LocalDate to) {
        if (from != null || to != null) return new LocalDate[]{from, to};
        LocalDate today = LocalDate.now();
        return switch (range == null ? "monthly" : range) {
            case "biweekly" -> today.getDayOfMonth() <= 15 ? 
                new LocalDate[]{today.withDayOfMonth(1), today.withDayOfMonth(15)} : 
                new LocalDate[]{today.withDayOfMonth(16), today.withDayOfMonth(today.lengthOfMonth())};
            case "custom" -> new LocalDate[]{today.minusDays(30), today};
            default -> new LocalDate[]{today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth())};
        };
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
        
        return rows.stream()
            .map(mapper::mapToInstallmentResponse)
            .collect(Collectors.toList());
    }

    public ContractDtos.InstallmentResponse createInstallment(String userId, ContractDtos.UpsertInstallmentRequest request) {
        UUID uuid = UUID.fromString(userId);
        UUID debtUuid = UUID.fromString(request.debtId());
        
        // Verificar que la deuda existe y pertenece al usuario
        if (debtRepo.existsByUserAndId(debtUuid, uuid) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "debt not found");
        }
        
        UUID installmentId = installmentRepo.createInstallment(
            uuid, debtUuid, request.number(), request.amount(), 
            request.dueDate(), request.paid() != null ? request.paid() : false
        );
        
        Object[] row = installmentRepo.getInstallmentById(uuid, installmentId);
        return mapper.mapToInstallmentResponse(row);
    }

    public ContractDtos.InstallmentResponse updateInstallment(String userId, String id, ContractDtos.UpsertInstallmentRequest request) {
        UUID uuid = UUID.fromString(userId);
        UUID installmentUuid = UUID.fromString(id);
        
        UUID updatedId = installmentRepo.updateInstallment(
            uuid, installmentUuid, request.number(), request.amount(),
            request.dueDate(), request.paid()
        );
        
        Object[] row = installmentRepo.getInstallmentById(uuid, updatedId);
        return mapper.mapToInstallmentResponse(row);
    }

    public ContractDtos.InstallmentResponse markInstallmentAsPaid(String userId, String id) {
        UUID uuid = UUID.fromString(userId);
        UUID installmentUuid = UUID.fromString(id);
        
        int updated = installmentRepo.markAsPaid(uuid, installmentUuid);
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "installment not found or already paid");
        }
        
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
}