package ai.opendw.koalawiki.domain.log;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 访问日志实体
 * 记录用户访问行为
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AccessLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（如果已登录）
     */
    private String userId;

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 访问动作
     * VIEW - 查看
     * DOWNLOAD - 下载
     * SEARCH - 搜索
     * CREATE - 创建
     * UPDATE - 更新
     * DELETE - 删除
     */
    private String action;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * User Agent
     */
    private String userAgent;

    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 请求方法 (GET, POST, PUT, DELETE)
     */
    private String requestMethod;

    /**
     * 响应时间（毫秒）
     */
    private Integer responseTime;

    /**
     * HTTP状态码
     */
    private Integer statusCode;

    /**
     * 访问时间
     */
    private Date accessTime;

    /**
     * Referer
     */
    private String referer;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 请求参数（JSON格式）
     */
    private String requestParams;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
}
