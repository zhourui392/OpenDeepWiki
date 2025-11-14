package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Date;

/**
 * 访问日志JPA实体
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "access_log", indexes = {
        @Index(name = "idx_accesslog_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_accesslog_user_id", columnList = "user_id"),
        @Index(name = "idx_accesslog_access_time", columnList = "access_time"),
        @Index(name = "idx_accesslog_action", columnList = "action"),
        @Index(name = "idx_accesslog_warehouse_access_time", columnList = "warehouse_id,access_time")
})
public class AccessLogEntity extends BaseJpaEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（如果已登录）
     */
    @Column(name = "user_id", length = 50)
    private String userId;

    /**
     * 仓库ID
     */
    @Column(name = "warehouse_id", length = 50)
    private String warehouseId;

    /**
     * 文档ID
     */
    @Column(name = "document_id", length = 50)
    private String documentId;

    /**
     * 访问动作
     */
    @Column(name = "action", length = 20, nullable = false)
    private String action;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * User Agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 请求URI
     */
    @Column(name = "request_uri", length = 500)
    private String requestUri;

    /**
     * 请求方法
     */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /**
     * 响应时间（毫秒）
     */
    @Column(name = "response_time")
    private Integer responseTime;

    /**
     * HTTP状态码
     */
    @Column(name = "status_code")
    private Integer statusCode;

    /**
     * 访问时间
     */
    @Column(name = "access_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date accessTime;

    /**
     * Referer
     */
    @Column(name = "referer", length = 500)
    private String referer;

    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * 请求参数（JSON格式）
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    /**
     * 错误信息（如果有）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}
