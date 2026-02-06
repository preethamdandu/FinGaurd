package com.fingaurd.service;

import com.fingaurd.dto.request.TransactionCreateRequest;
import com.fingaurd.dto.request.TransactionUpdateRequest;
import com.fingaurd.dto.response.TransactionResponse;
import com.fingaurd.dto.response.TransactionStatistics;
import com.fingaurd.dto.response.TransactionSummary;
import com.fingaurd.exception.ResourceNotFoundException;
import com.fingaurd.exception.ValidationException;
import com.fingaurd.model.Transaction;
import com.fingaurd.model.TransactionType;
import com.fingaurd.model.User;
import com.fingaurd.repository.TransactionRepository;
import com.fingaurd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive service for transaction-related business operations
 * Handles transaction creation, fraud detection, analytics, and financial calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WebClient fraudWebClient;
    
    // Business rules and constants
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("9999999999999.99");
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("0.75");
    private static final BigDecimal SUSPICIOUS_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
    private static final int RECENT_TRANSACTION_HOURS = 24;
    
    /**
     * Create a new transaction with comprehensive validation and fraud detection
     */
    @Transactional
    public TransactionResponse createTransaction(UUID userId, TransactionCreateRequest request) {
        log.info("Creating transaction for user: {}", userId);
        
        // Validate request
        validateTransactionRequest(request);
        
        // Get user
        User user = getUserById(userId);
        
        // Perform fraud detection
        FraudDetectionResult fraudResult = performFraudDetection(user, request);
        
        // Create transaction entity
        Transaction transaction = createTransactionEntity(user, request, fraudResult);
        
        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        log.info("Transaction created successfully with ID: {} for user: {}", 
                savedTransaction.getId(), userId);
        
        // Log high-risk transactions
        if (fraudResult.isHighRisk()) {
            log.warn("HIGH RISK TRANSACTION CREATED - User: {}, Amount: {}, Risk Score: {}", 
                    userId, request.getAmount(), fraudResult.getRiskScore());
        }

        // Call external fraud detection service (non-blocking to main flow)
        try {
            callExternalFraudService(user, request, savedTransaction.getId());
        } catch (Exception ex) {
            // Swallow errors to keep main transaction successful
            log.error("External fraud service call failed for transaction {}: {}", savedTransaction.getId(), ex.getMessage());
        }
        
        return TransactionResponse.from(savedTransaction);
    }
    
    /**
     * Update an existing transaction with validation
     */
    @Transactional
    public TransactionResponse updateTransaction(UUID userId, UUID transactionId, TransactionUpdateRequest request) {
        log.info("Updating transaction {} for user: {}", transactionId, userId);
        
        // Get transaction and validate ownership
        Transaction transaction = getTransactionByIdAndUser(transactionId, userId);
        
        // Validate update request
        validateUpdateRequest(request);
        
        // Check if transaction is too old to modify (business rule)
        if (isTransactionTooOldToModify(transaction)) {
            throw new ValidationException("Transaction is too old to modify");
        }
        
        // Update transaction fields
        updateTransactionFields(transaction, request);
        
        // Re-evaluate fraud detection if amount or type changed
        if (request.getAmount() != null || request.getTransactionType() != null) {
            // Create a TransactionCreateRequest for fraud detection
            TransactionCreateRequest fraudRequest = TransactionCreateRequest.builder()
                    .amount(request.getAmount() != null ? request.getAmount() : transaction.getAmount())
                    .transactionType(request.getTransactionType() != null ? request.getTransactionType() : transaction.getTransactionType())
                    .category(request.getCategory() != null ? request.getCategory() : transaction.getCategory())
                    .description(request.getDescription() != null ? request.getDescription() : transaction.getDescription())
                    .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : transaction.getTransactionDate())
                    .build();
            
            FraudDetectionResult fraudResult = performFraudDetection(transaction.getUser(), fraudRequest);
            transaction.setIsFraudFlagged(fraudResult.isHighRisk());
            transaction.setFraudRiskScore(fraudResult.getRiskScore());
        }
        
        // Save updated transaction
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        log.info("Transaction updated successfully: {}", transactionId);
        
        return TransactionResponse.from(savedTransaction);
    }
    
    /**
     * Get user's transactions with advanced filtering and sorting
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactions(UUID userId, Pageable pageable, 
                                                         TransactionType type, String category,
                                                         LocalDateTime startDate, LocalDateTime endDate,
                                                         Boolean fraudFlagged) {
        log.debug("Getting transactions for user: {} with filters", userId);
        
        User user = getUserById(userId);
        
        // Apply default sorting if not specified
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), 
                                    Sort.by(Sort.Direction.DESC, "transactionDate"));
        }
        
        Page<Transaction> transactions;
        
        // Apply filters based on parameters
        if (type != null && category != null && startDate != null && endDate != null) {
            // For combined filters, we'll use the date range method and filter by type in memory
            // This is not ideal for performance but works for now
            Page<Transaction> categoryDateTransactions = transactionRepository.findByUserCategoryAndDateRange(user, category, startDate, endDate, pageable);
            // Filter by type - we need to get all matching transactions and recreate the page
            List<Transaction> filteredList = categoryDateTransactions.getContent().stream()
                    .filter(t -> t.getTransactionType() == type)
                    .collect(Collectors.toList());
            transactions = new PageImpl<>(filteredList, pageable, filteredList.size());
        } else if (type != null && category != null) {
            transactions = transactionRepository.findByUserAndTransactionTypeAndCategory(user, type, category, pageable);
        } else if (type != null) {
            transactions = transactionRepository.findByUserAndTransactionType(user, type, pageable);
        } else if (category != null) {
            transactions = transactionRepository.findByUserAndCategory(user, category, pageable);
        } else if (startDate != null && endDate != null) {
            transactions = transactionRepository.findByUserAndTransactionDateBetween(user, startDate, endDate, pageable);
        } else if (fraudFlagged != null) {
            transactions = transactionRepository.findByUserAndIsFraudFlagged(user, fraudFlagged, pageable);
        } else {
            transactions = transactionRepository.findByUser(user, pageable);
        }
        
        return transactions.map(TransactionResponse::from);
    }
    
    /**
     * Get specific transaction with ownership validation
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID userId, UUID transactionId) {
        log.debug("Getting transaction {} for user: {}", transactionId, userId);
        
        Transaction transaction = getTransactionByIdAndUser(transactionId, userId);
        return TransactionResponse.from(transaction);
    }
    
    /**
     * Delete transaction with validation
     */
    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        log.info("Deleting transaction {} for user: {}", transactionId, userId);
        
        Transaction transaction = getTransactionByIdAndUser(transactionId, userId);
        
        // Check if transaction is too old to delete (business rule)
        if (isTransactionTooOldToModify(transaction)) {
            throw new ValidationException("Transaction is too old to delete");
        }
        
        transactionRepository.delete(transaction);
        log.info("Transaction deleted successfully: {}", transactionId);
    }
    
    /**
     * Get comprehensive transaction statistics for a user
     */
    @Transactional(readOnly = true)
    public TransactionStatistics getStatistics(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting statistics for user: {}", userId);
        
        User user = getUserById(userId);
        
        // Set default date range if not provided
        if (startDate == null || endDate == null) {
            endDate = LocalDateTime.now();
            startDate = endDate.minusMonths(1);
        }
        
        // Calculate totals
        BigDecimal totalIncome = transactionRepository.sumAmountByUserTypeAndDateRange(user, TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = transactionRepository.sumAmountByUserTypeAndDateRange(user, TransactionType.EXPENSE, startDate, endDate);
        
        // Count transactions
        long totalTransactions = transactionRepository.countByUser(user);
        long incomeTransactions = transactionRepository.countByUserAndTransactionType(user, TransactionType.INCOME);
        long expenseTransactions = transactionRepository.countByUserAndTransactionType(user, TransactionType.EXPENSE);
        
        // Get fraud statistics
        Page<Transaction> fraudTransactions = transactionRepository.findByUserAndIsFraudFlagged(user, true, Pageable.unpaged());
        long fraudCount = fraudTransactions.getTotalElements();
        
        // Calculate balance
        BigDecimal currentBalance = (totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .subtract(totalExpenses != null ? totalExpenses : BigDecimal.ZERO);
        
        // Calculate average transaction amounts
        BigDecimal avgIncome = incomeTransactions > 0 ? 
                (totalIncome != null ? totalIncome : BigDecimal.ZERO).divide(BigDecimal.valueOf(incomeTransactions), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        BigDecimal avgExpense = expenseTransactions > 0 ? 
                (totalExpenses != null ? totalExpenses : BigDecimal.ZERO).divide(BigDecimal.valueOf(expenseTransactions), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        
        return TransactionStatistics.builder()
                .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .totalExpenses(totalExpenses != null ? totalExpenses : BigDecimal.ZERO)
                .currentBalance(currentBalance)
                .totalTransactions(totalTransactions)
                .incomeTransactions(incomeTransactions)
                .expenseTransactions(expenseTransactions)
                .fraudTransactions(fraudCount)
                .averageIncome(avgIncome)
                .averageExpense(avgExpense)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }
    
    /**
     * Get transaction summary with insights
     */
    @Transactional(readOnly = true)
    public TransactionSummary getTransactionSummary(UUID userId, int days) {
        log.debug("Getting transaction summary for user: {} for last {} days", userId, days);
        
        User user = getUserById(userId);
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        
        // Get recent transactions
        List<Transaction> recentTransactions = transactionRepository.findByUserAndTransactionDateBetween(user, startDate, endDate);
        
        // Calculate summary metrics
        Map<TransactionType, BigDecimal> amountsByType = recentTransactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getTransactionType,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
        
        Map<String, BigDecimal> amountsByCategory = recentTransactions.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
        
        // Get top categories
        List<String> topCategories = amountsByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        // Count fraud transactions
        long fraudCount = recentTransactions.stream()
                .mapToLong(t -> t.isFraud() ? 1 : 0)
                .sum();
        
        return TransactionSummary.builder()
                .periodDays(days)
                .totalIncome(amountsByType.getOrDefault(TransactionType.INCOME, BigDecimal.ZERO))
                .totalExpenses(amountsByType.getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO))
                .netAmount(amountsByType.getOrDefault(TransactionType.INCOME, BigDecimal.ZERO)
                        .subtract(amountsByType.getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO)))
                .transactionCount(recentTransactions.size())
                .fraudCount(fraudCount)
                .topCategories(topCategories)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
    
    /**
     * Get fraud-flagged transactions
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getFraudFlaggedTransactions(UUID userId, Pageable pageable) {
        log.debug("Getting fraud-flagged transactions for user: {}", userId);
        
        User user = getUserById(userId);
        
        return transactionRepository.findByUserAndIsFraudFlagged(user, true, pageable)
                .map(TransactionResponse::from);
    }
    
    /**
     * Get recent transactions for a user
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(UUID userId, int limit) {
        log.debug("Getting recent transactions for user: {}", userId);
        
        User user = getUserById(userId);
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "transactionDate"));
        
        return transactionRepository.findRecentTransactionsByUser(user, pageable)
                .stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * Get transactions by category with statistics
     */
    @Transactional(readOnly = true)
    public Map<String, CategoryStatistics> getCategoryStatistics(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting category statistics for user: {}", userId);
        
        User user = getUserById(userId);
        
        if (startDate == null || endDate == null) {
            endDate = LocalDateTime.now();
            startDate = endDate.minusMonths(1);
        }
        
        Map<String, CategoryStatistics> statistics = new HashMap<>();
        
        // Get income statistics by category
        List<Object[]> incomeStats = transactionRepository.getTransactionStatsByCategory(user, TransactionType.INCOME, startDate, endDate);
        incomeStats.forEach(stat -> {
            String category = (String) stat[0];
            Long count = (Long) stat[1];
            BigDecimal total = (BigDecimal) stat[2];
            
            statistics.computeIfAbsent(category, k -> new CategoryStatistics())
                    .setIncomeCount(count)
                    .setIncomeTotal(total);
        });
        
        // Get expense statistics by category
        List<Object[]> expenseStats = transactionRepository.getTransactionStatsByCategory(user, TransactionType.EXPENSE, startDate, endDate);
        expenseStats.forEach(stat -> {
            String category = (String) stat[0];
            Long count = (Long) stat[1];
            BigDecimal total = (BigDecimal) stat[2];
            
            statistics.computeIfAbsent(category, k -> new CategoryStatistics())
                    .setExpenseCount(count)
                    .setExpenseTotal(total);
        });
        
        return statistics;
    }
    
    // Private helper methods
    
    private User getUserById(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        
        return userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }
    
    private Transaction getTransactionByIdAndUser(UUID transactionId, UUID userId) {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID cannot be null");
        }
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found for user: " + userId);
        }
        
        return transaction;
    }
    
    private void validateTransactionRequest(TransactionCreateRequest request) {
        if (request == null) {
            throw new ValidationException("Transaction request cannot be null");
        }
        
        if (request.getAmount() == null) {
            throw new ValidationException("Amount is required");
        }
        
        if (request.getAmount().compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
            throw new ValidationException("Amount must be greater than or equal to " + MIN_TRANSACTION_AMOUNT);
        }
        
        if (request.getAmount().compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new ValidationException("Amount must not exceed " + MAX_TRANSACTION_AMOUNT);
        }
        
        if (request.getTransactionType() == null) {
            throw new ValidationException("Transaction type is required");
        }
        
        if (!StringUtils.hasText(request.getCategory())) {
            throw new ValidationException("Category is required");
        }
        
        if (request.getCategory().length() > 100) {
            throw new ValidationException("Category must not exceed 100 characters");
        }
        
        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            throw new ValidationException("Description must not exceed 1000 characters");
        }
    }
    
    private void validateUpdateRequest(TransactionUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
                throw new ValidationException("Amount must be greater than or equal to " + MIN_TRANSACTION_AMOUNT);
            }
            if (request.getAmount().compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
                throw new ValidationException("Amount must not exceed " + MAX_TRANSACTION_AMOUNT);
            }
        }
        
        if (request.getCategory() != null && request.getCategory().length() > 100) {
            throw new ValidationException("Category must not exceed 100 characters");
        }
        
        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            throw new ValidationException("Description must not exceed 1000 characters");
        }
    }
    
    private boolean isTransactionTooOldToModify(Transaction transaction) {
        // Business rule: Cannot modify transactions older than 30 days
        return ChronoUnit.DAYS.between(transaction.getCreatedAt(), LocalDateTime.now()) > 30;
    }
    
    private Transaction createTransactionEntity(User user, TransactionCreateRequest request, FraudDetectionResult fraudResult) {
        return Transaction.builder()
                .user(user)
                .amount(request.getAmount())
                .transactionType(request.getTransactionType())
                .category(request.getCategory().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
                .isFraudFlagged(fraudResult.isHighRisk())
                .fraudRiskScore(fraudResult.getRiskScore())
                .build();
    }
    
    private void updateTransactionFields(Transaction transaction, TransactionUpdateRequest request) {
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        
        if (request.getTransactionType() != null) {
            transaction.setTransactionType(request.getTransactionType());
        }
        
        if (request.getCategory() != null) {
            transaction.setCategory(request.getCategory().trim());
        }
        
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription().trim());
        }
        
        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }
    }

    private void callExternalFraudService(User user, TransactionCreateRequest request, UUID transactionId) {
        // Build request payload
        com.fingaurd.dto.fraud.FraudDetectionRequest payload = com.fingaurd.dto.fraud.FraudDetectionRequest.builder()
                .userId(Math.abs(user.getId().hashCode()))
                .amount(request.getAmount())
                .timestamp((request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now()).toString())
                .build();

        try {
            com.fingaurd.dto.fraud.FraudDetectionResponse resp = fraudWebClient.post()
                    .uri("/detect")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(com.fingaurd.dto.fraud.FraudDetectionResponse.class)
                    .block(java.time.Duration.ofSeconds(2));

            if (resp != null) {
                log.info("Fraud service result for {}: fraudulent={}, reason={}, score={}",
                        transactionId, resp.isFraudulent(), resp.getReason(), resp.getRiskScore());

                // Flag transaction based on external decision (non-blocking to main flow)
                try {
                    transactionRepository.findById(transactionId).ifPresent(tx -> {
                        tx.setIsFraudFlagged(resp.isFraudulent());
                        tx.setFraudRiskScore(java.math.BigDecimal.valueOf(resp.getRiskScore()));
                        transactionRepository.save(tx);
                    });
                } catch (Exception saveEx) {
                    log.error("Failed to persist fraud flag for {}: {}", transactionId, saveEx.getMessage());
                }
            } else {
                log.warn("Fraud service returned no body for transaction {}", transactionId);
            }
        } catch (Exception e) {
            // Do not throw; just log
            log.error("Fraud service call error for transaction {}: {}", transactionId, e.getMessage());
        }
    }
    
    /**
     * Advanced fraud detection algorithm
     */
    private FraudDetectionResult performFraudDetection(User user, TransactionCreateRequest request) {
        log.debug("Performing fraud detection for user: {}", user.getId());
        
        BigDecimal riskScore = BigDecimal.ZERO;
        List<String> riskFactors = new ArrayList<>();
        
        // Factor 1: Amount-based risk
        BigDecimal amountRisk = calculateAmountRisk(request.getAmount());
        riskScore = riskScore.add(amountRisk);
        if (amountRisk.compareTo(BigDecimal.ZERO) > 0) {
            riskFactors.add("High transaction amount");
        }
        
        // Factor 2: Recent transaction pattern
        BigDecimal patternRisk = calculatePatternRisk(user, request);
        riskScore = riskScore.add(patternRisk);
        if (patternRisk.compareTo(BigDecimal.ZERO) > 0) {
            riskFactors.add("Unusual transaction pattern");
        }
        
        // Factor 3: Time-based risk
        BigDecimal timeRisk = calculateTimeRisk(request.getTransactionDate());
        riskScore = riskScore.add(timeRisk);
        if (timeRisk.compareTo(BigDecimal.ZERO) > 0) {
            riskFactors.add("Unusual transaction time");
        }
        
        // Factor 4: Category-based risk
        BigDecimal categoryRisk = calculateCategoryRisk(request.getCategory());
        riskScore = riskScore.add(categoryRisk);
        if (categoryRisk.compareTo(BigDecimal.ZERO) > 0) {
            riskFactors.add("High-risk category");
        }
        
        // Factor 5: User behavior risk
        BigDecimal behaviorRisk = calculateBehaviorRisk(user);
        riskScore = riskScore.add(behaviorRisk);
        if (behaviorRisk.compareTo(BigDecimal.ZERO) > 0) {
            riskFactors.add("Unusual user behavior");
        }
        
        // Normalize risk score to 0-1 range
        riskScore = riskScore.min(BigDecimal.ONE);
        
        boolean isHighRisk = riskScore.compareTo(HIGH_RISK_THRESHOLD) >= 0;
        
        if (isHighRisk) {
            log.warn("HIGH RISK TRANSACTION DETECTED - User: {}, Risk Score: {}, Factors: {}", 
                    user.getId(), riskScore, riskFactors);
        }
        
        return new FraudDetectionResult(riskScore, isHighRisk, riskFactors);
    }
    
    private BigDecimal calculateAmountRisk(BigDecimal amount) {
        if (amount.compareTo(SUSPICIOUS_AMOUNT_THRESHOLD) > 0) {
            // Linear increase in risk for amounts above threshold
            return amount.subtract(SUSPICIOUS_AMOUNT_THRESHOLD)
                    .divide(SUSPICIOUS_AMOUNT_THRESHOLD, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.3")); // Max 30% risk from amount
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculatePatternRisk(User user, TransactionCreateRequest request) {
        // Get recent transactions
        LocalDateTime cutoff = LocalDateTime.now().minusHours(RECENT_TRANSACTION_HOURS);
        List<Transaction> recentTransactions = transactionRepository.findByUserAndTransactionDateBetween(user, cutoff, LocalDateTime.now());
        
        if (recentTransactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Check for unusual frequency
        if (recentTransactions.size() > 10) {
            return new BigDecimal("0.2"); // 20% risk for high frequency
        }
        
        // Check for similar large amounts
        long similarAmounts = recentTransactions.stream()
                .mapToLong(t -> t.getAmount().compareTo(request.getAmount()) == 0 ? 1 : 0)
                .sum();
        
        if (similarAmounts > 3) {
            return new BigDecimal("0.15"); // 15% risk for repeated amounts
        }
        
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateTimeRisk(LocalDateTime transactionDate) {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        
        int hour = transactionDate.getHour();
        
        // Higher risk during unusual hours (2 AM - 6 AM)
        if (hour >= 2 && hour <= 6) {
            return new BigDecimal("0.1"); // 10% risk for late night transactions
        }
        
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateCategoryRisk(String category) {
        // High-risk categories
        Set<String> highRiskCategories = Set.of(
            "Cryptocurrency", "Gambling", "Adult Services", "Cash Advance", 
            "International Transfer", "Investment"
        );
        
        if (highRiskCategories.contains(category)) {
            return new BigDecimal("0.25"); // 25% risk for high-risk categories
        }
        
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateBehaviorRisk(User user) {
        // Check if user is new (less than 30 days old)
        if (user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30))) {
            return new BigDecimal("0.1"); // 10% risk for new users
        }
        
        // Check if user is unverified
        if (!user.getIsVerified()) {
            return new BigDecimal("0.05"); // 5% risk for unverified users
        }
        
        return BigDecimal.ZERO;
    }
    
    // Inner classes for fraud detection and statistics
    
    private static class FraudDetectionResult {
        private final BigDecimal riskScore;
        private final boolean highRisk;
        private final List<String> riskFactors;
        
        public FraudDetectionResult(BigDecimal riskScore, boolean highRisk, List<String> riskFactors) {
            this.riskScore = riskScore;
            this.highRisk = highRisk;
            this.riskFactors = riskFactors;
        }
        
        public BigDecimal getRiskScore() { return riskScore; }
        public boolean isHighRisk() { return highRisk; }
        public List<String> getRiskFactors() { return riskFactors; }
    }
    
    @lombok.Data
    public static class CategoryStatistics {
        private Long incomeCount = 0L;
        private BigDecimal incomeTotal = BigDecimal.ZERO;
        private Long expenseCount = 0L;
        private BigDecimal expenseTotal = BigDecimal.ZERO;
        
        public CategoryStatistics setIncomeCount(Long count) {
            this.incomeCount = count;
            return this;
        }
        
        public CategoryStatistics setIncomeTotal(BigDecimal total) {
            this.incomeTotal = total;
            return this;
        }
        
        public CategoryStatistics setExpenseCount(Long count) {
            this.expenseCount = count;
            return this;
        }
        
        public CategoryStatistics setExpenseTotal(BigDecimal total) {
            this.expenseTotal = total;
            return this;
        }
    }
}