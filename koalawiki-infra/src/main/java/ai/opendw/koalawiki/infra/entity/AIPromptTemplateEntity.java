package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

/**
 * AI提示词模板实体
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-18
 */
@Data
@Entity
@Table(name = "ai_prompt_template")
public class AIPromptTemplateEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "prompt_type", nullable = false, length = 50)
    private String promptType;

    @Column(name = "agent_type", nullable = false, length = 20)
    private String agentType;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Lob
    @Column(name = "template_content", nullable = false, columnDefinition = "LONGTEXT")
    private String templateContent;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, length = 20)
    private String version = "1.0.0";

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
