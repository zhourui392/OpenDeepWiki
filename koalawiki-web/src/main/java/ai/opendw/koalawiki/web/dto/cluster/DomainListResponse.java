package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 领域列表响应
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class DomainListResponse {

    /**
     * 领域列表
     */
    private List<DomainResponse> items;

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
