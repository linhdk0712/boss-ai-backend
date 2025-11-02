package ai.content.auto.repository;

import ai.content.auto.entity.VUserConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VUserConfigRepository extends JpaRepository<VUserConfig, Long> {
  @Query(
      nativeQuery = true,
      value = "select * from v_user_configs  WHERE (user_id = ?1 OR user_id IS NULL)AND category = ?2 order by sort_order")
  List<VUserConfig> findAllByCategory(Long userId,String category);
}
