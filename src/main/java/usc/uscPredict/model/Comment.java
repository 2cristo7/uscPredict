package usc.uscPredict.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @NotBlank(message = "El contenido no puede estar vacío")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Column(nullable = false)
    private UUID userId; // o @ManyToOne si tienes entidad User

    @NotNull
    @Column(nullable = false)
    private UUID postId; // o la entidad asociada (por ejemplo, Event, Prediction, etc.)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Comment() {}

    public Comment(UUID uuid, String content, UUID userId, UUID postId) {
        this.uuid = uuid;
        this.content = content;
        this.userId = userId;
        this.postId = postId;
    }
}
