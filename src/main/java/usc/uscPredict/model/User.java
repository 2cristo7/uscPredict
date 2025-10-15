package usc.uscPredict.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Usuario de la plataforma USC Predict")
@Entity
@Table(name = "users")
public class User {

    @Schema(description = "Identificador único del usuario", example = "123e4567-e89b-12d3-a456-426614174000", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
    @Setter
    @NotBlank
    private String name;

    @Schema(description = "Email único del usuario", example = "juan.perez@usc.edu", requiredMode = Schema.RequiredMode.REQUIRED)
    @Setter
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Schema(description = "Hash de la contraseña del usuario", example = "$2a$10$N9qo8uLOickgx2ZMRZoMye", accessMode = Schema.AccessMode.WRITE_ONLY)
    @Setter
    @Column
    private String pswd_hash;

    @Schema(description = "Rol del usuario en la plataforma", example = "USER", allowableValues = {"USER", "ADMIN"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Schema(description = "Fecha y hora de creación del usuario", example = "2025-10-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime created_at;

    // --- Constructor vacío (requerido por JPA)
    public User() {}

    // --- Constructor para crear nuevos usuarios
    public User(String name, String email, String pswd_hash, Role role) {
        this.name = name;
        this.email = email;
        this.pswd_hash = pswd_hash;
        this.role = role;
    }

    // --- Callback: se ejecuta justo antes de insertar en BD
    @PrePersist
    public void prePersist() {
        this.created_at = LocalDateTime.now();
    }

    // --- Getters ---
    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPswd_hash() {
        return pswd_hash;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }
}
