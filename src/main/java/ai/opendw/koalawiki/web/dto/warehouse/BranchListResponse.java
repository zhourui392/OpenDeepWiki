package ai.opendw.koalawiki.web.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Git分支列表响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchListResponse {

    /**
     * 分支列表
     */
    private List<String> branches;

    /**
     * 默认分支
     */
    private String defaultBranch;
}
