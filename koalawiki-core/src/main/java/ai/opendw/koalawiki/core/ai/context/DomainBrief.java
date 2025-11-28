package ai.opendw.koalawiki.core.ai.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 领域简要信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainBrief {

    /**
     * 领域ID
     */
    private String id;

    /**
     * 领域名称
     */
    private String name;

    /**
     * 领域编码
     */
    private String code;

    /**
     * 服务数量
     */
    private int serviceCount;

    /**
     * 接口数量
     */
    private int interfaceCount;

    /**
     * 便捷构造方法
     *
     * @param name         领域名称
     * @param code         领域编码
     * @param serviceCount 服务数量
     */
    public DomainBrief(String name, String code, int serviceCount) {
        this.name = name;
        this.code = code;
        this.serviceCount = serviceCount;
    }
}
