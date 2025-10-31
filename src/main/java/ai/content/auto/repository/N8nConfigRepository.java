package ai.content.auto.repository;

import ai.content.auto.entity.N8nConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface N8nConfigRepository extends JpaRepository<N8nConfig, Integer> {
    Optional<N8nConfig> findN8nConfigByAgentName(String agentName);
}
