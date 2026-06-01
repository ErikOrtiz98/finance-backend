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
        map.put("createdAt",   row[6]);
        map.put("updatedAt",   row[7]);
        map.put("deletedAt",   row[8]);
        map.put("syncStatus",  toStringText(row[9]));
        map.put("version",     row[10]);
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

    // Para listAccounts y createAccount (16 columnas - incluye currency)
    public ContractDtos.AccountResponse mapToAccountResponse(Object[] row) {
        row = unwrap(row);
        return new ContractDtos.AccountResponse(
            toString(row[0]),     // id
            toString(row[1]),     // userId
            toString(row[2]),     // type (account_type)
            toString(row[3]),     // name
            toString(row[4]),     // institution (bank_name)
            toString(row[5]),     // currency
            toBigDecimal(row[6]), // balance
            toBigDecimal(row[7]), // creditLimit
            toInteger(row[8]),    // closingDay
            toInteger(row[9]),    // dueDay
            toBoolean(row[10]),   // active
            toInstant(row[11]),   // createdAt
            toInstant(row[12]),   // updatedAt
            toInstant(row[13]),   // deletedAt
            toSyncStatus(row[14]), // syncStatus
            toLong(row[15], 1L)   // version
        );
    }

    // Para updateAccount (15 columnas - NO incluye currency)
    public ContractDtos.AccountResponse mapToAccountResponseUpdate(Object[] row, String currency) {
        row = unwrap(row);
        return new ContractDtos.AccountResponse(
            toString(row[0]),     // id
            toString(row[1]),     // userId
            toString(row[2]),     // type (account_type)
            toString(row[3]),     // name
            toString(row[4]),     // institution (bank_name)
            currency,             // currency - del request
            toBigDecimal(row[5]), // balance (1700.75)
            toBigDecimal(row[6]), // creditLimit (24000.00)
            toInteger(row[7]),    // closingDay (null)
            toInteger(row[8]),    // dueDay (null)
            toBoolean(row[9]),    // active (true)
            toInstant(row[10]),   // createdAt
            toInstant(row[11]),   // updatedAt
            toInstant(row[12]),   // deletedAt (null)
            toSyncStatus(row[13]), // syncStatus ("synced")
            toLong(row[14], 1L)   // version (2)
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

    public ContractDtos.RecurringPaymentResponse mapToRecurringResponse(Object[] row) {
    	row = unwrap(row);
        return new ContractDtos.RecurringPaymentResponse(
            toString(row[0]), toString(row[1]), toString(row[2]),
            toBigDecimal(row[3]), toString(row[4]), toString(row[5]),
            toLocalDate(row[6]), toString(row[7]),
            toInstant(row[8]), toInstant(row[9]), toInstant(row[10]),
            toSyncStatus(row[11]), toLong(row[12], 1L)
        );
    }

    // ==================== TYPE CONVERTERS PUBLICOS ====================
    // Son públicos porque el Service los usa para transformar parámetros.

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
        return obj instanceof java.sql.Date ? ((java.sql.Date) obj).toLocalDate() : null; 
    }

    public UUID toUuid(String str) { 
        return str != null ? UUID.fromString(str) : null; 
    }

    // ==================== METODOS PRIVADOS ====================

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
        return obj instanceof Integer ? (Integer) obj : null; 
    }
    
    private Boolean toBoolean(Object obj) { 
        return obj instanceof Boolean ? (Boolean) obj : null; 
    }
    
    private Long toLong(Object obj, Long defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Long l) return l;
        return Long.parseLong(obj.toString());
    }

    private Instant toInstant(Object obj) { 
        return obj instanceof OffsetDateTime ? ((OffsetDateTime) obj).toInstant() : null; 
    }

    private ContractDtos.SyncStatus toSyncStatus(Object obj) {
        return obj != null ? ContractDtos.SyncStatus.valueOf(obj.toString()) : null;
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