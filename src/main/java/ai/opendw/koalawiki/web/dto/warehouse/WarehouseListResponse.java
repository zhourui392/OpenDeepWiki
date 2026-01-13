package ai.opendw.koalawiki.web.dto.warehouse;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 仓库列表响应
 * 前端期望的格式,包含items和total字段
 *
 * @author zhourui(V33215020)
 * @since 2025/11/15
 */
@Data
@Builder
public class WarehouseListResponse {
    /**
     * 仓库列表
     */
    private List<WarehouseResponse> items;

    /**
     * 总数
     */
    private Integer total;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPages;
}
