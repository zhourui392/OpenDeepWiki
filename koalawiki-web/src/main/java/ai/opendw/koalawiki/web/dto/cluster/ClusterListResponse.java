package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 集群列表响应
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class ClusterListResponse {

    /**
     * 集群列表
     */
    private List<ClusterResponse> items;

    /**
     * 总数
     */
    private long total;

    /**
     * 当前页码
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
