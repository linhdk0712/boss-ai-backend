package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "n8n_config")
public class N8nConfig {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @Size(max = 255)
  @NotNull
  @Column(name = "agent_name", nullable = false)
  private String agentName;

  @NotNull
  @Column(name = "agent_url", nullable = false, length = Integer.MAX_VALUE)
  private String agentUrl;

  @Column(name = "x_api_key", length = Integer.MAX_VALUE)
  private String xApiKey;

  @Size(max = 255)
  @NotNull
  @Column(name = "model", nullable = false)
  private String model;

  @NotNull
  @ColumnDefault("0.6")
  @Column(name = "temperature", nullable = false)
  private Double temperature;
}
