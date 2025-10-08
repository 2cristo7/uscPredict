package usc.uscPredict.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Setter
    @NotBlank
    private String name;

    @Setter
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Setter
    @Column
    private String pswd_hash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

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
