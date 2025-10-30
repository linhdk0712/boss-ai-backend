package ai.content.auto.repository;

import ai.content.auto.entity.UserRole;
import ai.content.auto.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    /**
     * Find all roles for a specific user
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.id.userId = :userId")
    List<UserRole> findByUserId(@Param("userId") Long userId);

    /**
     * Find users with a specific role
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.id.role = :role")
    List<UserRole> findByRole(@Param("role") String role);

    /**
     * Find all roles for a specific user by user ID
     */
    List<UserRole> findByIdUserId(Long userId);
}
