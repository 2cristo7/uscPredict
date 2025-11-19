package usc.uscPredict.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Comentario en un evento o post")
@Entity
@Table(name = "comments")
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Comment {

    @Schema(description = "Identificador único del comentario", example = "123e4567-e89b-12d3-a456-426614174000", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Schema(description = "Contenido del comentario", example = "Creo que este evento tiene alta probabilidad de resolverse como YES", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El contenido no puede estar vacío")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Schema(description = "UUID del usuario que creó el comentario", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(nullable = false)
    private UUID userId;

    @Schema(description = "UUID del post/evento asociado al comentario", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(nullable = false)
    private UUID postId;

    @Schema(description = "Fecha y hora de creación del comentario", example = "2025-10-15T14:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    public Comment() {}

    public Comment(UUID uuid, String content, UUID userId, UUID postId) {
        this.uuid = uuid;
        this.content = content;
        this.userId = userId;
        this.postId = postId;
    }
}
