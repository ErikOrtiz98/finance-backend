package com.codex.finance.mapper;

import com.codex.finance.dto.ContractDtos;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FinanceMapper {

    private final ObjectMapper objectMapper;

    public FinanceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==================== MAPPERS PRINCIPALES ====================

    public ContractDtos.MeResponse mapToMeResponse(Object[] row) {
        row = unwrap(row);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId",      toStringText(row[0]));
        map.put("id",          toStringText(row[1]));
        map.put("displayName", toStringText(row[2]));
        map.put("currency",    toStringText(row[3]));
        map.put("payCycle",    toStringText(row[4]));
        map.put("payDays",     normalizePayDays(row[5]));
        map.put("monthlyIncome", toBigDecimal(row[6]));
        map.put("createdAt",   row[7]);
        map.put("updatedAt",   row[8]);
        map.put("deletedAt",   row[9]);
        map.put("syncStatus",  toStringText(row[10]));
        map.put("version",     row[11]);
        return objectMapper.convertValue(map, ContractDtos.MeResponse.class);
    }

    public ContractDtos.CategoryResponse mapToCategoryResponse(Object[] row) {
        row = unwrap(row);
        return new ContractDtos.CategoryResponse(
            toString(row[0]), toString(row[1]), toString(row[2]), toString(row[3]),
            toString(row[4]), toString(row[5]),
            toInstant(row[6]), toInstant(row[7]), toInstant(row[8]),
            toSyncStatus(row[9]), toLong(row[10], 1L)
        );
    }

    public ContractDtos.AccountResponse mapToAccountResponse(Object[] row) {
        row = unwrap(row);
        return new ContractDtos.AccountResponse(
            toString(row[0]), toString(row[1]), toString(row[2]), toString(row[3]),
            toString(row[4]), toString(row[5]), toBigDecimal(row[6]), toBigDecimal(row[7]),
            toInteger(row[8]), toInteger(row[9]), toBoolean(row[10]),
            toInstant(row[11]), toInstant(row[12]), toInstant(row[13]),
            toSyncStatus(row[14]), toLong(row[15], 1L)
        );
    }

    public ContractDtos.AccountResponse mapToAccountResponseUpdate(Object[] row, String currency) {
        row = unwrap(row);
        return new ContractDtos.AccountResponse(
            toString(row[0]), toString(row[1]), toString(row[2]), toString(row[3]),
            toString(row[4]), currency, toBigDecimal(row[5]), toBigDecimal(row[6]),
            toInteger(row[7]), toInteger(row[8]), toBoolean(row[9]),
            toInstant(row[10]), toInstant(row[11]), toInstant(row[12]),
            toSyncStatus(row[13]), toLong(row[14], 1L)
        );
    }

    public ContractDtos.TransactionResponse mapToTransactionResponse(Object[] row) {
        row = unwrap(row);
        return new ContractDtos.TransactionResponse(
            toString(row[0]), toString(row[1]), toString(row[2]), toString(row[3]),
            toString(row[4]), toString(row[5]), toString(row[6]), toBigDecimal(row[7]),
            toString(row[8]), toLocalDate(row[9]), toString(row[10]),
            toInstant(row[11]), toInstant(row[12]), toInstant(row[13]),
            toSyncStatus(row[14]), toLong(row[15], 1L)
        );
    }

    public ContractDtos.DebtResponse mapToDebtResponse(Object[] row) {
        row = unwrap(row);
        return new ContractDtos.DebtResponse(
            toString(row[0]), toString(row[1]), toString(row[2]),
            toBigDecimal(row[3]), toBigDecimal(row[4]), toString(row[5]),
            toLocalDate(row[6]), toString(row[7]),
            toInstant(row[8]), toInstant(row[9]), toInstant(row[10]),
            toSyncStatus(row[11]), toLong(row[12], 1L)
        );
    }

    // ==================== RECURRING PAYMENT ====================
    // Usando el constructor EXISTENTE (13 parámetros)
    public ContractDtos.RecurringPaymentResponse mapToRecurringResponse(Object[] row) {
        row = unwrap(row);
        return new ContractDtos.RecurringPaymentResponse(
            toString(row[0]),   // id
            toString(row[1]),   // userId
            toString(row[2]),   // name
            toBigDecimal(row[3]), // amount
            toString(row[4]),   // currency
            toString(row[5]),   // frequency
            toLocalDate(row[6]), // nextDueDate
            toString(row[7]),   // categoryId
            toInstant(row[8]),  // createdAt
            toInstant(row[9]),  // updatedAt
            toInstant(row[10]), // deletedAt
            toSyncStatus(row[11]), // syncStatus
            toLong(row[12], 1L) // version
        );
    }

    // ==================== INSTALLMENT ====================
    // Usando el constructor EXISTENTE (13 parámetros)
    public ContractDtos.InstallmentResponse mapToInstallmentResponse(Object[] row) {
        row = unwrap(row);
        return new ContractDtos.InstallmentResponse(
            toString(row[0]),      // id
            toString(row[1]),      // debtId
            toInteger(row[2]),     // number
            toBigDecimal(row[3]),  // amount
            toLocalDate(row[4]),   // dueDate
            toBoolean(row[5]),     // paid
            toInstant(row[6]),     // paidAt
            toString(row[7]),      // paymentMovementId
            toInstant(row[8]),     // createdAt
            toInstant(row[9]),     // updatedAt
            toInstant(row[10]),    // deletedAt
            toSyncStatus(row[11]), // syncStatus
            toLong(row[12], 1L)    // version
        );
    }

    public ContractDtos.FinancialGoalResponse mapToFinancialGoalResponse(Object[] row) {
        row = unwrap(row);
        return new ContractDtos.FinancialGoalResponse(
            toString(row[0]),      // id
            toString(row[1]),      // name
            toBigDecimal(row[2]),  // targetAmount
            toBigDecimal(row[3]),  // currentProgress
            toLocalDate(row[4]),   // targetDate
            toString(row[5]),      // status
            toInstant(row[6]),     // createdAt
            toInstant(row[7]),     // updatedAt
            toBigDecimal(row[8])   // progressPercentage
        );
    }

    public ContractDtos.BudgetResponse mapToBudgetResponse(Object[] row) {
        row = unwrap(row);
        return new ContractDtos.BudgetResponse(
            toString(row[0]),      // id
            toString(row[1]),      // categoryId
            toString(row[2]),      // categoryName
            toString(row[3]),      // period
            toLocalDate(row[4]),   // periodStart
            toLocalDate(row[5]),   // periodEnd
            toBigDecimal(row[6]),  // amountLimit
            toBigDecimal(row[7]),  // alertThreshold
            toBigDecimal(row[8]),  // spentAmount
            toBigDecimal(row[9]),  // usagePercentage
            Boolean.TRUE.equals(row[10]) // isAlert
        );
    }

    // ==================== TYPE CONVERTERS ====================

    public String toString(Object obj) { 
        return obj != null ? obj.toString() : null; 
    }
    
    public BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number num) return new BigDecimal(num.toString());
        if (obj instanceof String str) {
            try {
                return new BigDecimal(str.trim());
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    public LocalDate toLocalDate(Object obj) { 
        if (obj == null) return null;
        if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate();
        if (obj instanceof LocalDate) return (LocalDate) obj;
        try {
            return LocalDate.parse(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public Instant toInstant(Object obj) { 
        if (obj == null) return null;
        if (obj instanceof OffsetDateTime) return ((OffsetDateTime) obj).toInstant();
        if (obj instanceof Instant) return (Instant) obj;
        if (obj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) obj).toInstant();
        return null;
    }

    public UUID toUuid(String str) { 
        return str != null ? UUID.fromString(str) : null; 
    }

    public Object[] unwrap(Object[] row) {
        if (row != null && row.length == 1 && row[0] instanceof Object[]) {
            return (Object[]) row[0];
        }
        return row;
    }

    private String toStringText(Object val) {
        if (val == null) return null;
        return val.toString();
    }

    private Integer toInteger(Object obj) { 
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Boolean toBoolean(Object obj) { 
        if (obj == null) return null;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof String) return Boolean.parseBoolean((String) obj);
        return null;
    }
    
    private Long toLong(Object obj, Long defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Long) return (Long) obj;
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private ContractDtos.SyncStatus toSyncStatus(Object obj) {
        if (obj == null) return null;
        try {
            return ContractDtos.SyncStatus.valueOf(obj.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<Integer> normalizePayDays(Object value) {
        if (value == null) return List.of();
        try {
            if (value instanceof List<?> list) 
                return list.stream().map(o -> Integer.parseInt(o.toString())).collect(Collectors.toList());
            return objectMapper.readValue(value.toString(), new TypeReference<List<Integer>>() {});
        } catch (Exception e) { 
            return List.of(); 
        }
    }
}