package ai.opendw.koalawiki.web.dto.warehouse;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 触发同步请求DTO
 */
@Data
public class TriggerSyncRequest {

    /**
     * 仓库ID
     */
    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    /**
     * 是否强制同步
     * true: 即使正在同步也强制开始新的同步
     * false: 如果正在同步则返回当前同步状态
     */
    private boolean forceSync = false;

    /**
     * 同步模式
     * full: 全量同步
     * incremental: 增量同步
     */
    private String syncMode = "incremental";

    /**
     * 同步分支
     * 默认为主分支
     */
    private String branch;

    /**
     * 同步深度
     * 0表示同步全部历史
     */
    private Integer depth = 0;

    /**
     * 是否同步子模块
     */
    private boolean syncSubmodules = false;

    /**
     * 备注信息
     */
    private String remark;
}