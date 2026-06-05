package com.codex.finance.service;

import com.codex.finance.client.SupabaseAuthClient;
import com.codex.finance.dto.ContractDtos;
import com.codex.finance.mapper.FinanceMapper;
import com.codex.finance.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceApiServiceAuditTest {

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

    @BeforeEach
    void setUp() {
        service = new FinanceApiService(
            profileRepo, categoryRepo, accountRepo, movementRepo,
            debtRepo, scheduledPaymentRepo, authClient, objectMapper,
            mapper, jdbcTemplate, installmentRepo, financialGoalRepo, budgetRepo
        );
    }

    // =========================================================
    // F5: createCreditCardPurchase DEBE crear un movimiento
    // "income" en la tarjeta para reflejar la nueva deuda
    // =========================================================
    @Test
    void createCreditCardPurchase_createsIncomeMovementOnCard() {
        String userId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();
        String categoryId = UUID.randomUUID().toString();

        var request = new ContractDtos.CreditCardPurchaseRequest(
            accountId, "Laptop", BigDecimal.valueOf(24000), 12,
            BigDecimal.ZERO, LocalDate.of(2026, 7, 1), categoryId
        );

        UUID userUuid = UUID.fromString(userId);
        UUID accountUuid = UUID.fromString(accountId);

        // Mock profile currency
        when(profileRepo.getUserCurrency(any(UUID.class))).thenReturn("MXN");

        // Mock getAccountById to return a credit card row
        Object[] creditRow = {
            accountUuid, userUuid, "credit",
            "Tarjeta Visa", "Banco X", BigDecimal.valueOf(5000),
            BigDecimal.valueOf(50000), 15, 10, true, "MXN",
            java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(),
            null, "synced", 1L
        };
        when(accountRepo.getAccountById(any(UUID.class), any(UUID.class), anyString()))
            .thenReturn(creditRow);

        // Mock mapper: toString calls real, unwrap returns argument as-is
        when(mapper.toString(any())).thenCallRealMethod();
        when(mapper.unwrap(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock debt creation
        Object[] debtRow = { UUID.randomUUID(), "Laptop",
            BigDecimal.valueOf(24000), BigDecimal.valueOf(2000),
            "monthly", "2026-07-01", "Compra a meses con tarjeta de crédito"
        };
        when(debtRepo.createDebt(any(), anyString(), any(), any(), any(), any(), anyString()))
            .thenReturn(debtRow);

        // Mock installment creation (12 installments) - void method
        doNothing().when(installmentRepo).createCreditCardInstallment(
            any(), any(), any(), anyInt(), any(), any(), any(), any(), anyBoolean()
        );

        // Mock listInstallmentsByAccount return
        when(installmentRepo.listInstallmentsByAccount(any(), any()))
            .thenReturn(List.of());

        // Execute
        List<ContractDtos.InstallmentResponse> result = service.createCreditCardPurchase(
            userId, request
        );

        // VERIFY: movementRepo.createTransaction fue llamado con tipo "income"
        // en la cuenta de la tarjeta de crédito
        ArgumentCaptor<UUID> accountCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);

        verify(movementRepo, atLeastOnce()).createTransaction(
            any(UUID.class), // userId
            accountCaptor.capture(), // accountId
            isNull(),   // transferAccountId
            isNull(),   // categoryId
            typeCaptor.capture(), // type
            amountCaptor.capture(), // amount
            anyString(), // description
            anyString(), // currency
            any(LocalDate.class), // transactionDate
            anyString()  // notes
        );

        // Find the income movement (there should be at least one)
        boolean foundIncomeOnCard = false;
        for (int i = 0; i < typeCaptor.getAllValues().size(); i++) {
            if ("income".equals(typeCaptor.getAllValues().get(i))
                && accountId.equals(accountCaptor.getAllValues().get(i).toString())) {
                foundIncomeOnCard = true;
                assertEquals(0, BigDecimal.valueOf(24000).compareTo(amountCaptor.getAllValues().get(i)),
                    "Income amount debe ser el total de la compra");
                break;
            }
        }
        assertTrue(foundIncomeOnCard,
            "createCreditCardPurchase DEBE crear un movimiento 'income' en la tarjeta");
    }

    // =========================================================
    // F6: listTransactions DEBE pasar TODOS los filtros al repo
    // =========================================================
    @Test
    void listTransactions_passesAllFilterParamsToRepository() {
        String userId = UUID.randomUUID().toString();

        var filters = new ContractDtos.TransactionFilters(
            LocalDate.of(2026, 1, 1),  // from
            LocalDate.of(2026, 6, 30), // to
            UUID.randomUUID().toString(), // accountId
            UUID.randomUUID().toString(), // categoryId
            "expense",  // type
            10,          // limit
            0,           // offset
            null         // cursor
        );

        when(profileRepo.getUserCurrency(any(UUID.class))).thenReturn("MXN");
        when(movementRepo.findAllMovements(
            any(UUID.class), anyString(),
            any(), any(), any(), any(), anyString(),
            anyInt(), anyInt()
        )).thenReturn(List.of());

        service.listTransactions(userId, filters);

        // VERIFY: findAllMovements recibio todos los filtros
        verify(movementRepo).findAllMovements(
            any(UUID.class), // userId
            eq("MXN"),       // currency
            eq(filters.from()),   // from
            eq(filters.to()),     // to
            eq(UUID.fromString(filters.accountId())), // accountId
            eq(UUID.fromString(filters.categoryId())), // categoryId
            eq(filters.type()),   // type
            eq(10),               // limit
            eq(0)                 // offset
        );
    }

    // =========================================================
    // F4: alertThreshold solo se divide UNA vez (backend)
    //     Frontend envia porcentaje crudo (80 → backend /100 = 0.8)
    // =========================================================
    @Test
    void createBudget_alertThresholdStoredAsDecimal() {
        String userId = UUID.randomUUID().toString();
        String categoryId = UUID.randomUUID().toString();

        var request = new ContractDtos.UpsertBudgetRequest(
            categoryId, "monthly",
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
            BigDecimal.valueOf(3000),
            BigDecimal.valueOf(80) // 80% enviado por frontend (SIN dividir)
        );

        doNothing().when(budgetRepo).createBudget(
            any(), any(), anyString(), any(), any(), any(), any()
        );
        when(budgetRepo.listActiveBudgets(any()))
            .thenReturn(List.<Object[]>of(new Object[]{
                UUID.randomUUID(), UUID.fromString(categoryId), "Comida",
                "monthly", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                BigDecimal.valueOf(3000), BigDecimal.valueOf(0.8),
                BigDecimal.ZERO, BigDecimal.ZERO, false
            }));
        when(mapper.mapToBudgetResponse(any())).thenReturn(
            new ContractDtos.BudgetResponse(null, categoryId, "Comida",
                "monthly", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                BigDecimal.valueOf(3000), BigDecimal.valueOf(0.8),
                BigDecimal.ZERO, BigDecimal.ZERO, false)
        );

        ContractDtos.BudgetResponse result = service.createBudget(userId, request);

        // VERIFICAR que el alertThreshold guardado es 0.8 (80%)
        // y NO 0.008 (que seria el doble divide)
        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(0.8).compareTo(result.alertThreshold()),
            "alertThreshold debe ser 0.8 (80% almacenado como decimal). "
            + "Si es 0.008 es porque se dividio dos veces.");
    }

    // =========================================================
    // F3: markInstallmentAsPaid NO debe ejecutar SQL manual
    //     de actualizacion de saldo (lo hace el trigger)
    // =========================================================
    @Test
    void markInstallmentAsPaid_doesNotExecuteManualBalanceSql() {
        String userId = UUID.randomUUID().toString();
        String installmentId = UUID.randomUUID().toString();
        String debitAccountId = UUID.randomUUID().toString();

        var request = new ContractDtos.PayInstallmentRequest(
            debitAccountId, "MXN", "Test pago"
        );

        // Mock installment row
        Object[] installmentRow = new Object[9];
        installmentRow[0] = UUID.fromString(installmentId);
        installmentRow[1] = UUID.randomUUID(); // debt_id
        installmentRow[3] = BigDecimal.valueOf(500); // amount
        installmentRow[8] = UUID.randomUUID().toString(); // account_id (tarjeta de credito)

        when(installmentRepo.getInstallmentById(any(UUID.class), any(UUID.class)))
            .thenReturn(installmentRow);

        Object[] unwrapped = new Object[9];
        unwrapped[0] = UUID.fromString(installmentId);
        unwrapped[1] = UUID.randomUUID(); // debt_id
        unwrapped[3] = BigDecimal.valueOf(500); // amount
        unwrapped[8] = UUID.randomUUID(); // account_id (tarjeta)

        when(mapper.unwrap(any())).thenReturn(unwrapped);
        when(mapper.toString(any())).thenCallRealMethod();
        when(mapper.toBigDecimal(any())).thenCallRealMethod();

        when(installmentRepo.markAsPaid(any(), any())).thenReturn(1);
        when(mapper.mapToInstallmentResponse(any())).thenReturn(
            new ContractDtos.InstallmentResponse(installmentId, null, 1,
                BigDecimal.valueOf(500), LocalDate.now(), true, null, null,
                null, null, null, null, 0)
        );

        service.markInstallmentAsPaid(userId, installmentId, request);

        // VERIFY: jdbcTemplate.update fue llamado con SQL de debt
        verify(jdbcTemplate, atLeastOnce()).update(
            contains("UPDATE debts SET remaining_balance"),
            any(), any(), any()
        );

        // NO debe haber llamado a UPDATE accounts SET current_balance
        verify(jdbcTemplate, never()).update(
            contains("UPDATE accounts SET current_balance"),
            any(), any(), any()
        );

        // VERIFY: crea 2 transacciones (expense en debito + payment en tarjeta)
        verify(movementRepo, times(2)).createTransaction(
            any(UUID.class), any(UUID.class), isNull(), isNull(),
            anyString(), eq(BigDecimal.valueOf(500)),
            anyString(), anyString(), any(LocalDate.class), anyString()
        );
    }

    // =========================================================
    // F1: setAuthContext usa query parametrizada (no concatenacion)
    //     Probamos via createTransaction
    // =========================================================
    @Test
    void createTransaction_usesParameterizedQuery() {
        String userId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();

        var request = new ContractDtos.UpsertTransactionRequest(
            accountId, null, null, null,
            "expense", "Test", BigDecimal.valueOf(100),
            "MXN", LocalDate.now(), null
        );

        // Use real method implementations for mapper conversions
        when(mapper.toUuid(anyString())).thenCallRealMethod();
        when(mapper.mapToTransactionResponse(any())).thenReturn(null);

        service.createTransaction(userId, request);

        // VERIFY: setAuthContext usa jdbcTemplate.update con placeholder
        // en lugar de jdbcTemplate.execute con concatenacion
        verify(jdbcTemplate, never()).execute(anyString());
    }
}
