package ai.opendw.koalawiki.app.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * README生成上下文
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadmeContext {

    /**
     * 仓库名称
     */
    private String repositoryName;

    /**
     * 仓库所有者
     */
    private String owner;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * 主要编程语言
     */
    private String primaryLanguage;

    /**
     * 技术栈列表
     */
    private List<String> techStack;

    /**
     * 文件列表（主要文件）
     */
    private List<String> files;

    /**
     * 目录结构
     */
    private String directoryStructure;

    /**
     * 现有README内容（如果存在）
     */
    private String existingReadme;

    /**
     * 额外的元数据
     */
    private Map<String, Object> metadata;

    /**
     * 目标语言（中文、英文等）
     */
    private String language;
}
