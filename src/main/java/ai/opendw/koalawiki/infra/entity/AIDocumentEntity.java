package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

/**
 * AI文档实体
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
@Entity
@Table(name = "ai_document")
public class AIDocumentEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "service_id", nullable = false, length = 64)
    private String serviceId;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Column(name = "doc_type", nullable = false, length = 32)
    private String docType;

    @Column(name = "prompt_template_id", length = 36)
    private String promptTemplateId;

    @Column(name = "source_file", nullable = false, length = 500)
    private String sourceFile;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "agent_type", length = 20)
    private String agentType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Lob
    @Column(name = "metadata", columnDefinition = "LONGTEXT")
    private String metadata;

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
