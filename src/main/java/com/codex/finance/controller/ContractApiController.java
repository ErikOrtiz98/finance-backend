package com.codex.finance.controller;

import com.codex.finance.dto.ContractDtos.AuthResponse;
import com.codex.finance.dto.ContractDtos.BackupExportResponse;
import com.codex.finance.dto.ContractDtos.AccountResponse;
import com.codex.finance.dto.ContractDtos.BackupImportRequest;
import com.codex.finance.dto.ContractDtos.CategoryResponse;
import com.codex.finance.dto.ContractDtos.CategoryStatResponse;
import com.codex.finance.dto.ContractDtos.DebtResponse;
import com.codex.finance.dto.ContractDtos.MeResponse;
import com.codex.finance.dto.ContractDtos.MigrationImportRequest;
import com.codex.finance.dto.ContractDtos.MigrationImportResponse;
import com.codex.finance.dto.ContractDtos.RecurringPaymentResponse;
import com.codex.finance.dto.ContractDtos.RefreshRequest;
import com.codex.finance.dto.ContractDtos.SessionResponse;
import com.codex.finance.dto.ContractDtos.SignInRequest;
import com.codex.finance.dto.ContractDtos.SignOutRequest;
import com.codex.finance.dto.ContractDtos.SignUpRequest;
import com.codex.finance.dto.ContractDtos.SummaryResponse;
import com.codex.finance.dto.ContractDtos.SyncConflictResolutionRequest;
import com.codex.finance.dto.ContractDtos.SyncPullResponse;
import com.codex.finance.dto.ContractDtos.SyncPushRequest;
import com.codex.finance.dto.ContractDtos.SyncPushResponse;
import com.codex.finance.dto.ContractDtos.TransactionFilters;
import com.codex.finance.dto.ContractDtos.TransactionResponse;
import com.codex.finance.dto.ContractDtos.UpcomingResponse;
import com.codex.finance.dto.ContractDtos.UpdateMeRequest;
import com.codex.finance.dto.ContractDtos.UpsertAccountRequest;
import com.codex.finance.dto.ContractDtos.UpsertCategoryRequest;
import com.codex.finance.dto.ContractDtos.UpsertDebtRequest;
import com.codex.finance.dto.ContractDtos.UpsertRecurringPaymentRequest;
import com.codex.finance.dto.ContractDtos.UpsertTransactionRequest;
import com.codex.finance.service.FinanceApiService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class ContractApiController {
    private final FinanceApiService service;

    public ContractApiController(FinanceApiService service) {
        this.service = service;
    }

    @PostMapping("/auth/sign-up")
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return service.signUp(request);
    }

    @PostMapping("/auth/sign-in")
    public AuthResponse signIn(@Valid @RequestBody SignInRequest request) {
        return service.signIn(request);
    }

    @PostMapping("/auth/refresh")
    public SessionResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return service.refresh(request);
    }

    @PostMapping("/auth/sign-out")
    public Map<String, Object> signOut(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @Valid @RequestBody SignOutRequest request) {
        String accessToken = authorization == null ? null : authorization.replaceFirst("(?i)^Bearer\\s+", "");
        service.signOut(accessToken, request);
        return Map.of("success", true);
    }

    @GetMapping("/auth/session")
    public SessionResponse session(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        String accessToken = authorization == null ? null : authorization.replace("Bearer ", "");
        return service.session(accessToken, refreshToken);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.getMe(jwt.getSubject());
    }

    @PatchMapping("/me")
    public MeResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateMeRequest request) {
        return service.updateMe(jwt.getSubject(), request);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories(@AuthenticationPrincipal Jwt jwt) {
        return service.listCategories(jwt.getSubject());
    }

    @PostMapping("/categories")
    public CategoryResponse createCategory(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertCategoryRequest request) {
        return service.createCategory(jwt.getSubject(), request);
    }

    @PatchMapping("/categories/{id}")
    public CategoryResponse updateCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @Valid @RequestBody UpsertCategoryRequest request) {
        return service.updateCategory(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/categories/{id}")
    public Map<String, Object> deleteCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        service.deleteCategory(jwt.getSubject(), id);
        return Map.of("success", true);
    }

    @GetMapping("/accounts")
    public List<AccountResponse> listAccounts(@AuthenticationPrincipal Jwt jwt) {
        return service.listAccounts(jwt.getSubject());
    }

    @PostMapping("/accounts")
    public AccountResponse createAccount(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertAccountRequest request) {
        return service.createAccount(jwt.getSubject(), request);
    }

    @PatchMapping("/accounts/{id}")
    public AccountResponse updateAccount(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @Valid @RequestBody UpsertAccountRequest request) {
        return service.updateAccount(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/accounts/{id}")
    public Map<String, Object> deleteAccount(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        service.deleteAccount(jwt.getSubject(), id);
        return Map.of("success", true);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> listTransactions(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                      @RequestParam(required = false) String accountId,
                                                      @RequestParam(required = false) String categoryId,
                                                      @RequestParam(required = false) String type,
                                                      @RequestParam(required = false) Integer limit,
                                                      @RequestParam(required = false) String cursor) {
        return service.listTransactions(jwt.getSubject(), new TransactionFilters(from, to, accountId, categoryId, type, limit, cursor));
    }

    @PostMapping("/transactions")
    public TransactionResponse createTransaction(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertTransactionRequest request) {
        return service.createTransaction(jwt.getSubject(), request);
    }

    @PatchMapping("/transactions/{id}")
    public TransactionResponse updateTransaction(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @Valid @RequestBody UpsertTransactionRequest request) {
        return service.updateTransaction(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/transactions/{id}")
    public Map<String, Object> deleteTransaction(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        service.deleteTransaction(jwt.getSubject(), id);
        return Map.of("success", true);
    }

    @GetMapping("/debts")
    public List<DebtResponse> listDebts(@AuthenticationPrincipal Jwt jwt) {
        return service.listDebts(jwt.getSubject());
    }

    @PostMapping("/debts")
    public DebtResponse createDebt(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertDebtRequest request) {
        return service.createDebt(jwt.getSubject(), request);
    }

    @PatchMapping("/debts/{id}")
    public DebtResponse updateDebt(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @Valid @RequestBody UpsertDebtRequest request) {
        return service.updateDebt(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/debts/{id}")
    public Map<String, Object> deleteDebt(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        service.deleteDebt(jwt.getSubject(), id);
        return Map.of("success", true);
    }

    @GetMapping("/recurring-payments")
    public List<RecurringPaymentResponse> listRecurringPayments(@AuthenticationPrincipal Jwt jwt) {
        return service.listRecurringPayments(jwt.getSubject());
    }

    @PostMapping("/recurring-payments")
    public RecurringPaymentResponse createRecurringPayment(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertRecurringPaymentRequest request) {
        return service.createRecurringPayment(jwt.getSubject(), request);
    }

    @PatchMapping("/recurring-payments/{id}")
    public RecurringPaymentResponse updateRecurringPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @Valid @RequestBody UpsertRecurringPaymentRequest request) {
        return service.updateRecurringPayment(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/recurring-payments/{id}")
    public Map<String, Object> deleteRecurringPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        service.deleteRecurringPayment(jwt.getSubject(), id);
        return Map.of("success", true);
    }

    @GetMapping("/sync/pull")
    public SyncPullResponse pullSync(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
                                     @RequestParam(required = false) String entity) {
        return service.pullSync(jwt.getSubject(), since, entity);
    }

    @PostMapping("/sync/push")
    public SyncPushResponse pushSync(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SyncPushRequest request) {
        return service.pushSync(jwt.getSubject(), request);
    }

    @PostMapping("/sync/resolve-conflict")
    public Map<String, Object> resolveConflict(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SyncConflictResolutionRequest request) {
        return service.resolveConflict(jwt.getSubject(), request);
    }

    @PostMapping("/migration/import-local")
    public MigrationImportResponse importLocal(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MigrationImportRequest request) {
        return service.importMigration(jwt.getSubject(), request);
    }

    @GetMapping("/stats/summary")
    public SummaryResponse summary(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(required = false) String range,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @RequestParam(required = false) String accountId) {
        return service.summary(jwt.getSubject(), range, from, to, accountId);
    }

    @GetMapping("/stats/categories")
    public List<CategoryStatResponse> categoryStats(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(required = false) String range,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(required = false) String accountId) {
        return service.categoryStats(jwt.getSubject(), range, from, to, accountId);
    }

    @GetMapping("/stats/upcoming")
    public UpcomingResponse upcoming(@AuthenticationPrincipal Jwt jwt) {
        return service.upcoming(jwt.getSubject());
    }

    @GetMapping("/backup/export")
    public BackupExportResponse exportBackup(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(required = false, defaultValue = "json") String format,
                                             @RequestParam(required = false, defaultValue = "false") boolean encrypted) {
        return service.exportBackup(jwt.getSubject());
    }

    @PostMapping("/backup/import")
    public Map<String, Object> importBackup(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BackupImportRequest request) {
        return service.importBackup(jwt.getSubject(), request);
    }
}
