package usc.uscPredict.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.Comment;
import usc.uscPredict.service.CommentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    // Obtener todos los comentarios de un post
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(service.getCommentsByPost(postId));
    }

    // Crear un comentario
    @PostMapping
    public ResponseEntity<Comment> createComment(@RequestBody @Valid Comment comment) {
        Comment saved = service.addComment(comment);
        return ResponseEntity.ok(saved);
    }

    // Eliminar un comentario
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        service.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
