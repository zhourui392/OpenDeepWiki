package ai.opendw.koalawiki.infra.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 领域实体
 *
 * @author zhourui(V33215020)
 * @since 2025/01/13
 */
@Data
@Entity
@Table(name = "domain_info")
public class DomainInfoEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 64)
    private String code;

    @Lob
    @Column(name = "document_content", columnDefinition = "LONGTEXT")
    private String documentContent;

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
