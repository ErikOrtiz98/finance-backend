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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceApiServiceAdvancedTest {

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

    // ==================== SUMMARY ====================

    @Test
    void summary_usesRealIncome_whenAvailable() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(
            new Object[]{ BigDecimal.valueOf(5000), BigDecimal.valueOf(2000), BigDecimal.valueOf(500), BigDecimal.valueOf(300) }
        );
        when(debtRepo.getTotalRemainingBalance(any())).thenReturn(BigDecimal.valueOf(10000));
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toBigDecimal(any())).thenCallRealMethod();

        ContractDtos.SummaryResponse result = service.summary(userId, "monthly", null, null, null);
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(result.income()), "Should use real income");
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(result.totalRemainingDebt()));
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(result.expenses()));
        assertEquals(0, BigDecimal.valueOf(4700).compareTo(result.availableBalance()));
    }

    @Test
    void summary_returnsZeroIncome_whenNoMovements() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(null);
        when(debtRepo.getTotalRemainingBalance(any())).thenReturn(BigDecimal.ZERO);

        ContractDtos.SummaryResponse result = service.summary(userId, "monthly", null, null, null);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.income()), "Should be zero income when no movements");
        assertNotNull(result.currency());
    }

    @Test
    void summary_biweekly_returnsZero_whenNoMovements() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(null);
        when(debtRepo.getTotalRemainingBalance(any())).thenReturn(BigDecimal.ZERO);

        ContractDtos.SummaryResponse result = service.summary(userId, "biweekly", null, null, null);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.income()), "Biweekly income should be zero when no movements");
    }

    @Test
    void summary_usesZero_whenNoIncome() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(
            new Object[]{ BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO }
        );
        when(debtRepo.getTotalRemainingBalance(any())).thenReturn(BigDecimal.ZERO);
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toBigDecimal(any())).thenCallRealMethod();

        ContractDtos.SummaryResponse result = service.summary(userId, "monthly", null, null, null);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.income()));
    }

    @Test
    void summary_usesCustomDates_whenProvided() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        when(movementRepo.getSummaryByDateRange(any(), eq(from), eq(to))).thenReturn(
            new Object[]{ BigDecimal.valueOf(3000), BigDecimal.valueOf(1500), BigDecimal.ZERO, BigDecimal.ZERO }
        );
        when(debtRepo.getTotalRemainingBalance(any())).thenReturn(BigDecimal.ZERO);
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toBigDecimal(any())).thenCallRealMethod();

        ContractDtos.SummaryResponse result = service.summary(userId, "custom", from, to, null);
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(result.income()));
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(result.expenses()));
    }

    @Test
    void summary_withProfileNullRow_setsZeroIncome() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(null);
        when(debtRepo.getTotalRemainingBalance(any())).thenReturn(BigDecimal.ZERO);

        ContractDtos.SummaryResponse result = service.summary(userId, "monthly", null, null, null);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.income()));
    }

    // ==================== CATEGORY STATS ====================

    @Test
    void categoryStats_returnsPercentages() {
        when(movementRepo.getCategoryStatsByDateRange(any(), any(), any())).thenReturn(List.<Object[]>of(
            new Object[]{"cat1", "Food", BigDecimal.valueOf(800)},
            new Object[]{"cat2", "Transport", BigDecimal.valueOf(200)}
        ));
        when(mapper.toString(any())).thenCallRealMethod();
        when(mapper.toBigDecimal(any())).thenCallRealMethod();

        List<ContractDtos.CategoryStatResponse> result = service.categoryStats(userId, "monthly", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null);
        assertEquals(2, result.size());
        assertEquals(0, BigDecimal.valueOf(80).compareTo(result.get(0).percentage()), "Food should be 80%");
        assertEquals(0, BigDecimal.valueOf(20).compareTo(result.get(1).percentage()), "Transport should be 20%");
    }

    @Test
    void categoryStats_allTime_whenNoRange() {
        when(movementRepo.getCategoryStatsAll(any())).thenReturn(List.<Object[]>of(
            new Object[]{"cat1", "Food", BigDecimal.valueOf(1000)}
        ));
        when(mapper.toString(any())).thenCallRealMethod();
        when(mapper.toBigDecimal(any())).thenCallRealMethod();
        // Force resolveWindow to return {null, date} by passing only 'to'
        // This triggers the else branch: getCategoryStatsAll
        List<ContractDtos.CategoryStatResponse> result = service.categoryStats(userId, null, null, LocalDate.now(), null);
        assertEquals(1, result.size());
        verify(movementRepo).getCategoryStatsAll(any());
    }

    @Test
    void categoryStats_zeroTotal_returnsZeroPercentages() {
        when(movementRepo.getCategoryStatsByDateRange(any(), any(), any())).thenReturn(List.<Object[]>of(
            new Object[]{"cat1", "Food", BigDecimal.ZERO}
        ));
        when(mapper.toString(any())).thenCallRealMethod();
        when(mapper.toBigDecimal(any())).thenCallRealMethod();

        List<ContractDtos.CategoryStatResponse> result = service.categoryStats(userId, "monthly", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).percentage()));
    }

    // ==================== DEBT RATIO ====================

    @Test
    void getDebtRatio_lowRisk_whenRatioBelow30() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(
            new Object[]{ BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(2000), BigDecimal.ZERO }
        );
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toBigDecimal(any())).thenCallRealMethod();
        ContractDtos.DebtRatioResponse result = service.getDebtRatio(userId);
        assertEquals("bajo", result.riskLevel());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(result.debtToIncomeRatio()));
    }

    @Test
    void getDebtRatio_mediumRisk_whenRatioBetween30And50() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(
            new Object[]{ BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(4000), BigDecimal.ZERO }
        );
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toBigDecimal(any())).thenCallRealMethod();
        ContractDtos.DebtRatioResponse result = service.getDebtRatio(userId);
        assertEquals("medio", result.riskLevel());
    }

    @Test
    void getDebtRatio_highRisk_whenRatioBetween50And70() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(
            new Object[]{ BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(6000), BigDecimal.ZERO }
        );
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toBigDecimal(any())).thenCallRealMethod();
        ContractDtos.DebtRatioResponse result = service.getDebtRatio(userId);
        assertEquals("alto", result.riskLevel());
    }

    @Test
    void getDebtRatio_criticalRisk_whenRatioAbove70() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(
            new Object[]{ BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(8000), BigDecimal.ZERO }
        );
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toBigDecimal(any())).thenCallRealMethod();
        ContractDtos.DebtRatioResponse result = service.getDebtRatio(userId);
        assertEquals("crítico", result.riskLevel());
    }

    @Test
    void getDebtRatio_100percent_whenNoIncome() {
        when(profileRepo.getUserCurrency(any())).thenReturn("MXN");
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(null);

        ContractDtos.DebtRatioResponse result = service.getDebtRatio(userId);
        assertEquals(0, BigDecimal.valueOf(100).compareTo(result.debtToIncomeRatio()));
        assertEquals("crítico", result.riskLevel());
    }

    // ==================== BIWEEKLY SCHEDULE ====================

    @Test
    void getBiweeklySchedule_returnsTwoPeriods() {
        Object[] incomeRow = new Object[]{ BigDecimal.valueOf(60000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(incomeRow);
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toBigDecimal(any())).thenCallRealMethod();
        when(scheduledPaymentRepo.getUpcomingRecurringPaymentsInRange(any(), any(), any())).thenReturn(new ArrayList<>());
        when(debtRepo.getUpcomingDebtsInRange(any(), any(), any())).thenReturn(new ArrayList<>());

        List<ContractDtos.BiweeklyScheduleResponse> result = service.getBiweeklySchedule(userId);
        assertEquals(2, result.size());
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(result.get(0).availableIncome()), "Each half gets 30000");
        assertEquals("Primera quincena", result.get(0).periodName());
        assertEquals("Segunda quincena", result.get(1).periodName());
    }

    @Test
    void getBiweeklySchedule_returnsZeroIncome_whenNoMovements() {
        when(movementRepo.getSummaryByDateRange(any(), any(), any())).thenReturn(null);
        when(scheduledPaymentRepo.getUpcomingRecurringPaymentsInRange(any(), any(), any())).thenReturn(new ArrayList<>());
        when(debtRepo.getUpcomingDebtsInRange(any(), any(), any())).thenReturn(new ArrayList<>());

        List<ContractDtos.BiweeklyScheduleResponse> result = service.getBiweeklySchedule(userId);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).availableIncome()));
    }

    // ==================== UPCOMING ====================

    @Test
    void upcoming_aggregatesAllTypes() {
        when(scheduledPaymentRepo.getUpcomingRecurringPaymentsInRange(any(), any(), any())).thenReturn(List.<Object[]>of(
            new Object[]{ UUID.randomUUID(), "Netflix", BigDecimal.valueOf(200), LocalDate.now(), "monthly" }
        ));
        when(debtRepo.getUpcomingDebtsInRange(any(), any(), any())).thenReturn(List.<Object[]>of(
            new Object[]{ UUID.randomUUID(), "Car Loan", BigDecimal.valueOf(5000), LocalDate.now(), BigDecimal.valueOf(500) }
        ));
        when(installmentRepo.getUpcomingInstallments(any(), any(), any())).thenReturn(List.<Object[]>of(
            new Object[]{ UUID.randomUUID(), "Installment 1", LocalDate.now(), BigDecimal.valueOf(300) }
        ));
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toString(any())).thenCallRealMethod();
        when(mapper.toBigDecimal(any())).thenCallRealMethod();
        when(mapper.toLocalDate(any())).thenCallRealMethod();

        ContractDtos.UpcomingResponse result = service.upcoming(userId);
        assertEquals(3, result.next7Days().size());
    }

    @Test
    void upcoming_handlesNullLists() {
        lenient().when(scheduledPaymentRepo.getUpcomingRecurringPaymentsInRange(any(), any(), any())).thenReturn(null);
        lenient().when(debtRepo.getUpcomingDebtsInRange(any(), any(), any())).thenReturn(null);
        lenient().when(installmentRepo.getUpcomingInstallments(any(), any(), any())).thenReturn(null);

        ContractDtos.UpcomingResponse result = service.upcoming(userId);
        assertTrue(result.next7Days().isEmpty());
    }

    @Test
    void upcoming_handlesExceptionGracefully() {
        when(scheduledPaymentRepo.getUpcomingRecurringPaymentsInRange(any(), any(), any())).thenThrow(new RuntimeException("DB error"));
        when(debtRepo.getUpcomingDebtsInRange(any(), any(), any())).thenReturn(new ArrayList<>());
        when(installmentRepo.getUpcomingInstallments(any(), any(), any())).thenReturn(new ArrayList<>());

        ContractDtos.UpcomingResponse result = service.upcoming(userId);
        assertNotNull(result);
    }

    @Test
    void upcoming_skipsNullDueDates() {
        Object[] row = { UUID.randomUUID(), "Test", BigDecimal.valueOf(100), null, "monthly" };
        when(scheduledPaymentRepo.getUpcomingRecurringPaymentsInRange(any(), any(), any())).thenReturn(List.<Object[]>of(row));
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(mapper.toString(any())).thenCallRealMethod();
        lenient().when(mapper.toBigDecimal(any())).thenCallRealMethod();
        when(mapper.toLocalDate(any())).thenReturn(null);

        ContractDtos.UpcomingResponse result = service.upcoming(userId);
        assertTrue(result.next7Days().isEmpty());
    }

    @Test
    void upcoming_skipsDebtsWithZeroAmount() {
        Object[] row = { UUID.randomUUID(), "Paid Debt", LocalDate.now(), BigDecimal.ZERO };
        when(scheduledPaymentRepo.getUpcomingRecurringPaymentsInRange(any(), any(), any())).thenReturn(new ArrayList<>());
        when(debtRepo.getUpcomingDebtsInRange(any(), any(), any())).thenReturn(List.<Object[]>of(row));
        when(mapper.unwrap(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(mapper.toString(any())).thenCallRealMethod();
        lenient().when(mapper.toBigDecimal(any())).thenCallRealMethod();
        when(mapper.toLocalDate(any())).thenCallRealMethod();

        ContractDtos.UpcomingResponse result = service.upcoming(userId);
        assertTrue(result.next7Days().isEmpty());
    }

    // ==================== PAYMENT SUMMARY ====================

    @Test
    void getPaymentSummary_sumsPayments() {
        when(movementRepo.getCreditCardPayments(any(), any(), any())).thenReturn(BigDecimal.valueOf(1500));
        when(movementRepo.getDebtPayments(any(), any(), any())).thenReturn(BigDecimal.valueOf(800));

        var result = service.getPaymentSummary(userId, "monthly");
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(result.get("creditCardPayments")));
        assertEquals(0, BigDecimal.valueOf(800).compareTo(result.get("debtPayments")));
        assertEquals(0, BigDecimal.valueOf(2300).compareTo(result.get("totalPayments")));
    }
}
