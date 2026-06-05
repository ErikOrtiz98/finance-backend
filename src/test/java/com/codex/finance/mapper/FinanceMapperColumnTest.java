package com.codex.finance.mapper;

import com.codex.finance.dto.ContractDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FinanceMapperColumnTest {

    private FinanceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FinanceMapper(new ObjectMapper());
    }

    @Test
    void mapToAccountResponse_matches_listAccountsQuery() {
        // listAccounts SQL: id(0), userId(1), type(2), name(3), institution(4),
        // currency(5), balance(6), creditLimit(7), closingDay(8), dueDay(9),
        // active(10), createdAt(11), updatedAt(12), deletedAt(13), syncStatus(14), version(15)
        Object[] row = {
            UUID.fromString("a0000000-0000-0000-0000-000000000001"), // 0 id
            UUID.fromString("a0000000-0000-0000-0000-000000000002"), // 1 userId
            "debit",        // 2 type
            "Mi Cuenta",    // 3 name
            "Banco X",      // 4 institution
            "MXN",          // 5 currency
            BigDecimal.valueOf(5000.00), // 6 balance
            null,           // 7 creditLimit (mapper returns BigDecimal.ZERO for null)
            15,             // 8 closingDay
            10,             // 9 dueDay
            true,           // 10 active
            OffsetDateTime.now(), // 11 createdAt
            OffsetDateTime.now(), // 12 updatedAt
            null,           // 13 deletedAt
            "synced",       // 14 syncStatus
            1L              // 15 version
        };

        ContractDtos.AccountResponse resp = mapper.mapToAccountResponse(row);

        assertNotNull(resp);
        assertEquals("a0000000-0000-0000-0000-000000000001", resp.id());
        assertEquals("debit", resp.type());
        assertEquals("Mi Cuenta", resp.name());
        assertEquals("Banco X", resp.institution());
        assertEquals("MXN", resp.currency());
        assertEquals(0, BigDecimal.valueOf(5000.00).compareTo(resp.balance()));
        assertEquals(BigDecimal.ZERO, resp.creditLimit()); // mapper convierte null en ZERO
        assertEquals(Integer.valueOf(15), resp.closingDay());
        assertEquals(Integer.valueOf(10), resp.dueDay());
        assertTrue(resp.active());
        assertEquals("synced", resp.syncStatus().name());
        assertEquals(Long.valueOf(1), resp.version());
    }

    @Test
    void mapToTransactionResponse_matches_findAllMovementsQuery() {
        // findAllMovements SQL: id(0), userId(1), accountId(2), transferAccountId(3),
        // categoryId(4), type(5), description(6), amount(7), currency(8),
        // transactionDate(9), notes(10), createdAt(11), updatedAt(12), deletedAt(13),
        // syncStatus(14), version(15)
        String txId = UUID.randomUUID().toString();
        Object[] row = {
            UUID.fromString(txId), // 0 id
            UUID.randomUUID(), // 1 userId
            UUID.randomUUID(), // 2 accountId
            null,           // 3 transferAccountId
            UUID.randomUUID(), // 4 categoryId
            "expense",      // 5 type
            "Test tx",      // 6 description
            BigDecimal.valueOf(100), // 7 amount
            "MXN",          // 8 currency
            LocalDate.of(2026, 6, 5), // 9 transactionDate
            "nota",         // 10 notes
            OffsetDateTime.now(), // 11 createdAt
            OffsetDateTime.now(), // 12 updatedAt
            null,           // 13 deletedAt
            "synced",       // 14 syncStatus
            1L              // 15 version
        };

        ContractDtos.TransactionResponse resp = mapper.mapToTransactionResponse(row);

        assertNotNull(resp);
        assertEquals(txId, resp.id());
        assertEquals("expense", resp.type());
        assertEquals("Test tx", resp.description());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(resp.amount()));
        assertEquals("MXN", resp.currency());
        assertEquals(LocalDate.of(2026, 6, 5), resp.transactionDate());
        assertEquals("synced", resp.syncStatus().name());
    }

    @Test
    void mapToDebtResponse_matches_listDebtsQuery() {
        // listDebts SQL: id(0), userId(1), name(2), principalBalance(3),
        // remainingBalance(4), installment(5), frequency(6), nextDueDate(7),
        // notes(8), createdAt(9), updatedAt(10), deletedAt(11), syncStatus(12), version(13)
        Object[] row = {
            UUID.fromString("d0000000-0000-0000-0000-000000000001"), // 0 id
            UUID.fromString("d0000000-0000-0000-0000-000000000002"), // 1 userId
            "Deuda test",   // 2 name
            BigDecimal.valueOf(1000), // 3 principalBalance
            BigDecimal.valueOf(500), // 4 remainingBalance
            BigDecimal.valueOf(100), // 5 installment
            "monthly",      // 6 frequency
            LocalDate.of(2026, 7, 5), // 7 nextDueDate
            "nota",         // 8 notes
            OffsetDateTime.now(), // 9 createdAt
            OffsetDateTime.now(), // 10 updatedAt
            null,           // 11 deletedAt
            "synced",       // 12 syncStatus
            1L              // 13 version
        };

        ContractDtos.DebtResponse resp = mapper.mapToDebtResponse(row);

        assertNotNull(resp);
        assertEquals("Deuda test", resp.name());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(resp.principalBalance()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(resp.remainingBalance()));
        assertEquals("synced", resp.syncStatus().name());
    }

    @Test
    void mapToBudgetResponse_matches_listActiveBudgetsQuery() {
        // listActiveBudgets SQL: id(0), category_id(1), category_name(2), period(3),
        // periodStart(4), periodEnd(5), amountLimit(6), alertThreshold(7),
        // spentAmount(8), usagePercentage(9), isAlert(10)
        Object[] row = {
            UUID.fromString("b0000000-0000-0000-0000-000000000001"), // 0 id
            UUID.fromString("b0000000-0000-0000-0000-000000000002"), // 1 categoryId
            "Comida",       // 2 categoryName
            "monthly",      // 3 period
            LocalDate.of(2026, 6, 1), // 4 periodStart
            LocalDate.of(2026, 6, 30), // 5 periodEnd
            BigDecimal.valueOf(3000), // 6 amountLimit
            BigDecimal.valueOf(0.8),  // 7 alertThreshold (80%)
            BigDecimal.valueOf(1500), // 8 spentAmount
            BigDecimal.valueOf(50.00), // 9 usagePercentage
            false           // 10 isAlert
        };

        ContractDtos.BudgetResponse resp = mapper.mapToBudgetResponse(row);

        assertNotNull(resp);
        assertEquals("Comida", resp.categoryName());
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(resp.amountLimit()));
        assertEquals(0, BigDecimal.valueOf(0.8).compareTo(resp.alertThreshold()));
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(resp.spentAmount()));
        assertFalse(resp.isAlert());
    }

    @Test
    void mapToAccountResponse_withDeletedAccount_returnsDeletedSyncStatus() {
        Object[] row = new Object[16];
        row[0] = UUID.randomUUID();
        row[1] = UUID.randomUUID();
        row[2] = "debit";
        row[3] = "Cuenta eliminada";
        row[4] = "";
        row[5] = "MXN";
        row[6] = BigDecimal.ZERO;
        row[7] = null;
        row[8] = null;
        row[9] = null;
        row[10] = false;
        row[11] = OffsetDateTime.now();
        row[12] = OffsetDateTime.now();
        row[13] = OffsetDateTime.now(); // deletedAt presente
        row[14] = "deleted";
        row[15] = 1L;

        ContractDtos.AccountResponse resp = mapper.mapToAccountResponse(row);
        assertEquals("deleted", resp.syncStatus().name());
    }

    @Test
    void mapToFinancialGoalResponse() {
        Object[] row = {
            UUID.randomUUID(), // 0 id
            "Meta test",       // 1 name
            BigDecimal.valueOf(10000), // 2 targetAmount
            BigDecimal.valueOf(2500),  // 3 currentProgress
            LocalDate.of(2026, 12, 31), // 4 targetDate
            "active",          // 5 status
            OffsetDateTime.now(), // 6 createdAt
            OffsetDateTime.now(), // 7 updatedAt
            BigDecimal.valueOf(25.00) // 8 progressPercentage
        };

        ContractDtos.FinancialGoalResponse resp = mapper.mapToFinancialGoalResponse(row);
        assertNotNull(resp);
        assertEquals("Meta test", resp.name());
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(resp.targetAmount()));
        assertEquals(0, BigDecimal.valueOf(2500).compareTo(resp.currentProgress()));
        assertEquals("active", resp.status());
    }
}
