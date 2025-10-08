package usc.uscPredict.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import usc.uscPredict.model.Comment;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPostIdOrderByCreatedAtDesc(UUID postId);
}
