package ai.opendw.koalawiki.domain.dubbo;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 方法参数
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class MethodParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 参数名
     */
    @Size(max = 64, message = "参数名长度不能超过64字符")
    private String name;

    /**
     * 参数类型
     */
    @Size(max = 255, message = "参数类型长度不能超过255字符")
    private String type;

    /**
     * 参数描述
     */
    private String description;

    /**
     * 是否必填
     */
    private Boolean required = true;

    /**
     * 参数索引
     */
    private Integer index;

    /**
     * 默认构造
     */
    public MethodParameter() {
    }

    /**
     * 便捷构造
     *
     * @param name 参数名
     * @param type 参数类型
     */
    public MethodParameter(String name, String type) {
        this.name = name;
        this.type = type;
    }

    /**
     * 完整构造
     *
     * @param name        参数名
     * @param type        参数类型
     * @param description 描述
     * @param required    是否必填
     */
    public MethodParameter(String name, String type, String description, Boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }
}
