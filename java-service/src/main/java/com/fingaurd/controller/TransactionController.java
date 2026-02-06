package com.fingaurd.controller;

import com.fingaurd.dto.request.TransactionCreateRequest;
import com.fingaurd.dto.request.TransactionUpdateRequest;
import com.fingaurd.dto.response.TransactionResponse;
import com.fingaurd.dto.response.TransactionStatistics;
import com.fingaurd.dto.response.TransactionSummary;
import com.fingaurd.model.TransactionType;
import com.fingaurd.security.UserPrincipal;
import com.fingaurd.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for transaction endpoints.
 *
 * Provides full CRUD, filtering, statistics, category analytics,
 * and fraud-flagged transaction retrieval.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    // ── Create ─────────────────────────────────────────────────────

    /**
     * Create a new transaction (US-006)
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            Authentication authentication,
            @Valid @RequestBody TransactionCreateRequest request) {

        try {
            UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
            log.info("Creating transaction for user: {} - Amount: {}, Type: {}, Category: {}",
                    currentUser.getEmail(), request.getAmount(),
                    request.getTransactionType(), request.getCategory());

            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Invalid transaction amount: {}", request.getAmount());
                return ResponseEntity.badRequest().build();
            }

            TransactionResponse response = transactionService.createTransaction(
                    currentUser.getId(), request);

            log.info("Transaction created successfully with ID: {} for user: {}",
                    response.getId(), currentUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating transaction for user: {} - Error: {}",
                    authentication.getName(), e.getMessage());
            throw e;
        }
    }

    // ── Read (list + filters) ──────────────────────────────────────

    /**
     * Get user's transactions with pagination and optional filters (US-007, US-008)
     *
     * Query parameters:
     *   page, size, sortBy, sortDir – pagination / sorting
     *   type        – INCOME or EXPENSE
     *   category    – category name
     *   startDate   – ISO date-time lower bound
     *   endDate     – ISO date-time upper bound
     *   fraudFlagged – true/false
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean fraudFlagged) {

        try {
            UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
            log.debug("Getting transactions for user: {} - Page: {}, Size: {}, type={}, category={}, fraud={}",
                    currentUser.getEmail(), page, size, type, category, fraudFlagged);

            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);

            Page<TransactionResponse> transactions = transactionService.getUserTransactions(
                    currentUser.getId(),
                    pageable,
                    type,
                    category,
                    startDate,
                    endDate,
                    fraudFlagged
            );

            log.debug("Retrieved {} transactions for user: {}",
                    transactions.getTotalElements(), currentUser.getEmail());
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error getting transactions for user: {} - Error: {}",
                    authentication.getName(), e.getMessage());
            throw e;
        }
    }

    // ── Read (single) ──────────────────────────────────────────────

    /**
     * Get specific transaction by ID (US-009)
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
            Authentication authentication,
            @PathVariable UUID id) {

        try {
            UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
            log.debug("Getting transaction {} for user: {}", id, currentUser.getEmail());

            TransactionResponse response = transactionService.getTransaction(
                    currentUser.getId(), id);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting transaction {} for user: {} - Error: {}",
                    id, authentication.getName(), e.getMessage());
            throw e;
        }
    }

    // ── Update ─────────────────────────────────────────────────────

    /**
     * Update an existing transaction (US-010)
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody TransactionUpdateRequest request) {

        try {
            UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
            log.info("Updating transaction {} for user: {}", id, currentUser.getEmail());

            TransactionResponse response = transactionService.updateTransaction(
                    currentUser.getId(), id, request);

            log.info("Transaction updated successfully: {} for user: {}",
                    id, currentUser.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating transaction {} for user: {} - Error: {}",
                    id, authentication.getName(), e.getMessage());
            throw e;
        }
    }

    // ── Delete ─────────────────────────────────────────────────────

    /**
     * Delete transaction (US-011)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            Authentication authentication,
            @PathVariable UUID id) {

        try {
            UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
            log.info("Deleting transaction {} for user: {}", id, currentUser.getEmail());

            transactionService.deleteTransaction(currentUser.getId(), id);

            log.info("Successfully deleted transaction {} for user: {}",
                    id, currentUser.getEmail());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting transaction {} for user: {} - Error: {}",
                    id, authentication.getName(), e.getMessage());
            throw e;
        }
    }

    // ── Statistics ──────────────────────────────────────────────────

    /**
     * Get transaction statistics / account balance (US-012)
     */
    @GetMapping("/stats")
    public ResponseEntity<TransactionStatistics> getStatistics(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.debug("Getting statistics for user: {}", currentUser.getEmail());

        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();

        TransactionStatistics stats = transactionService.getStatistics(
                currentUser.getId(), startDate, endDate);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get spending broken down by category (US-015)
     */
    @GetMapping("/stats/by-category")
    public ResponseEntity<Map<String, TransactionService.CategoryStatistics>> getStatsByCategory(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.debug("Getting category statistics for user: {}", currentUser.getEmail());

        Map<String, TransactionService.CategoryStatistics> stats =
                transactionService.getCategoryStatistics(currentUser.getId(), startDate, endDate);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get a summary with insights for the last N days
     */
    @GetMapping("/summary")
    public ResponseEntity<TransactionSummary> getSummary(
            Authentication authentication,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {

        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.debug("Getting transaction summary for user: {} for last {} days",
                currentUser.getEmail(), days);

        TransactionSummary summary = transactionService.getTransactionSummary(
                currentUser.getId(), days);

        return ResponseEntity.ok(summary);
    }

    // ── Fraud ──────────────────────────────────────────────────────

    /**
     * Get fraud-flagged transactions (US-014)
     */
    @GetMapping("/fraud")
    public ResponseEntity<Page<TransactionResponse>> getFraudFlaggedTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.debug("Getting fraud-flagged transactions for user: {}", currentUser.getEmail());

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());

        Page<TransactionResponse> transactions = transactionService.getFraudFlaggedTransactions(
                currentUser.getId(), pageable);

        return ResponseEntity.ok(transactions);
    }
}
