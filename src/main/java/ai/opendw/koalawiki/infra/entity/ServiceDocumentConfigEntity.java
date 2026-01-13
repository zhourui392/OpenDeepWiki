package ai.opendw.koalawiki.infra.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 服务文档库配置实体
 *
 * @author zhourui(V33215020)
 * @since 2025/02/14
 */
@Data
@Entity
@Table(name = "service_document_config")
public class ServiceDocumentConfigEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "domain_id", length = 36)
    private String domainId;

    @Column(name = "service_id", nullable = false, length = 64)
    private String serviceId;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Column(length = 255)
    private String description;

    @Lob
    @Column(name = "document_content", columnDefinition = "LONGTEXT")
    private String documentContent;

    @Column(name = "doc_type", nullable = false, length = 32)
    private String docType;

    @Column(name = "prompt_template_id", length = 36)
    private String promptTemplateId;

    @Column(name = "agent_type", length = 32)
    private String agentType;

    @Lob
    @Column(name = "source_globs", columnDefinition = "LONGTEXT")
    private String sourceGlobs;

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "last_commit_id", length = 64)
    private String lastCommitId;

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
