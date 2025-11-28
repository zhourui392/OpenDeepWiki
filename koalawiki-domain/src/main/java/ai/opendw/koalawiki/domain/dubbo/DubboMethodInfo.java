package ai.opendw.koalawiki.domain.dubbo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dubbo方法信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class DubboMethodInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 方法名
     */
    @NotBlank(message = "方法名不能为空")
    @Size(max = 128, message = "方法名长度不能超过128字符")
    private String name;

    /**
     * 返回类型
     */
    @Size(max = 255, message = "返回类型长度不能超过255字符")
    private String returnType;

    /**
     * 参数列表
     */
    private List<MethodParameter> parameters = new ArrayList<>();

    /**
     * 方法描述
     */
    private String description;

    /**
     * 是否已废弃
     */
    private Boolean deprecated = false;

    /**
     * 异常列表
     */
    private List<String> exceptions = new ArrayList<>();

    /**
     * 添加参数
     *
     * @param parameter 参数
     */
    public void addParameter(MethodParameter parameter) {
        if (parameter != null) {
            parameters.add(parameter);
        }
    }

    /**
     * 获取方法签名
     *
     * @return 方法签名
     */
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType != null ? returnType : "void");
        sb.append(" ");
        sb.append(name);
        sb.append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            MethodParameter param = parameters.get(i);
            sb.append(param.getType());
            sb.append(" ");
            sb.append(param.getName());
        }
        sb.append(")");
        return sb.toString();
    }
}
