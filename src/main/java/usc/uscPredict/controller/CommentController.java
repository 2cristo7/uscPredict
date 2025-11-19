package usc.uscPredict.controller;

import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.Comment;
import usc.uscPredict.service.CommentService;
import usc.uscPredict.util.PatchUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Comments", description = "API de gestión de comentarios")
@RestController
@RequestMapping("/api/v1/comments")
@Validated
public class CommentController {

    private final CommentService service;
    private final PatchUtils patchUtils;

    public CommentController(CommentService service, PatchUtils patchUtils) {
        this.service = service;
        this.patchUtils = patchUtils;
    }

    @Operation(
            summary = "Obtener comentarios de un post",
            description = "Retorna todos los comentarios asociados a un evento/post específico, ordenados por fecha de creación (más reciente primero)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de comentarios obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Comment.class)))
    })
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsByPost(
            @Parameter(description = "UUID del post/evento para obtener sus comentarios", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID postId) {
        return ResponseEntity.ok(service.getCommentsByPost(postId));
    }

    @Operation(
            summary = "Crear nuevo comentario",
            description = "Crea un nuevo comentario en un evento/post. Requiere contenido, userId y postId"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comentario creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Comment.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Comment> createComment(
            @Parameter(description = "Datos del comentario a crear", required = true)
            @RequestBody @Valid Comment comment) {
        Comment saved = service.addComment(comment);
        return ResponseEntity.ok(saved);
    }

    @Operation(
            summary = "Eliminar comentario",
            description = "Elimina permanentemente un comentario por su ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Comentario eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Comentario no encontrado", content = @Content)
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "UUID del comentario a eliminar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID commentId) {
        service.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Editar comentario con JSON-Patch",
            description = "Aplica operaciones JSON-Patch (RFC 6902) para actualizar un comentario. Solo el contenido puede ser editado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comentario actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Comment.class))),
            @ApiResponse(responseCode = "400", description = "Operación de patch inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Comentario no encontrado", content = @Content)
    })
    @PatchMapping("/{commentId}")
    public ResponseEntity<Comment> patchComment(
            @Parameter(description = "UUID del comentario a editar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID commentId,
            @Parameter(description = "Lista de operaciones JSON-Patch", required = true)
            @RequestBody List<Map<String, Object>> updates) throws JsonPatchException {
        // 1. Obter o comentario da base de datos (throws exception if not found)
        Comment existingComment = service.getCommentById(commentId);

        // 2. Aplicar as operacións JSON-Patch (JsonPatchException handled globally)
        Comment patchedComment = patchUtils.applyPatch(existingComment, updates);

        // 3. Gardar o comentario actualizado
        Comment updated = service.updateComment(commentId, patchedComment);
        return ResponseEntity.ok(updated);
    }
}
