package usc.uscPredict.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import usc.uscPredict.model.Comment;
import usc.uscPredict.repository.CommentRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository repository;

    @Autowired
    public CommentService(CommentRepository repository) {
        this.repository = repository;
    }

    public List<Comment> getCommentsByPost(UUID postId) {
        return repository.findByPostIdOrderByCreatedAtDesc(postId);
    }

    public Comment addComment(Comment comment) {
        return repository.save(comment);
    }

    public void deleteComment(UUID commentId) {
        repository.deleteById(commentId);
    }

    public Comment getCommentById(UUID commentId) {
        return repository.findById(commentId).orElse(null);
    }

    public Comment updateComment(UUID commentId, Comment comment) {
        if (repository.existsById(commentId)) {
            comment.setUuid(commentId);
            return repository.save(comment);
        }
        return null;
    }
}
