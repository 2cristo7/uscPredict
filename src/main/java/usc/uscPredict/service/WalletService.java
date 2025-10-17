package usc.uscPredict.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.model.TransactionType;
import usc.uscPredict.model.Wallet;
import usc.uscPredict.repository.UserRepository;
import usc.uscPredict.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for Wallet entity.
 * Handles wallet management, deposits, withdrawals, and fund locking/unlocking.
 */
@Getter
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    @Autowired
    public WalletService(
            WalletRepository walletRepository,
            UserRepository userRepository,
            TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    /**
     * Retrieves all wallets in the system.
     * @return Set of all wallets
     */
    public Set<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    /**
     * Retrieves a single wallet by its UUID.
     * @param uuid The wallet's unique identifier
     * @return The wallet if found, null otherwise
     */
    public Wallet getWalletById(UUID uuid) {
        Optional<Wallet> wallet = walletRepository.findById(uuid);
        return wallet.orElse(null);
    }

    /**
     * Retrieves or creates a wallet for a specific user.
     * If the user doesn't have a wallet, creates one with zero balance.
     * @param userId The user UUID
     * @return The user's wallet
     */
    @Transactional
    public Wallet getWalletByUserId(UUID userId) {
        Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);

        if (walletOpt.isPresent()) {
            return walletOpt.get();
        }

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        // Create new wallet with zero balance
        Wallet newWallet = new Wallet(userId);
        return walletRepository.save(newWallet);
    }

    /**
     * Creates a new wallet manually.
     * Note: Normally wallets are created automatically via getWalletByUserId()
     * @param wallet The wallet to create
     * @return The created wallet
     */
    @Transactional
    public Wallet createWallet(Wallet wallet) {
        // Verify user exists
        if (!userRepository.existsById(wallet.getUserId())) {
            throw new IllegalArgumentException("User not found");
        }

        // Check if wallet already exists for this user
        Optional<Wallet> existing = walletRepository.findByUserId(wallet.getUserId());
        if (existing.isPresent()) {
            throw new IllegalStateException("Wallet already exists for this user");
        }

        return walletRepository.save(wallet);
    }

    /**
     * Deposits funds into a user's wallet.
     * Creates a DEPOSIT transaction.
     * @param userId The user UUID
     * @param amount The amount to deposit (must be positive)
     * @return The updated wallet
     */
    @Transactional
    public Wallet deposit(UUID userId, BigDecimal amount) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        // Get or create wallet
        Wallet wallet = getWalletByUserId(userId);

        // Add to balance
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);

        // Save wallet
        Wallet updatedWallet = walletRepository.save(wallet);

        // Create transaction record
        transactionService.logTransaction(userId, TransactionType.DEPOSIT, amount);

        return updatedWallet;
    }

    /**
     * Withdraws funds from a user's wallet.
     * Creates a WITHDRAWAL transaction.
     * @param userId The user UUID
     * @param amount The amount to withdraw (must be positive)
     * @return The updated wallet
     */
    @Transactional
    public Wallet withdraw(UUID userId, BigDecimal amount) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        // Get wallet
        Wallet wallet = getWalletByUserId(userId);

        // Check sufficient balance
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance for withdrawal");
        }

        // Subtract from balance
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);

        // Save wallet
        Wallet updatedWallet = walletRepository.save(wallet);

        // Create transaction record
        transactionService.logTransaction(userId, TransactionType.WITHDRAWAL, amount);

        return updatedWallet;
    }

    /**
     * Locks funds in a user's wallet.
     * Moves funds from available balance to locked balance.
     * Used when placing orders.
     * @param userId The user UUID
     * @param amount The amount to lock
     * @return The updated wallet
     */
    @Transactional
    public Wallet lockFunds(UUID userId, BigDecimal amount) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Lock amount must be positive");
        }

        // Get wallet
        Wallet wallet = getWalletByUserId(userId);

        // Check sufficient available balance
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException(
                String.format("Insufficient balance. Available: %s, Required: %s",
                    wallet.getBalance(), amount)
            );
        }

        // Move from balance to lockedBalance
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));

        // Save and return
        return walletRepository.save(wallet);
    }

    /**
     * Unlocks funds in a user's wallet.
     * Moves funds from locked balance back to available balance.
     * Used when cancelling orders.
     * @param userId The user UUID
     * @param amount The amount to unlock
     * @return The updated wallet
     */
    @Transactional
    public Wallet unlockFunds(UUID userId, BigDecimal amount) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unlock amount must be positive");
        }

        // Get wallet
        Wallet wallet = getWalletByUserId(userId);

        // Check sufficient locked balance
        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new IllegalStateException(
                String.format("Insufficient locked balance. Locked: %s, Required: %s",
                    wallet.getLockedBalance(), amount)
            );
        }

        // Move from lockedBalance to balance
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));

        // Save and return
        return walletRepository.save(wallet);
    }

    /**
     * Transfers locked funds from one user to another.
     * Used when executing trades.
     * The buyer's locked funds are transferred to the seller's available balance.
     * @param fromUserId The buyer's user ID (source)
     * @param toUserId The seller's user ID (destination)
     * @param amount The amount to transfer
     * @return The buyer's updated wallet
     */
    @Transactional
    public Wallet transferLockedToUser(UUID fromUserId, UUID toUserId, BigDecimal amount) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Get both wallets
        Wallet fromWallet = getWalletByUserId(fromUserId);
        Wallet toWallet = getWalletByUserId(toUserId);

        // Check sufficient locked balance in source wallet
        if (fromWallet.getLockedBalance().compareTo(amount) < 0) {
            throw new IllegalStateException(
                String.format("Insufficient locked balance for transfer. Locked: %s, Required: %s",
                    fromWallet.getLockedBalance(), amount)
            );
        }

        // Transfer: subtract from sender's locked, add to receiver's balance
        fromWallet.setLockedBalance(fromWallet.getLockedBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));

        // Save both wallets
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        return fromWallet;
    }

    /**
     * Deletes a wallet by UUID.
     * WARNING: Should only be used in testing/development.
     * @param uuid The wallet UUID
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean deleteWallet(UUID uuid) {
        if (walletRepository.existsById(uuid)) {
            walletRepository.deleteById(uuid);
            return true;
        }
        return false;
    }

    /**
     * Gets the total balance (available + locked) for a user.
     * @param userId The user UUID
     * @return Total balance
     */
    public BigDecimal getTotalBalance(UUID userId) {
        Wallet wallet = getWalletByUserId(userId);
        return wallet.getBalance().add(wallet.getLockedBalance());
    }
}
