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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.Wallet;
import usc.uscPredict.service.WalletService;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Wallet endpoints.
 * Handles HTTP requests for wallet management and fund operations.
 * Base path: /api/v1/wallets
 */
@Tag(name = "Wallets", description = "API de gestión de carteras y operaciones de fondos")
@RestController
@RequestMapping("/api/v1/wallets")
@Validated
public class WalletController {

    private final WalletService walletService;

    @Autowired
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * GET /wallets
     * Retrieves all wallets (admin use).
     * @return 200 OK with list of wallets
     */
    @Operation(
            summary = "Listar todas las carteras",
            description = "Obtiene una lista completa de todas las carteras de usuarios (uso administrativo)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de carteras obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Wallet.class)))
    })
    @GetMapping
    public ResponseEntity<Set<Wallet>> getAllWallets() {
        Set<Wallet> wallets = walletService.getAllWallets();
        return new ResponseEntity<>(wallets, HttpStatus.OK);
    }

    /**
     * GET /wallets/{uuid}
     * Retrieves a single wallet by UUID.
     * @param uuid The wallet UUID
     * @return 200 OK with wallet, or 404 NOT FOUND
     */
    @Operation(
            summary = "Obtener cartera por ID",
            description = "Busca y retorna una cartera específica utilizando su identificador UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cartera encontrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Wallet.class))),
            @ApiResponse(responseCode = "404", description = "Cartera no encontrada", content = @Content)
    })
    @GetMapping("/{uuid}")
    public ResponseEntity<Wallet> getWalletById(
            @Parameter(description = "UUID de la cartera a buscar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        Wallet wallet = walletService.getWalletById(uuid);
        return ResponseEntity.ok(wallet);
    }

    /**
     * GET /wallets/user/{userId}
     * Retrieves or creates a wallet for a specific user.
     * If the wallet doesn't exist, creates one automatically.
     * Example: GET /wallets/user/123e4567-e89b-12d3-a456-426614174000
     * @param userId The user UUID
     * @return 200 OK with wallet
     */
    @Operation(
            summary = "Obtener cartera de usuario",
            description = "Retorna la cartera de un usuario específico. Si la cartera no existe, se crea automáticamente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cartera obtenida o creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Wallet.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<Wallet> getWalletByUserId(
            @Parameter(description = "UUID del usuario", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(wallet);
    }

    /**
     * POST /wallets
     * Creates a new wallet manually.
     * Note: Wallets are usually created automatically when accessing /wallets/user/{userId}
     * @param wallet The wallet data (JSON body)
     * @return 201 CREATED with created wallet, or 400/409 on error
     */
    @Operation(
            summary = "Crear cartera manualmente",
            description = "Crea una nueva cartera de forma manual. NOTA: Las carteras normalmente se crean automáticamente al acceder a /wallets/user/{userId}"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cartera creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Wallet.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflicto - La cartera ya existe", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Wallet> createWallet(@RequestBody @Valid @NonNull Wallet wallet) {
        Wallet created = walletService.createWallet(wallet);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * POST /wallets/deposit
     * Deposits funds into a user's wallet.
     * Body: { "userId": "...", "amount": 1000.00 }
     * @param request The deposit request
     * @return 200 OK with updated wallet
     */
    @Operation(
            summary = "Depositar fondos",
            description = "Deposita fondos en la cartera de un usuario. Incrementa el balance disponible"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Depósito realizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Wallet.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o monto negativo", content = @Content)
    })
    @PostMapping("/deposit")
    public ResponseEntity<Wallet> deposit(@RequestBody @Valid DepositRequest request) {
        Wallet wallet = walletService.deposit(request.getUserId(), request.getAmount());
        return ResponseEntity.ok(wallet);
    }

    /**
     * POST /wallets/withdraw
     * Withdraws funds from a user's wallet.
     * Body: { "userId": "...", "amount": 100.00 }
     * @param request The withdrawal request
     * @return 200 OK with updated wallet, or 400 if insufficient funds
     */
    @Operation(
            summary = "Retirar fondos",
            description = "Retira fondos de la cartera de un usuario. Requiere saldo disponible suficiente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retiro realizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Wallet.class))),
            @ApiResponse(responseCode = "400", description = "Fondos insuficientes o datos de entrada inválidos", content = @Content)
    })
    @PostMapping("/withdraw")
    public ResponseEntity<Wallet> withdraw(@RequestBody @Valid WithdrawRequest request) {
        Wallet wallet = walletService.withdraw(request.getUserId(), request.getAmount());
        return ResponseEntity.ok(wallet);
    }

    /**
     * GET /wallets/user/{userId}/balance
     * Gets the total balance (available + locked) for a user.
     * @param userId The user UUID
     * @return 200 OK with balance info
     */
    @Operation(
            summary = "Obtener balance de usuario",
            description = "Retorna información detallada del balance: disponible, bloqueado y total"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BalanceInfo.class)))
    })
    @GetMapping("/user/{userId}/balance")
    public ResponseEntity<BalanceInfo> getUserBalance(
            @Parameter(description = "UUID del usuario", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        BigDecimal total = walletService.getTotalBalance(userId);

        BalanceInfo info = new BalanceInfo(
            wallet.getBalance(),
            wallet.getLockedBalance(),
            total
        );

        return ResponseEntity.ok(info);
    }

    /**
     * DELETE /wallets/{uuid}
     * Deletes a wallet.
     * WARNING: Only for testing/development.
     * @param uuid The wallet UUID
     * @return 204 NO CONTENT if deleted, or 404 NOT FOUND
     */
    @Operation(
            summary = "Eliminar cartera",
            description = "Elimina permanentemente una cartera. ADVERTENCIA: Solo para desarrollo/pruebas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cartera eliminada exitosamente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cartera no encontrada", content = @Content)
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteWallet(
            @Parameter(description = "UUID de la cartera a eliminar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        boolean deleted = walletService.deleteWallet(uuid);
        if (deleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Inner class for deposit requests.
     */
    public static class DepositRequest {
        @NotNull(message = "User ID cannot be null")
        private UUID userId;

        @NotNull(message = "Amount cannot be null")
        @DecimalMin(value = "0.01", message = "Deposit amount must be at least 0.01")
        private BigDecimal amount;

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    /**
     * Inner class for withdrawal requests.
     */
    public static class WithdrawRequest {
        @NotNull(message = "User ID cannot be null")
        private UUID userId;

        @NotNull(message = "Amount cannot be null")
        @DecimalMin(value = "0.01", message = "Withdrawal amount must be at least 0.01")
        private BigDecimal amount;

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    /**
     * Inner class for balance information response.
     */
    public static class BalanceInfo {
        private BigDecimal availableBalance;
        private BigDecimal lockedBalance;
        private BigDecimal totalBalance;

        public BalanceInfo(BigDecimal availableBalance, BigDecimal lockedBalance, BigDecimal totalBalance) {
            this.availableBalance = availableBalance;
            this.lockedBalance = lockedBalance;
            this.totalBalance = totalBalance;
        }

        public BigDecimal getAvailableBalance() {
            return availableBalance;
        }

        public void setAvailableBalance(BigDecimal availableBalance) {
            this.availableBalance = availableBalance;
        }

        public BigDecimal getLockedBalance() {
            return lockedBalance;
        }

        public void setLockedBalance(BigDecimal lockedBalance) {
            this.lockedBalance = lockedBalance;
        }

        public BigDecimal getTotalBalance() {
            return totalBalance;
        }

        public void setTotalBalance(BigDecimal totalBalance) {
            this.totalBalance = totalBalance;
        }
    }
}
