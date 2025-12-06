package usc.uscPredict.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Schema(description = "Usuario de la plataforma USC Predict")
@Entity
@Table(name = "users")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User implements UserDetails {

    // --- Interfaces para vistas JSON ---
    public interface UserSummaryView {}
    public interface UserDetailView extends UserSummaryView {}

    @Schema(description = "Identificador único del usuario", example = "123e4567-e89b-12d3-a456-426614174000", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonView(UserSummaryView.class)
    private UUID uuid;

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
    @Setter
    @NotBlank
    @JsonView(UserSummaryView.class)
    private String name;

    @Schema(description = "Email único del usuario", example = "juan.perez@usc.edu", requiredMode = Schema.RequiredMode.REQUIRED)
    @Setter
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    @JsonView(UserSummaryView.class)
    private String email;

    @Schema(description = "Hash de la contraseña del usuario", example = "$2a$10$N9qo8uLOickgx2ZMRZoMye", accessMode = Schema.AccessMode.WRITE_ONLY)
    @Setter
    @NotBlank(message = "Password hash cannot be empty")
    @Column
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pswd_hash;

    @Schema(description = "Rol del usuario en la plataforma", example = "USER", allowableValues = {"USER", "ADMIN"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @Setter
    @NotNull(message = "Role cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonView(UserDetailView.class)
    private Role role;

    @Schema(description = "Fecha y hora de creación del usuario", example = "2025-10-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    @JsonView(UserDetailView.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
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

    // --- UserDetails implementation ---
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return pswd_hash;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }
}
