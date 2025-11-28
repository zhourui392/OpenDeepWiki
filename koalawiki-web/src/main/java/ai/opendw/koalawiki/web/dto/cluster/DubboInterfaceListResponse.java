package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Dubbo接口列表响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class DubboInterfaceListResponse {

    /**
     * 接口列表
     */
    private List<DubboInterfaceResponse> items;

    /**
     * 总数
     */
    private long total;

    /**
     * 当前页
     */
    private int page;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int totalPages;
}
