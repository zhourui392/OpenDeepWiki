package ai.opendw.koalawiki.app.ai;

import java.util.List;
import java.util.Map;

/**
 * AI服务接口
 * 提供高级AI功能，封装底层AI客户端调用
 */
public interface IAIService {

    /**
     * 生成README文档
     *
     * @param context 仓库上下文信息
     * @return 生成的README内容（Markdown格式）
     */
    String generateReadme(ReadmeContext context);

    /**
     * 优化文档目录
     * 智能过滤不重要的文件，保留核心文档结构
     *
     * @param catalogData 原始目录数据
     * @param maxFiles 目标文件数量
     * @return 优化后的目录结构
     */
    String optimizeCatalog(String catalogData, int maxFiles);

    /**
     * 生成文档摘要
     *
     * @param content 文档内容
     * @param maxLength 摘要最大长度（字符）
     * @return 文档摘要
     */
    String summarizeDocument(String content, int maxLength);

    /**
     * 回答问题（基于文档内容）
     *
     * @param question 问题
     * @param documentContext 文档上下文
     * @return 答案
     */
    String answerQuestion(String question, String documentContext);

    /**
     * 生成文档标签
     *
     * @param content 文档内容
     * @param maxTags 最多生成标签数
     * @return 标签列表
     */
    List<String> generateTags(String content, int maxTags);

    /**
     * 分析仓库技术栈
     *
     * @param fileList 文件列表
     * @param readmeContent README内容（可选）
     * @return 技术栈列表
     */
    List<String> analyzeTechStack(List<String> fileList, String readmeContent);

    /**
     * 生成项目描述
     *
     * @param context 仓库上下文
     * @return 项目描述
     */
    String generateProjectDescription(Map<String, Object> context);
}
