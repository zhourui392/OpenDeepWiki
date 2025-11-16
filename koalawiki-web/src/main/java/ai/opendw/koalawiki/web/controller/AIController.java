package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.ai.IAIService;
import ai.opendw.koalawiki.app.ai.ReadmeContext;
import ai.opendw.koalawiki.app.ai.generators.ReadmeGenerator;
import ai.opendw.koalawiki.web.dto.Result;
import ai.opendw.koalawiki.web.dto.ai.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * AI功能控制器
 * 提供AI增强功能的REST API
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Validated
public class AIController {

    private final IAIService aiService;
    private final ReadmeGenerator readmeGenerator;

    /**
     * 生成README文档
     *
     * @param request README生成请求
     * @return README内容
     */
    @PostMapping("/readme/generate")
    public ResponseEntity<Result<ReadmeResponse>> generateReadme(
            @Valid @RequestBody GenerateReadmeRequest request) {

        log.info("生成README: warehouseId={}, language={}",
                request.getWarehouseId(), request.getLanguage());

        long startTime = System.currentTimeMillis();

        try {
            // 构建上下文
            ReadmeContext context = buildReadmeContext(request);

            // 生成README
            String readmeContent = aiService.generateReadme(context);
            long duration = System.currentTimeMillis() - startTime;

            // 构建响应
            ReadmeResponse response = ReadmeResponse.builder()
                    .content(readmeContent)
                    .generationMethod(readmeContent.contains("AI生成") ? "AI" : "FALLBACK")
                    .detectedLanguage(request.getLanguage() != null ? request.getLanguage() : "auto")
                    .duration(duration)
                    .message("README生成成功")
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("生成README失败: warehouseId={}", request.getWarehouseId(), e);
            return ResponseEntity.ok(Result.error("生成README失败: " + e.getMessage()));
        }
    }

    /**
     * 生成简单README
     *
     * @param warehouseId 仓库ID
     * @param name 仓库名称
     * @param description 描述
     * @return README内容
     */
    @GetMapping("/readme/generate-simple")
    public ResponseEntity<Result<ReadmeResponse>> generateSimpleReadme(
            @RequestParam String warehouseId,
            @RequestParam String name,
            @RequestParam(required = false) String description) {

        log.info("生成简单README: warehouseId={}, name={}", warehouseId, name);

        long startTime = System.currentTimeMillis();

        try {
            String readmeContent = readmeGenerator.generateSimple(name, "Unknown", description);
            long duration = System.currentTimeMillis() - startTime;

            ReadmeResponse response = ReadmeResponse.builder()
                    .content(readmeContent)
                    .generationMethod("SIMPLE")
                    .detectedLanguage("auto")
                    .duration(duration)
                    .message("简单README生成成功")
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("生成简单README失败: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("生成简单README失败: " + e.getMessage()));
        }
    }

    /**
     * 问答接口
     *
     * @param request 问答请求
     * @return 答案
     */
    @PostMapping("/qa/ask")
    public ResponseEntity<Result<AnswerResponse>> askQuestion(
            @Valid @RequestBody AskQuestionRequest request) {

        log.info("AI问答: warehouseId={}, question={}",
                request.getWarehouseId(), request.getQuestion());

        long startTime = System.currentTimeMillis();

        try {
            // TODO: 根据warehouseId和documentIds获取相关文档内容
            String context = buildQuestionContext(request);

            // 调用AI服务回答问题
            String answer = aiService.answerQuestion(request.getQuestion(), context);
            long duration = System.currentTimeMillis() - startTime;

            // 构建响应
            AnswerResponse response = AnswerResponse.builder()
                    .answer(answer)
                    .confidence(0.85)  // 暂时固定值
                    .references(Collections.emptyList())  // TODO: 实现引用提取
                    .relatedQuestions(Collections.emptyList())  // TODO: 实现相关问题推荐
                    .sessionId(request.getSessionId() != null ?
                            request.getSessionId() : UUID.randomUUID().toString())
                    .duration(duration)
                    .message("回答生成成功")
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("AI问答失败: question={}", request.getQuestion(), e);
            return ResponseEntity.ok(Result.error("AI问答失败: " + e.getMessage()));
        }
    }

    /**
     * 生成标签
     *
     * @param content 内容
     * @param maxTags 最大标签数
     * @return 标签列表
     */
    @PostMapping("/tags/generate")
    public ResponseEntity<Result<List<String>>> generateTags(
            @RequestBody String content,
            @RequestParam(defaultValue = "5") Integer maxTags) {

        log.info("生成标签: contentLength={}, maxTags={}", content.length(), maxTags);

        try {
            List<String> tags = aiService.generateTags(content, maxTags);
            return ResponseEntity.ok(Result.success(tags, "成功生成 " + tags.size() + " 个标签"));

        } catch (Exception e) {
            log.error("生成标签失败", e);
            return ResponseEntity.ok(Result.error("生成标签失败: " + e.getMessage()));
        }
    }

    /**
     * 分析技术栈
     *
     * @param fileList 文件列表
     * @return 技术栈列表
     */
    @PostMapping("/tech-stack/analyze")
    public ResponseEntity<Result<List<String>>> analyzeTechStack(
            @RequestBody List<String> fileList) {

        log.info("分析技术栈: fileCount={}", fileList.size());

        try {
            List<String> techStack = aiService.analyzeTechStack(fileList, null);
            return ResponseEntity.ok(Result.success(techStack,
                    "检测到 " + techStack.size() + " 个技术栈"));

        } catch (Exception e) {
            log.error("分析技术栈失败", e);
            return ResponseEntity.ok(Result.error("分析技术栈失败: " + e.getMessage()));
        }
    }

    /**
     * 生成项目描述
     *
     * @param name 项目名称
     * @param techStack 技术栈
     * @return 项目描述
     */
    @GetMapping("/project/describe")
    public ResponseEntity<Result<String>> describeProject(
            @RequestParam String name,
            @RequestParam(required = false) String techStack) {

        log.info("生成项目描述: name={}", name);

        try {
            // 构建上下文Map
            java.util.Map<String, Object> context = new java.util.HashMap<>();
            context.put("repositoryName", name);
            context.put("techStack", techStack != null ? techStack : "");

            String description = aiService.generateProjectDescription(context);
            return ResponseEntity.ok(Result.success(description));

        } catch (Exception e) {
            log.error("生成项目描述失败: name={}", name, e);
            return ResponseEntity.ok(Result.error("生成项目描述失败: " + e.getMessage()));
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建README上下文
     */
    private ReadmeContext buildReadmeContext(GenerateReadmeRequest request) {
        // TODO: 从数据库获取仓库信息
        return ReadmeContext.builder()
                .repositoryName("Example Project")
                .owner("OpenDeepWiki")
                .description("A sample project")
                .techStack(java.util.Arrays.asList("Java", "Spring Boot"))
                .primaryLanguage("Java")
                .files(Collections.emptyList())
                .language(request.getLanguage())
                .build();
    }

    /**
     * 获取文档内容
     */
    private String getDocumentContent(String documentId) {
        // TODO: 从数据库获取文档内容
        log.warn("getDocumentContent未实现，使用模拟数据: {}", documentId);
        return "This is a sample document content for testing purposes. " +
               "In production, this should fetch real document content from the database.";
    }

    /**
     * 构建问答上下文
     */
    private String buildQuestionContext(AskQuestionRequest request) {
        // TODO: 根据warehouseId和documentIds获取相关文档
        log.warn("buildQuestionContext未实现，使用模拟上下文");
        return "Sample context for warehouse: " + request.getWarehouseId();
    }
}
