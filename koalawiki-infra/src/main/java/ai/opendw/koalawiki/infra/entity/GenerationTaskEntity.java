package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

/**
 * 文档生成任务实体
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
@Entity
@Table(name = "generation_task")
public class GenerationTaskEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "service_id", nullable = false, length = 64)
    private String serviceId;

    @Column(name = "doc_type", nullable = false, length = 32)
    private String docType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "total_files")
    private Integer totalFiles;

    @Column(name = "completed_files")
    private Integer completedFiles;

    @Column(name = "failed_files")
    private Integer failedFiles;

    @Column(name = "agent_type", length = 20)
    private String agentType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "started_at")
    private Date startedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "completed_at")
    private Date completedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
