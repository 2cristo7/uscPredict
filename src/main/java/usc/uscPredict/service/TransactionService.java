package usc.uscPredict.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.exception.TransactionNotFoundException;
import usc.uscPredict.model.Transaction;
import usc.uscPredict.model.TransactionType;
import usc.uscPredict.repository.TransactionRepository;
import usc.uscPredict.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for Transaction entity.
 * Handles audit logging of all balance and position changes.
 * Transactions are typically created by other services, not directly by users.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves all transactions in the system.
     * Note: In production, this should be paginated.
     * @return Set of all transactions
     */
    public Set<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Retrieves a single transaction by its UUID.
     * @param uuid The transaction's unique identifier
     * @return The transaction if found
     * @throws TransactionNotFoundException if the transaction is not found
     */
    public Transaction getTransactionById(UUID uuid) {
        return transactionRepository.findById(uuid)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + uuid));
    }

    /**
     * Creates a new transaction record.
     * This is typically called by other services (OrderService, WalletService)
     * to log balance or position changes.
     * @param transaction The transaction to record
     * @return The created transaction with generated UUID
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        // TODO: Add validation
        // - Verify user exists
        // - Verify related order exists (if provided)
        return transactionRepository.save(transaction);
    }

    /**
     * Helper method to create and save a transaction.
     * Useful for other services to quickly log events.
     * @param userId The user UUID
     * @param type The transaction type
     * @param amount The amount
     * @return The created transaction
     */
    @Transactional
    public Transaction logTransaction(UUID userId, TransactionType type, BigDecimal amount) {
        Transaction transaction = new Transaction(userId, type, amount);
        return transactionRepository.save(transaction);
    }

    /**
     * Helper method to create and save a transaction with order reference.
     * @param userId The user UUID
     * @param type The transaction type
     * @param amount The amount
     * @param relatedOrderId The related order UUID
     * @param description Optional description
     * @return The created transaction
     */
    @Transactional
    public Transaction logTransactionWithOrder(
            UUID userId,
            TransactionType type,
            BigDecimal amount,
            UUID relatedOrderId,
            String description) {
        Transaction transaction = new Transaction(userId, type, amount, relatedOrderId, description);
        return transactionRepository.save(transaction);
    }

    /**
     * Deletes a transaction by UUID.
     * WARNING: In production, transactions should NEVER be deleted (audit trail).
     * @param uuid The UUID of the transaction to delete
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean deleteTransaction(UUID uuid) {
        if (transactionRepository.existsById(uuid)) {
            // WARNING: Deleting transactions breaks audit trail
            transactionRepository.deleteById(uuid);
            return true;
        }
        return false;
    }

    /**
     * Retrieves all transactions for a specific user.
     * @param userId The user UUID
     * @return Set of transactions for the user
     */
    public Set<Transaction> getTransactionsByUserId(UUID userId) {
        return transactionRepository.findByUserId(userId);
    }

    /**
     * Retrieves all transactions for a user, ordered by date (newest first).
     * Useful for showing transaction history.
     * @param userId The user UUID
     * @return Set of transactions ordered by date descending
     */
    public Set<Transaction> getTransactionHistory(UUID userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Gets the total deposited amount for a user.
     * TODO: Implement calculation logic
     * @param userId The user UUID
     * @return Total deposits
     */
    public BigDecimal getTotalDeposits(UUID userId) {
        // TODO: Filter transactions by type DEPOSIT and sum amounts
        return BigDecimal.ZERO; // Placeholder
    }

    /**
     * Gets the total withdrawn amount for a user.
     * TODO: Implement calculation logic
     * @param userId The user UUID
     * @return Total withdrawals
     */
    public BigDecimal getTotalWithdrawals(UUID userId) {
        // TODO: Filter transactions by type WITHDRAWAL and sum amounts
        return BigDecimal.ZERO; // Placeholder
    }

    /**
     * Gets net trading profit/loss for a user.
     * TODO: Implement calculation logic
     * @param userId The user UUID
     * @return Net trading P&L
     */
    public BigDecimal getTradingPnL(UUID userId) {
        // TODO: Calculate based on ORDER_EXECUTED and SETTLEMENT transactions
        return BigDecimal.ZERO; // Placeholder
    }
}
