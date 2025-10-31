package ai.content.auto.repository;

import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentGenerationRepository extends JpaRepository<ContentGeneration, Long> {
    List<ContentGeneration> findByUserOrderByCreatedAtDesc(User user);
}
