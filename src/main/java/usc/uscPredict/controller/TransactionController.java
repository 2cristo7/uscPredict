package usc.uscPredict.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.Transaction;
import usc.uscPredict.service.TransactionService;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Transaction endpoints.
 * Handles HTTP requests for transaction history and audit logs.
 * Base path: /api/v1/transactions
 *
 * Note: Most transactions are created automatically by other services.
 * This controller is mainly for reading transaction history.
 */
@Tag(name = "Transactions", description = "API de historial de transacciones y auditoría")
@RestController
@RequestMapping("/api/v1/transactions")
@Validated
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
    @Operation(
            summary = "Listar todas las transacciones",
            description = "Obtiene una lista completa de todas las transacciones (uso administrativo). ADVERTENCIA: Debería estar paginada en producción"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de transacciones obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class)))
    })
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
    @Operation(
            summary = "Obtener transacción por ID",
            description = "Busca y retorna una transacción específica utilizando su identificador UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transacción encontrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class))),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada", content = @Content)
    })
    @GetMapping("/{uuid}")
    public ResponseEntity<Transaction> getTransactionById(
            @Parameter(description = "UUID de la transacción a buscar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        Transaction transaction = transactionService.getTransactionById(uuid);
        return ResponseEntity.ok(transaction);
    }

    /**
     * POST /transactions
     * Creates a new transaction record.
     * Note: This is typically done automatically by other services.
     * Manual creation should be restricted to admins.
     * @param transaction The transaction data (JSON body)
     * @return 201 CREATED with created transaction
     */
    @Operation(
            summary = "Crear transacción manualmente",
            description = "Crea un nuevo registro de transacción de forma manual. NOTA: Las transacciones normalmente se crean automáticamente. La creación manual debería estar restringida a administradores"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transacción creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content)
    })
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
    @Operation(
            summary = "Eliminar transacción",
            description = "Elimina permanentemente una transacción. ADVERTENCIA: Eliminar transacciones rompe el registro de auditoría. Usar con precaución"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transacción eliminada exitosamente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada", content = @Content)
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "UUID de la transacción a eliminar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
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
    @Operation(
            summary = "Buscar transacciones por usuario",
            description = "Retorna todas las transacciones realizadas por un usuario específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de transacciones obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<Set<Transaction>> getTransactionsByUserId(
            @Parameter(description = "UUID del usuario", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
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
    @Operation(
            summary = "Obtener historial de transacciones de usuario",
            description = "Retorna el historial completo de transacciones de un usuario, ordenado por fecha (más reciente primero). Este es el endpoint principal para mostrar historial"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial de transacciones obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class)))
    })
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<Set<Transaction>> getTransactionHistory(
            @Parameter(description = "UUID del usuario", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
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
    @Operation(
            summary = "Obtener estadísticas de transacciones de usuario",
            description = "Retorna estadísticas agregadas de transacciones: total de depósitos, retiros y P&L (ganancias/pérdidas) de trading"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserTransactionStats.class)))
    })
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<UserTransactionStats> getUserTransactionStats(
            @Parameter(description = "UUID del usuario", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
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
