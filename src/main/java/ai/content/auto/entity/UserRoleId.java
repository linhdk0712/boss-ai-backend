package ai.content.auto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

@Getter
@Setter
@Embeddable
public class UserRoleId implements Serializable {
  private static final long serialVersionUID = -6999813971014367036L;

  @NotNull
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Size(max = 50)
  @NotNull
  @Column(name = "role", nullable = false, length = 50)
  private String role;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    UserRoleId entity = (UserRoleId) o;
    return Objects.equals(this.role, entity.role) && Objects.equals(this.userId, entity.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(role, userId);
  }
}
