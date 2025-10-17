package usc.uscPredict.controller;

import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.Transaction;
import usc.uscPredict.service.TransactionService;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Transaction endpoints.
 * Handles HTTP requests for transaction history and audit logs.
 * Base path: /transactions
 *
 * Note: Most transactions are created automatically by other services.
 * This controller is mainly for reading transaction history.
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * GET /transactions
     * Retrieves all transactions (admin use).
     * WARNING: This should be paginated in production.
     * @return 200 OK with list of transactions
     */
    @GetMapping
    public ResponseEntity<Set<Transaction>> getAllTransactions() {
        Set<Transaction> transactions = transactionService.getAllTransactions();
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    /**
     * GET /transactions/{uuid}
     * Retrieves a single transaction by UUID.
     * @param uuid The transaction UUID
     * @return 200 OK with transaction, or 404 NOT FOUND
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable UUID uuid) {
        Transaction transaction = transactionService.getTransactionById(uuid);
        if (transaction != null) {
            return new ResponseEntity<>(transaction, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * POST /transactions
     * Creates a new transaction record.
     * Note: This is typically done automatically by other services.
     * Manual creation should be restricted to admins.
     * @param transaction The transaction data (JSON body)
     * @return 201 CREATED with created transaction
     */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @RequestBody @Valid @NonNull Transaction transaction) {
        Transaction created = transactionService.createTransaction(transaction);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * DELETE /transactions/{uuid}
     * Deletes a transaction.
     * WARNING: Deleting transactions breaks audit trail. Use with caution.
     * @param uuid The transaction UUID
     * @return 204 NO CONTENT if deleted, or 404 NOT FOUND
     */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable UUID uuid) {
        boolean deleted = transactionService.deleteTransaction(uuid);
        if (deleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * GET /transactions/user/{userId}
     * Retrieves all transactions for a specific user.
     * Example: GET /transactions/user/123e4567-e89b-12d3-a456-426614174000
     * @param userId The user UUID
     * @return 200 OK with list of transactions
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Set<Transaction>> getTransactionsByUserId(@PathVariable UUID userId) {
        Set<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    /**
     * GET /transactions/user/{userId}/history
     * Retrieves transaction history for a user (ordered by date, newest first).
     * This is the main endpoint for showing user transaction history.
     * Example: GET /transactions/user/123e4567.../history
     * @param userId The user UUID
     * @return 200 OK with transaction history
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<Set<Transaction>> getTransactionHistory(@PathVariable UUID userId) {
        Set<Transaction> history = transactionService.getTransactionHistory(userId);
        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    /**
     * GET /transactions/user/{userId}/stats
     * Retrieves transaction statistics for a user.
     * Returns total deposits, withdrawals, and trading P&L.
     * Example: GET /transactions/user/123e4567.../stats
     * @param userId The user UUID
     * @return 200 OK with user statistics
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<UserTransactionStats> getUserTransactionStats(@PathVariable UUID userId) {
        BigDecimal totalDeposits = transactionService.getTotalDeposits(userId);
        BigDecimal totalWithdrawals = transactionService.getTotalWithdrawals(userId);
        BigDecimal tradingPnL = transactionService.getTradingPnL(userId);

        UserTransactionStats stats = new UserTransactionStats(
            totalDeposits,
            totalWithdrawals,
            tradingPnL
        );

        return new ResponseEntity<>(stats, HttpStatus.OK);
    }

    /**
     * Inner class for user transaction statistics.
     */
    public static class UserTransactionStats {
        private BigDecimal totalDeposits;
        private BigDecimal totalWithdrawals;
        private BigDecimal tradingPnL;

        public UserTransactionStats(
                BigDecimal totalDeposits,
                BigDecimal totalWithdrawals,
                BigDecimal tradingPnL) {
            this.totalDeposits = totalDeposits;
            this.totalWithdrawals = totalWithdrawals;
            this.tradingPnL = tradingPnL;
        }

        public BigDecimal getTotalDeposits() {
            return totalDeposits;
        }

        public void setTotalDeposits(BigDecimal totalDeposits) {
            this.totalDeposits = totalDeposits;
        }

        public BigDecimal getTotalWithdrawals() {
            return totalWithdrawals;
        }

        public void setTotalWithdrawals(BigDecimal totalWithdrawals) {
            this.totalWithdrawals = totalWithdrawals;
        }

        public BigDecimal getTradingPnL() {
            return tradingPnL;
        }

        public void setTradingPnL(BigDecimal tradingPnL) {
            this.tradingPnL = tradingPnL;
        }
    }
}
