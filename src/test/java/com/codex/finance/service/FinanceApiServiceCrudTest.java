package com.codex.finance.service;

import com.codex.finance.client.SupabaseAuthClient;
import com.codex.finance.dto.ContractDtos;
import com.codex.finance.exception.ApiException;
import com.codex.finance.mapper.FinanceMapper;
import com.codex.finance.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceApiServiceCrudTest {

    @Mock private ProfileRepository profileRepo;
    @Mock private CategoryRepository categoryRepo;
    @Mock private AccountRepository accountRepo;
    @Mock private MovementRepository movementRepo;
    @Mock private DebtRepository debtRepo;
    @Mock private ScheduledPaymentRepository scheduledPaymentRepo;
    @Mock private SupabaseAuthClient authClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private FinanceMapper mapper;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private InstallmentRepository installmentRepo;
    @Mock private FinancialGoalRepository financialGoalRepo;
    @Mock private BudgetRepository budgetRepo;

    private FinanceApiService service;
    private final String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        service = new FinanceApiService(
            profileRepo, categoryRepo, accountRepo, movementRepo,
            debtRepo, scheduledPaymentRepo, authClient, objectMapper,
            mapper, jdbcTemplate, installmentRepo, financialGoalRepo, budgetRepo
        );
    }

    // ==================== CATEGORIES ====================

    @Test
    void listCategories_returnsList() {
        when(categoryRepo.listCategories(any())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToCategoryResponse(any())).thenReturn(new ContractDtos.CategoryResponse("id", null, "Food", null, null, "expense", null, null, null, null, 0));
        List<ContractDtos.CategoryResponse> result = service.listCategories(userId);
        assertEquals(1, result.size());
        verify(categoryRepo).listCategories(any());
    }

    @Test
    void createCategory_returnsResponse() {
        var request = new ContractDtos.UpsertCategoryRequest("Food", "#f00", "icon", "expense");
        when(categoryRepo.createCategory(any(), eq("Food"), any(), any(), eq("expense"))).thenReturn(new Object[]{"1"});
        when(mapper.mapToCategoryResponse(any())).thenReturn(new ContractDtos.CategoryResponse("id", null, "Food", null, null, "expense", null, null, null, null, 0));
        ContractDtos.CategoryResponse result = service.createCategory(userId, request);
        assertNotNull(result);
        verify(categoryRepo).createCategory(any(), eq("Food"), eq("#f00"), eq("icon"), eq("expense"));
    }

    @Test
    void updateCategory_throwsNotFound_whenMissing() {
        when(categoryRepo.existsByUserAndId(any(), any())).thenReturn(0);
        var request = new ContractDtos.UpsertCategoryRequest("Food", null, null, "expense");
        assertThrows(ApiException.class, () -> service.updateCategory(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void updateCategory_returnsResponse() {
        String catId = UUID.randomUUID().toString();
        when(categoryRepo.existsByUserAndId(any(), any())).thenReturn(1);
        var request = new ContractDtos.UpsertCategoryRequest("Food", "#f00", "icon", "expense");
        when(categoryRepo.updateCategory(any(), any(), eq("Food"), eq("#f00"), eq("icon"), eq("expense"))).thenReturn(new Object[]{"1"});
        when(mapper.mapToCategoryResponse(any())).thenReturn(new ContractDtos.CategoryResponse(catId, null, "Food", null, null, "expense", null, null, null, null, 0));
        ContractDtos.CategoryResponse result = service.updateCategory(userId, catId, request);
        assertNotNull(result);
    }

    @Test
    void deleteCategory_throwsNotFound_whenMissing() {
        when(categoryRepo.softDelete(any(), any())).thenReturn(0);
        assertThrows(ApiException.class, () -> service.deleteCategory(userId, UUID.randomUUID().toString()));
    }

    @Test
    void deleteCategory_deletesSuccessfully() {
        when(categoryRepo.softDelete(any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> service.deleteCategory(userId, UUID.randomUUID().toString()));
    }

    // ==================== ACCOUNTS ====================

    @Test
    void listAccounts_returnsList() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(accountRepo.listAccounts(any(), any())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToAccountResponse(any())).thenReturn(new ContractDtos.AccountResponse("id", null, "debit", "Acct", "Bank", "MXN", BigDecimal.ZERO, null, null, null, true, null, null, null, null, 0));
        List<ContractDtos.AccountResponse> result = service.listAccounts(userId);
        assertEquals(1, result.size());
    }

    @Test
    void createAccount_returnsResponse() {
        var request = new ContractDtos.UpsertAccountRequest("debit", "Acct", "Bank", "MXN", BigDecimal.TEN, BigDecimal.ZERO, 1, 10, true);
        when(accountRepo.createAccount(any(), eq("Acct"), eq("debit"), eq("Bank"), any(), any(), any(), any(), any(), eq("MXN"))).thenReturn(new Object[]{"1"});
        when(mapper.mapToAccountResponse(any())).thenReturn(new ContractDtos.AccountResponse("id", null, "debit", "Acct", "Bank", "MXN", BigDecimal.TEN, BigDecimal.ZERO, 1, 10, true, null, null, null, null, 0));
        ContractDtos.AccountResponse result = service.createAccount(userId, request);
        assertNotNull(result);
    }

    @Test
    void updateAccount_throwsNotFound_whenMissing() {
        when(accountRepo.existsByUserAndId(any(), any())).thenReturn(0);
        var request = new ContractDtos.UpsertAccountRequest("debit", "Acct", "Bank", "MXN", BigDecimal.ZERO, BigDecimal.ZERO, null, null, true);
        assertThrows(ApiException.class, () -> service.updateAccount(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void updateAccount_returnsResponse() {
        when(accountRepo.existsByUserAndId(any(), any())).thenReturn(1);
        var request = new ContractDtos.UpsertAccountRequest("debit", "Acct", "Bank", "MXN", BigDecimal.TEN, BigDecimal.ZERO, 1, 10, true);
        Object[] repoResult = new Object[]{"1"};
        when(accountRepo.updateAccount(any(), any(), eq("Acct"), eq("debit"), eq("Bank"), any(), any(), any(), any(), any())).thenReturn(repoResult);
        when(mapper.mapToAccountResponseUpdate(any(), eq("MXN"))).thenReturn(new ContractDtos.AccountResponse("id", null, "debit", "Acct", "Bank", "MXN", BigDecimal.TEN, BigDecimal.ZERO, 1, 10, true, null, null, null, null, 0));
        ContractDtos.AccountResponse result = service.updateAccount(userId, UUID.randomUUID().toString(), request);
        assertNotNull(result);
    }

    @Test
    void deleteAccount_throwsNotFound_whenMissing() {
        when(accountRepo.softDelete(any(), any())).thenReturn(0);
        assertThrows(ApiException.class, () -> service.deleteAccount(userId, UUID.randomUUID().toString()));
    }

    @Test
    void deleteAccount_deletesSuccessfully() {
        when(accountRepo.softDelete(any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> service.deleteAccount(userId, UUID.randomUUID().toString()));
    }

    // ==================== TRANSACTIONS ====================

    @Test
    void deleteTransaction_throwsNotFound_whenMissing() {
        when(movementRepo.softDelete(any(), any())).thenReturn(0);
        assertThrows(ApiException.class, () -> service.deleteTransaction(userId, UUID.randomUUID().toString()));
    }

    @Test
    void deleteTransaction_deletesSuccessfully() {
        when(movementRepo.softDelete(any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> service.deleteTransaction(userId, UUID.randomUUID().toString()));
    }

    @Test
    void updateTransaction_throwsNotFound_whenMissing() {
        when(movementRepo.existsByUserAndId(any(), any())).thenReturn(0);
        var request = new ContractDtos.UpsertTransactionRequest(UUID.randomUUID().toString(), null, null, null, "expense", "test", BigDecimal.ONE, "MXN", LocalDate.now(), null);
        assertThrows(ApiException.class, () -> service.updateTransaction(userId, UUID.randomUUID().toString(), request));
    }

    // ==================== DEBTS ====================

    @Test
    void listDebts_returnsList() {
        when(debtRepo.listDebts(any())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToDebtResponse(any())).thenReturn(new ContractDtos.DebtResponse("id", null, "Debt", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, "monthly", LocalDate.now(), null, null, null, null, null, 0));
        List<ContractDtos.DebtResponse> result = service.listDebts(userId);
        assertEquals(1, result.size());
    }

    @Test
    void createDebt_returnsResponse() {
        var request = new ContractDtos.UpsertDebtRequest("Debt", BigDecimal.TEN, BigDecimal.ONE, "monthly", LocalDate.now(), "notes");
        when(debtRepo.createDebt(any(), eq("Debt"), eq(BigDecimal.TEN), eq(BigDecimal.ONE), eq("monthly"), any(), eq("notes"))).thenReturn(new Object[]{"1"});
        when(mapper.mapToDebtResponse(any())).thenReturn(new ContractDtos.DebtResponse("id", null, "Debt", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, "monthly", LocalDate.now(), null, null, null, null, null, 0));
        assertNotNull(service.createDebt(userId, request));
    }

    @Test
    void updateDebt_throwsNotFound_whenMissing() {
        when(debtRepo.existsByUserAndId(any(), any())).thenReturn(0);
        var request = new ContractDtos.UpsertDebtRequest("Debt", BigDecimal.TEN, BigDecimal.ONE, "monthly", null, null);
        assertThrows(ApiException.class, () -> service.updateDebt(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void updateDebt_returnsResponse() {
        when(debtRepo.existsByUserAndId(any(), any())).thenReturn(1);
        var request = new ContractDtos.UpsertDebtRequest("Debt", BigDecimal.TEN, BigDecimal.ONE, "monthly", LocalDate.now(), "notes");
        when(debtRepo.updateDebt(any(), any(), eq("Debt"), eq(BigDecimal.TEN), eq(BigDecimal.ONE), eq("monthly"), any(), eq("notes"))).thenReturn(new Object[]{"1"});
        when(mapper.mapToDebtResponse(any())).thenReturn(new ContractDtos.DebtResponse("id", null, "Debt", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, "monthly", LocalDate.now(), null, null, null, null, null, 0));
        assertNotNull(service.updateDebt(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void deleteDebt_throwsNotFound_whenMissing() {
        when(debtRepo.softDelete(any(), any())).thenReturn(0);
        assertThrows(ApiException.class, () -> service.deleteDebt(userId, UUID.randomUUID().toString()));
    }

    @Test
    void deleteDebt_deletesSuccessfully() {
        when(debtRepo.softDelete(any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> service.deleteDebt(userId, UUID.randomUUID().toString()));
    }

    // ==================== RECURRING PAYMENTS ====================

    @Test
    void listRecurringPayments_returnsList() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(scheduledPaymentRepo.listRecurringPayments(any(), any())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToRecurringResponse(any())).thenReturn(new ContractDtos.RecurringPaymentResponse("id", null, "Rent", BigDecimal.ONE, "MXN", "monthly", LocalDate.now(), null, null, null, null, null, 0));
        assertEquals(1, service.listRecurringPayments(userId).size());
    }

    @Test
    void createRecurringPayment_returnsResponse() {
        var request = new ContractDtos.UpsertRecurringPaymentRequest("Rent", BigDecimal.ONE, "MXN", "monthly", LocalDate.now(), null, null, null);
        when(scheduledPaymentRepo.createRecurringPayment(any(), eq("Rent"), eq(BigDecimal.ONE), eq("MXN"), eq("monthly"), any(), isNull(), eq("expense"))).thenReturn(new Object[]{"1"});
        when(mapper.mapToRecurringResponse(any())).thenReturn(new ContractDtos.RecurringPaymentResponse("id", null, "Rent", BigDecimal.ONE, "MXN", "monthly", LocalDate.now(), null, null, null, null, null, 0));
        assertNotNull(service.createRecurringPayment(userId, request));
    }

    @Test
    void updateRecurringPayment_throwsNotFound_whenMissing() {
        when(scheduledPaymentRepo.existsByUserAndId(any(), any())).thenReturn(0);
        var request = new ContractDtos.UpsertRecurringPaymentRequest("Rent", BigDecimal.ONE, "MXN", "monthly", LocalDate.now(), null, null, null);
        assertThrows(ApiException.class, () -> service.updateRecurringPayment(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void updateRecurringPayment_returnsResponse() {
        when(scheduledPaymentRepo.existsByUserAndId(any(), any())).thenReturn(1);
        var request = new ContractDtos.UpsertRecurringPaymentRequest("Rent", BigDecimal.ONE, "MXN", "monthly", LocalDate.now(), null, null, "income");
        when(scheduledPaymentRepo.updateRecurringPayment(any(), any(), eq("Rent"), eq(BigDecimal.ONE), eq("MXN"), eq("monthly"), any(), isNull(), eq("income"))).thenReturn(new Object[]{"1"});
        when(mapper.mapToRecurringResponse(any())).thenReturn(new ContractDtos.RecurringPaymentResponse("id", null, "Rent", BigDecimal.ONE, "MXN", "monthly", LocalDate.now(), null, null, null, null, null, 0));
        assertNotNull(service.updateRecurringPayment(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void deleteRecurringPayment_throwsNotFound_whenMissing() {
        when(scheduledPaymentRepo.softDelete(any(), any())).thenReturn(0);
        assertThrows(ApiException.class, () -> service.deleteRecurringPayment(userId, UUID.randomUUID().toString()));
    }

    @Test
    void deleteRecurringPayment_deletesSuccessfully() {
        when(scheduledPaymentRepo.softDelete(any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> service.deleteRecurringPayment(userId, UUID.randomUUID().toString()));
    }

    // ==================== INSTALLMENTS ====================

    @Test
    void listInstallments_returnsList() {
        when(installmentRepo.listInstallments(any(), anyInt())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToInstallmentResponse(any())).thenReturn(new ContractDtos.InstallmentResponse("id", null, 1, BigDecimal.ONE, LocalDate.now(), false, null, null, null, null, null, null, 0));
        assertEquals(1, service.listInstallments(userId, null).size());
    }

    @Test
    void listInstallments_withDebtId_returnsFiltered() {
        when(installmentRepo.listInstallmentsByDebt(any(), any())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToInstallmentResponse(any())).thenReturn(new ContractDtos.InstallmentResponse("id", null, 1, BigDecimal.ONE, LocalDate.now(), false, null, null, null, null, null, null, 0));
        assertEquals(1, service.listInstallments(userId, UUID.randomUUID().toString()).size());
        verify(installmentRepo).listInstallmentsByDebt(any(), any());
    }

    @Test
    void createInstallment_throwsNotFound_whenDebtMissing() {
        when(debtRepo.existsByUserAndId(any(), any())).thenReturn(0);
        var request = new ContractDtos.UpsertInstallmentRequest(UUID.randomUUID().toString(), null, 1, BigDecimal.ONE, LocalDate.now(), false, null, null);
        assertThrows(ApiException.class, () -> service.createInstallment(userId, request));
    }

    @Test
    void createInstallment_returnsResponse() {
        String debtId = UUID.randomUUID().toString();
        when(debtRepo.existsByUserAndId(any(), any())).thenReturn(1);
        var request = new ContractDtos.UpsertInstallmentRequest(debtId, null, 1, BigDecimal.ONE, LocalDate.now(), true, null, null);
        doNothing().when(installmentRepo).createInstallment(any(), any(), eq(1), eq(BigDecimal.ONE), any(), eq(true));
        when(installmentRepo.listInstallmentsByDebt(any(), any())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToInstallmentResponse(any())).thenReturn(new ContractDtos.InstallmentResponse("id", debtId, 1, BigDecimal.ONE, LocalDate.now(), true, null, null, null, null, null, null, 0));
        assertNotNull(service.createInstallment(userId, request));
    }

    @Test
    void updateInstallment_returnsResponse() {
        var request = new ContractDtos.UpsertInstallmentRequest(null, null, 2, BigDecimal.TEN, LocalDate.now(), true, null, null);
        doNothing().when(installmentRepo).updateInstallment(any(), any(), eq(2), eq(BigDecimal.TEN), any(), eq(true));
        when(installmentRepo.getInstallmentById(any(), any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToInstallmentResponse(any())).thenReturn(new ContractDtos.InstallmentResponse("id", null, 2, BigDecimal.TEN, LocalDate.now(), true, null, null, null, null, null, null, 0));
        assertNotNull(service.updateInstallment(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void deleteInstallment_throwsNotFound_whenMissing() {
        when(installmentRepo.softDeleteInstallment(any(), any())).thenReturn(0);
        assertThrows(ApiException.class, () -> service.deleteInstallment(userId, UUID.randomUUID().toString()));
    }

    @Test
    void deleteInstallment_deletesSuccessfully() {
        when(installmentRepo.softDeleteInstallment(any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> service.deleteInstallment(userId, UUID.randomUUID().toString()));
    }

    // ==================== FINANCIAL GOALS ====================

    @Test
    void listGoals_returnsList() {
        when(financialGoalRepo.listGoals(any())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToFinancialGoalResponse(any())).thenReturn(new ContractDtos.FinancialGoalResponse("id", "Goal", BigDecimal.TEN, BigDecimal.ZERO, LocalDate.now(), "active", null, null, BigDecimal.ZERO));
        assertEquals(1, service.listGoals(userId).size());
    }

    @Test
    void createGoal_returnsResponse() {
        var request = new ContractDtos.UpsertFinancialGoalRequest("Goal", BigDecimal.TEN, null, LocalDate.now(), "active");
        doNothing().when(financialGoalRepo).createGoal(any(), eq("Goal"), eq(BigDecimal.TEN), eq(BigDecimal.ZERO), any(), eq("active"));
        when(financialGoalRepo.listGoals(any())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToFinancialGoalResponse(any())).thenReturn(new ContractDtos.FinancialGoalResponse("id", "Goal", BigDecimal.TEN, BigDecimal.ZERO, LocalDate.now(), "active", null, null, BigDecimal.ZERO));
        assertNotNull(service.createGoal(userId, request));
    }

    @Test
    void updateGoal_returnsResponse() {
        var request = new ContractDtos.UpsertFinancialGoalRequest("Goal", BigDecimal.TEN, BigDecimal.ONE, LocalDate.now(), "active");
        doNothing().when(financialGoalRepo).updateGoal(any(), any(), eq("Goal"), eq(BigDecimal.TEN), eq(BigDecimal.ONE), any(), eq("active"));
        when(financialGoalRepo.getGoalById(any(), any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToFinancialGoalResponse(any())).thenReturn(new ContractDtos.FinancialGoalResponse("id", "Goal", BigDecimal.TEN, BigDecimal.ONE, LocalDate.now(), "active", null, null, BigDecimal.ZERO));
        assertNotNull(service.updateGoal(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void deleteGoal_deletesSuccessfully() {
        when(financialGoalRepo.softDeleteGoal(any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> service.deleteGoal(userId, UUID.randomUUID().toString()));
    }

    // ==================== BUDGETS ====================

    @Test
    void listBudgets_returnsList() {
        when(budgetRepo.listActiveBudgets(any())).thenReturn(List.<Object[]>of(new Object[]{"1"}));
        when(mapper.mapToBudgetResponse(any())).thenReturn(new ContractDtos.BudgetResponse("id", "catId", "Food", "monthly", LocalDate.now(), LocalDate.now(), BigDecimal.TEN, BigDecimal.valueOf(0.8), BigDecimal.ZERO, BigDecimal.ZERO, false));
        assertEquals(1, service.listBudgets(userId).size());
    }

    @Test
    void updateBudget_throwsNotFound_whenRowNull() {
        when(budgetRepo.getBudgetById(any(), any())).thenReturn(null);
        var request = new ContractDtos.UpsertBudgetRequest(UUID.randomUUID().toString(), "monthly", LocalDate.now(), LocalDate.now(), BigDecimal.TEN, null);
        assertThrows(ApiException.class, () -> service.updateBudget(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void updateBudget_returnsResponse() {
        String budgetId = UUID.randomUUID().toString();
        doNothing().when(budgetRepo).updateBudget(any(), any(), any(), anyString(), any(), any(), any(), any());
        String catId = UUID.randomUUID().toString();
        when(budgetRepo.getBudgetById(any(), any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToBudgetResponse(any())).thenReturn(new ContractDtos.BudgetResponse(budgetId, catId, "Food", "monthly", LocalDate.now(), LocalDate.now(), BigDecimal.TEN, BigDecimal.valueOf(0.8), BigDecimal.ZERO, BigDecimal.ZERO, false));
        var request = new ContractDtos.UpsertBudgetRequest(catId, "monthly", LocalDate.now(), LocalDate.now(), BigDecimal.TEN, null);
        assertNotNull(service.updateBudget(userId, budgetId, request));
    }

    @Test
    void deleteBudget_deletesSuccessfully() {
        when(budgetRepo.softDeleteBudget(any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> service.deleteBudget(userId, UUID.randomUUID().toString()));
    }

    // ==================== SIGN OUT ====================

    @Test
    void signOut_throwsBadRequest_whenNoToken() {
        var request = new ContractDtos.SignOutRequest("refresh");
        ApiException ex = assertThrows(ApiException.class, () -> service.signOut(null, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void signOut_callsAuthClient() {
        var request = new ContractDtos.SignOutRequest("refresh");
        doNothing().when(authClient).signOut("Bearer token");
        assertDoesNotThrow(() -> service.signOut("Bearer token", request));
        verify(authClient).signOut("Bearer token");
    }

    // ==================== AUTH ====================

    @Test
    void signUp_delegatesToAuthClient() {
        var request = new ContractDtos.SignUpRequest("a@b.com", "pass", "A");
        when(authClient.signUp(request)).thenReturn(new ContractDtos.AuthResponse(null, null));
        assertNotNull(service.signUp(request));
    }

    @Test
    void signIn_delegatesToAuthClient() {
        var request = new ContractDtos.SignInRequest("a@b.com", "pass");
        when(authClient.signIn(request)).thenReturn(new ContractDtos.AuthResponse(null, null));
        assertNotNull(service.signIn(request));
    }

    @Test
    void refresh_delegatesToAuthClient() {
        var request = new ContractDtos.RefreshRequest("rt");
        when(authClient.refresh(request)).thenReturn(new ContractDtos.SessionResponse(null, null));
        assertNotNull(service.refresh(request));
    }

    @Test
    void session_delegatesToAuthClient() {
        when(authClient.currentSession("at", "rt")).thenReturn(new ContractDtos.SessionResponse(null, null));
        assertNotNull(service.session("at", "rt"));
    }

    // ==================== PROFILE ====================

    @Test
    void getMe_throwsNotFound_whenProfileMissing() {
        when(profileRepo.getProfile(any())).thenReturn(null);
        assertThrows(ApiException.class, () -> service.getMe(userId));
    }

    @Test
    void getMe_returnsResponse() {
        when(profileRepo.getProfile(any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToMeResponse(any())).thenReturn(new ContractDtos.MeResponse("id", userId, "A", "MXN", "monthly", List.of(15), BigDecimal.ZERO, null, null, null, null, null, 0));
        assertNotNull(service.getMe(userId));
    }

    @Test
    void updateMe_returnsResponse() throws Exception {
        var request = new ContractDtos.UpdateMeRequest("NewName", "MXN", "monthly", List.of(15, 30), BigDecimal.valueOf(50000), null);
        when(profileRepo.getSettingsJson(any())).thenReturn("{}");
        when(objectMapper.readValue(eq("{}"), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(new java.util.HashMap<String, Object>());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"monthlyIncome\":50000}");
        when(profileRepo.upsertProfile(any(), eq("NewName"), eq("MXN"), any(), any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToMeResponse(any())).thenReturn(new ContractDtos.MeResponse("id", userId, "NewName", "MXN", "monthly", List.of(15, 30), BigDecimal.valueOf(50000), null, null, null, null, null, 0));
        assertNotNull(service.updateMe(userId, request));
    }

    // ==================== SYNC ====================

    @Test
    void pullSync_returnsResponse() {
        when(profileRepo.getProfile(any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToMeResponse(any())).thenReturn(new ContractDtos.MeResponse("id", userId, "A", "MXN", "monthly", List.of(15), BigDecimal.ZERO, null, null, null, null, null, 0));
        ContractDtos.SyncPullResponse result = service.pullSync(userId, null, "me");
        assertNotNull(result);
        assertNotNull(result.serverTime());
    }

    @Test
    void pushSync_returnsResponse() {
        var request = new ContractDtos.SyncPushRequest("dev1", List.of());
        ContractDtos.SyncPushResponse result = service.pushSync(userId, request);
        assertTrue(result.accepted() >= 0);
    }

    @Test
    void resolveConflict_returnsResponse() {
        var request = new ContractDtos.SyncConflictResolutionRequest("category", "id", "use_local");
        Map<String, Object> result = service.resolveConflict(userId, request);
        assertTrue((Boolean) result.get("resolved"));
    }

    @Test
    void exportBackup_returnsResponse() {
        ContractDtos.BackupExportResponse result = service.exportBackup(userId);
        assertNotNull(result.fileName());
    }

    @Test
    void importMigration_returnsResponse() {
        var request = new ContractDtos.MigrationImportRequest("mint", 1, null);
        ContractDtos.MigrationImportResponse result = service.importMigration(userId, request);
        assertTrue(result.imported());
    }

    @Test
    void importBackup_returnsResponse() {
        var request = new ContractDtos.BackupImportRequest("json", false, null);
        Map<String, Object> result = service.importBackup(userId, request);
        assertTrue((Boolean) result.get("imported"));
    }

    // ==================== REPORTS ====================

    @Test
    void getMonthlyReports_returnsList() {
        when(movementRepo.getMonthlyReport(any(), any())).thenReturn(List.<Object[]>of(
            new Object[]{ java.sql.Date.valueOf(LocalDate.of(2026, 1, 1)), BigDecimal.valueOf(1000), BigDecimal.valueOf(500) }
        ));
        when(mapper.toBigDecimal(any())).thenCallRealMethod();

        List<ContractDtos.MonthlyReportResponse> result = service.getMonthlyReports(userId, 2026);
        assertEquals(1, result.size());
        assertEquals("2026-01-01", result.get(0).yearMonth());
    }

    @Test
    void getPaymentSummary_returnsMap() {
        when(movementRepo.getCreditCardPayments(any(), any(), any())).thenReturn(BigDecimal.valueOf(500));
        when(movementRepo.getDebtPayments(any(), any(), any())).thenReturn(BigDecimal.valueOf(300));
        Map<String, BigDecimal> result = service.getPaymentSummary(userId, "monthly");
        assertEquals(BigDecimal.valueOf(500), result.get("creditCardPayments"));
        assertEquals(BigDecimal.valueOf(300), result.get("debtPayments"));
        assertEquals(BigDecimal.valueOf(800), result.get("totalPayments"));
    }

    // ==================== TYPE VALIDATION ====================

    @Test
    void createTransaction_acceptsValidTypes() {
        String accountId = UUID.randomUUID().toString();
        when(mapper.toUuid(any())).thenCallRealMethod();
        when(movementRepo.createTransaction(any(), any(), isNull(), isNull(), eq("expense"), any(), any(), any(), any(), any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToTransactionResponse(any())).thenReturn(null);
        var request = new ContractDtos.UpsertTransactionRequest(accountId, null, null, null, "expense", "test", BigDecimal.ONE, "MXN", LocalDate.now(), null);
        service.createTransaction(userId, request);
        verify(movementRepo).createTransaction(any(), any(), isNull(), isNull(), eq("expense"), any(), any(), any(), any(), any());
    }

    @Test
    void createTransaction_convertsWithdrawalToExpense() {
        String accountId = UUID.randomUUID().toString();
        when(mapper.toUuid(any())).thenCallRealMethod();
        when(movementRepo.createTransaction(any(), any(), isNull(), isNull(), eq("expense"), any(), any(), any(), any(), any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToTransactionResponse(any())).thenReturn(null);
        var request = new ContractDtos.UpsertTransactionRequest(accountId, null, null, null, "withdrawal", "test", BigDecimal.ONE, "MXN", LocalDate.now(), null);
        service.createTransaction(userId, request);
        verify(movementRepo).createTransaction(any(), any(), isNull(), isNull(), eq("expense"), any(), any(), any(), any(), any());
    }

    @Test
    void createTransaction_defaultsNullTypeToExpense() {
        String accountId = UUID.randomUUID().toString();
        when(mapper.toUuid(any())).thenCallRealMethod();
        when(movementRepo.createTransaction(any(), any(), isNull(), isNull(), eq("expense"), any(), any(), any(), any(), any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToTransactionResponse(any())).thenReturn(null);
        var request = new ContractDtos.UpsertTransactionRequest(accountId, null, null, null, null, "test", BigDecimal.ONE, "MXN", LocalDate.now(), null);
        service.createTransaction(userId, request);
        verify(movementRepo).createTransaction(any(), any(), isNull(), isNull(), eq("expense"), any(), any(), any(), any(), any());
    }

    @Test
    void createTransaction_rejectsInvalidType() {
        String accountId = UUID.randomUUID().toString();
        when(mapper.toUuid(any())).thenCallRealMethod();
        when(movementRepo.createTransaction(any(), any(), isNull(), isNull(), eq("expense"), any(), any(), any(), any(), any())).thenReturn(new Object[]{"1"});
        when(mapper.mapToTransactionResponse(any())).thenReturn(null);
        var request = new ContractDtos.UpsertTransactionRequest(accountId, null, null, null, "invalid_type", "test", BigDecimal.ONE, "MXN", LocalDate.now(), null);
        service.createTransaction(userId, request);
        verify(movementRepo).createTransaction(any(), any(), isNull(), isNull(), eq("expense"), any(), any(), any(), any(), any());
    }

    // ==================== CREDIT CARD INSTALLMENT ====================

    @Test
    void createCreditCardInstallment_throwsNotFound_whenAccountMissing() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(accountRepo.getAccountById(any(), any(), any())).thenReturn(null);
        var request = new ContractDtos.UpsertInstallmentRequest(null, UUID.randomUUID().toString(), 12, BigDecimal.valueOf(1000), LocalDate.now(), false, null, null);
        assertThrows(ApiException.class, () -> service.createCreditCardInstallment(userId, request));
    }

    @Test
    void createCreditCardInstallment_throwsBadRequest_whenNotCredit() {
        String accountId = UUID.randomUUID().toString();
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        Object[] accountRow = { UUID.fromString(accountId), UUID.fromString(userId), "debit", "Acct", "Bank", BigDecimal.ZERO, BigDecimal.ZERO, null, null, true, "MXN", null, null, null, "synced", 1L };
        when(accountRepo.getAccountById(any(), any(), any())).thenReturn(accountRow);
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toString(any())).thenCallRealMethod();
        var request = new ContractDtos.UpsertInstallmentRequest(null, accountId, 12, BigDecimal.valueOf(1000), LocalDate.now(), false, null, null);
        assertThrows(ApiException.class, () -> service.createCreditCardInstallment(userId, request));
    }

    @Test
    void createCreditCardInstallment_createsDebtWhenNoDebtId() {
        String accountId = UUID.randomUUID().toString();
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        Object[] accountRow = { UUID.fromString(accountId), UUID.fromString(userId), "credit", "Visa", "Bank", BigDecimal.ZERO, BigDecimal.valueOf(50000), 15, 10, true, "MXN", null, null, null, "synced", 1L };
        when(accountRepo.getAccountById(any(), any(), any())).thenReturn(accountRow);
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toString(any())).thenCallRealMethod();
        lenient().when(mapper.toBigDecimal(any())).thenCallRealMethod();
        Object[] debtRow = { UUID.randomUUID(), "Compra a meses - 12 meses", BigDecimal.valueOf(12000), BigDecimal.valueOf(1000), "monthly", "2026-07-01", "Compra a meses con tarjeta" };
        lenient().when(debtRepo.createDebt(any(), anyString(), any(), any(), any(), any(), anyString())).thenReturn(debtRow);
        doNothing().when(installmentRepo).createCreditCardInstallment(any(), any(), any(), anyInt(), any(), any(), any(), any(), anyBoolean());
        Object[] installmentRow = { UUID.randomUUID(), UUID.randomUUID(), 1, BigDecimal.valueOf(1000), LocalDate.of(2026, 7, 1), false, null, null, null, null, null, null, 1L };
        when(installmentRepo.listInstallmentsByAccount(any(), any())).thenReturn(List.<Object[]>of(installmentRow));
        when(mapper.mapToInstallmentResponse(any())).thenReturn(new ContractDtos.InstallmentResponse(
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), 1, BigDecimal.valueOf(1000), LocalDate.of(2026, 7, 1),
            false, null, null, null, null, null, null, 0L));
        var request = new ContractDtos.UpsertInstallmentRequest(null, accountId, 12, BigDecimal.valueOf(1000), LocalDate.of(2026, 7, 1), false, BigDecimal.valueOf(12000), BigDecimal.ZERO);
        assertNotNull(service.createCreditCardInstallment(userId, request));
        verify(debtRepo).createDebt(any(), anyString(), any(), any(), any(), any(), anyString());
    }

    // ==================== PAY CREDIT CARD INSTALLMENT ====================

    @Test
    void payCreditCardInstallment_throwsNotFound_whenInstallmentMissing() {
        when(installmentRepo.getInstallmentById(any(), any())).thenReturn(null);
        var request = new ContractDtos.PayInstallmentRequest(UUID.randomUUID().toString(), "MXN", "notes");
        assertThrows(ApiException.class, () -> service.payCreditCardInstallment(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void payCreditCardInstallment_throwsBadRequest_whenNoAccountId() {
        Object[] row = new Object[8];
        row[0] = UUID.randomUUID();
        row[1] = UUID.randomUUID();
        row[3] = BigDecimal.valueOf(500);
        when(installmentRepo.getInstallmentById(any(), any())).thenReturn(row);
        when(mapper.unwrap(any())).thenReturn(row);
        var request = new ContractDtos.PayInstallmentRequest(UUID.randomUUID().toString(), "MXN", "notes");
        assertThrows(ApiException.class, () -> service.payCreditCardInstallment(userId, UUID.randomUUID().toString(), request));
    }

    @Test
    void payCreditCardInstallment_paysSuccessfully() {
        String installmentId = UUID.randomUUID().toString();
        String debtIdStr = UUID.randomUUID().toString();
        String accountIdStr = UUID.randomUUID().toString();
        String debitAccountIdStr = UUID.randomUUID().toString();
        Object[] row = new Object[9];
        row[0] = UUID.fromString(installmentId);
        row[1] = UUID.fromString(debtIdStr);
        row[2] = 1;
        row[3] = BigDecimal.valueOf(500);
        row[4] = LocalDate.now();
        row[8] = UUID.fromString(accountIdStr);
        Object[] unwrapped = new Object[9];
        unwrapped[0] = UUID.fromString(installmentId);
        unwrapped[1] = UUID.fromString(debtIdStr);
        unwrapped[2] = 1;
        unwrapped[3] = BigDecimal.valueOf(500);
        unwrapped[4] = LocalDate.now();
        unwrapped[8] = UUID.fromString(accountIdStr);
        when(installmentRepo.getInstallmentById(any(), any())).thenReturn(row);
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toString(any())).thenCallRealMethod();
        when(mapper.toBigDecimal(any())).thenCallRealMethod();
        Object[] debitRow = { UUID.fromString(debitAccountIdStr), UUID.fromString(userId), "debit", "Efectivo", null, BigDecimal.valueOf(10000), null, null, null, true, "MXN", null, null, null, "synced", 1L };
        when(accountRepo.getAccountById(any(), any(), any())).thenReturn(debitRow);
        when(movementRepo.createTransaction(any(), any(), isNull(), isNull(), eq("expense"), eq(BigDecimal.valueOf(500)), anyString(), anyString(), any(), anyString())).thenReturn(new Object[]{"1"});
        when(installmentRepo.markAsPaid(any(), any())).thenReturn(1);
        Object[] debtRow = { UUID.fromString(debtIdStr), UUID.fromString(userId), "Debt", BigDecimal.valueOf(5000), BigDecimal.valueOf(500), BigDecimal.valueOf(500), "monthly", LocalDate.now(), "notes", null, null, null, "synced", 1L };
        when(debtRepo.getDebtById(any(), any())).thenReturn(debtRow);
        when(debtRepo.updateDebt(any(), any(), anyString(), any(), any(), anyString(), any(), anyString())).thenReturn(new Object[]{"1"});
        when(mapper.mapToInstallmentResponse(any())).thenReturn(new ContractDtos.InstallmentResponse(installmentId, debtIdStr, 1, BigDecimal.valueOf(500), LocalDate.now(), true, null, null, null, null, null, null, 0));
        var request = new ContractDtos.PayInstallmentRequest(debitAccountIdStr, "MXN", "pago tarjeta");
        assertNotNull(service.payCreditCardInstallment(userId, installmentId, request));
        verify(movementRepo, times(2)).createTransaction(any(), any(), isNull(), isNull(), eq("expense"), eq(BigDecimal.valueOf(500)), anyString(), anyString(), any(), anyString());
    }
}
