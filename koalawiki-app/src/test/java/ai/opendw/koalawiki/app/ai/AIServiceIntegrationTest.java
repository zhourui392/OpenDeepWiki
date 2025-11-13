package ai.opendw.koalawiki.app.ai;

import ai.opendw.koalawiki.infra.ai.IAIClient;
import ai.opendw.koalawiki.infra.ai.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AI服务集成测试
 *
 * @author OpenDeepWiki Team
 * @date 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
public class AIServiceIntegrationTest {

    @Mock
    private IAIClient aiClient;

    @Mock
    private PromptTemplateService promptTemplateService;

    @InjectMocks
    private AIServiceImpl aiService;

    private ReadmeContext mockContext;

    @BeforeEach
    public void setUp() {
        // 准备测试上下文
        mockContext = new ReadmeContext();
        mockContext.setRepositoryName("test-repo");
        mockContext.setOwner("testuser");
        mockContext.setDescription("A test repository");
        mockContext.setLanguage("Java");
        mockContext.setFileStructure("src/\n  main/\n    java/\n  test/\n    java/");

        List<String> techStack = new ArrayList<>();
        techStack.add("Java");
        techStack.add("Spring Boot");
        techStack.add("Maven");
        mockContext.setTechStack(techStack);
    }

    /**
     * 测试：生成README - 成功场景
     */
    @Test
    public void testGenerateReadme_Success() {
        // Given
        String mockPrompt = "Generate README for test-repo";
        String mockReadme = "# test-repo\n\nA test repository\n\n## Features\n- Feature 1\n- Feature 2";

        when(promptTemplateService.getReadmePrompt(any(ReadmeContext.class)))
            .thenReturn(mockPrompt);
        when(aiClient.chat(anyList(), anyString()))
            .thenReturn(mockReadme);

        // When
        String result = aiService.generateReadme(mockContext);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("# test-repo"));
        verify(promptTemplateService, times(1)).getReadmePrompt(any());
        verify(aiClient, times(1)).chat(anyList(), anyString());
    }

    /**
     * 测试：生成README - AI调用失败,使用降级策略
     */
    @Test
    public void testGenerateReadme_FallbackOnAIFailure() {
        // Given
        when(promptTemplateService.getReadmePrompt(any(ReadmeContext.class)))
            .thenReturn("Generate README");
        when(aiClient.chat(anyList(), anyString()))
            .thenThrow(new RuntimeException("AI service unavailable"));

        // When
        String result = aiService.generateReadme(mockContext);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("test-repo"));
        // 应该生成基本的README,即使AI失败
    }

    /**
     * 测试：优化目录 - 成功场景
     */
    @Test
    public void testOptimizeCatalog_Success() {
        // Given
        String catalogData = "{\n" +
            "  \"files\": [\n" +
            "    \"README.md\",\n" +
            "    \"src/main/java/App.java\",\n" +
            "    \"target/classes/App.class\",\n" +
            "    \"node_modules/package/index.js\"\n" +
            "  ]\n" +
            "}";
        int maxFiles = 50;

        String mockOptimized = "{\n" +
            "  \"files\": [\n" +
            "    \"README.md\",\n" +
            "    \"src/main/java/App.java\"\n" +
            "  ]\n" +
            "}";

        when(promptTemplateService.getCatalogOptimizationPrompt(anyString(), anyInt()))
            .thenReturn("Optimize catalog");
        when(aiClient.chat(anyList(), anyString()))
            .thenReturn(mockOptimized);

        // When
        String result = aiService.optimizeCatalog(catalogData, maxFiles);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.contains("target/"));
        assertFalse(result.contains("node_modules/"));
    }

    /**
     * 测试：生成文档摘要 - 成功场景
     */
    @Test
    public void testSummarizeDocument_Success() {
        // Given
        String content = "# API Documentation\n\n" +
            "This document describes the REST API endpoints.\n\n" +
            "## Authentication\n" +
            "Use Bearer token for authentication.\n\n" +
            "## Endpoints\n" +
            "### GET /api/users\n" +
            "Returns list of users.";
        int maxLength = 200;

        String mockSummary = "This document describes REST API endpoints including authentication using Bearer tokens and user management endpoints.";

        when(promptTemplateService.getSummaryPrompt(anyString(), anyInt()))
            .thenReturn("Summarize document");
        when(aiClient.chat(anyList(), anyString()))
            .thenReturn(mockSummary);

        // When
        String result = aiService.summarizeDocument(content, maxLength);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.length() <= maxLength);
    }

    /**
     * 测试：生成文档摘要 - 降级到截取策略
     */
    @Test
    public void testSummarizeDocument_FallbackToTruncate() {
        // Given
        String content = "A very long document content that needs to be summarized...";
        int maxLength = 50;

        when(promptTemplateService.getSummaryPrompt(anyString(), anyInt()))
            .thenReturn("Summarize");
        when(aiClient.chat(anyList(), anyString()))
            .thenThrow(new RuntimeException("AI unavailable"));

        // When
        String result = aiService.summarizeDocument(content, maxLength);

        // Then
        assertNotNull(result);
        assertTrue(result.length() <= maxLength);
    }

    /**
     * 测试：问答功能 - 成功场景
     */
    @Test
    public void testAskQuestion_Success() {
        // Given
        String question = "How do I install this project?";
        String context = "# Installation\n\nRun `npm install` to install dependencies.";

        String mockAnswer = "To install this project, run the command `npm install` to install all required dependencies.";

        when(promptTemplateService.getQAPrompt(anyString(), anyString()))
            .thenReturn("Answer question");
        when(aiClient.chat(anyList(), anyString()))
            .thenReturn(mockAnswer);

        // When
        String result = aiService.answerQuestion(question, context);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.toLowerCase().contains("install"));
    }

    /**
     * 测试：生成标签 - 成功场景
     */
    @Test
    public void testGenerateTags_Success() {
        // Given
        String content = "This is a Spring Boot REST API project with MySQL database and JWT authentication.";
        int maxTags = 5;

        String mockResponse = "Spring Boot, REST API, MySQL, JWT, Authentication";

        when(promptTemplateService.getTagGenerationPrompt(anyString(), anyInt()))
            .thenReturn("Generate tags");
        when(aiClient.chat(anyList(), anyString()))
            .thenReturn(mockResponse);

        // When
        List<String> result = aiService.generateTags(content, maxTags);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() <= maxTags);
        assertTrue(result.contains("Spring Boot"));
    }

    /**
     * 测试：识别技术栈 - 成功场景
     */
    @Test
    public void testIdentifyTechStack_Success() {
        // Given
        List<String> fileList = Arrays.asList(
            "pom.xml",
            "src/main/java/Application.java",
            "package.json",
            "Dockerfile"
        );

        String mockResponse = "Java, Spring Boot, Maven, Node.js, Docker";

        when(promptTemplateService.getTechStackPrompt(anyList()))
            .thenReturn("Identify tech stack");
        when(aiClient.chat(anyList(), anyString()))
            .thenReturn(mockResponse);

        // When
        List<String> result = aiService.identifyTechStack(fileList);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Java"));
        assertTrue(result.contains("Maven"));
    }

    /**
     * 测试：识别技术栈 - 降级到规则推断
     */
    @Test
    public void testIdentifyTechStack_FallbackToRules() {
        // Given
        List<String> fileList = Arrays.asList(
            "pom.xml",
            "package.json",
            "requirements.txt"
        );

        when(promptTemplateService.getTechStackPrompt(anyList()))
            .thenReturn("Identify tech stack");
        when(aiClient.chat(anyList(), anyString()))
            .thenThrow(new RuntimeException("AI unavailable"));

        // When
        List<String> result = aiService.identifyTechStack(fileList);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 应该通过文件名规则推断出技术栈
        assertTrue(result.stream().anyMatch(tech ->
            tech.contains("Java") || tech.contains("Maven") ||
            tech.contains("JavaScript") || tech.contains("Python")));
    }

    /**
     * 测试：生成项目描述 - 成功场景
     */
    @Test
    public void testGenerateProjectDescription_Success() {
        // Given
        when(promptTemplateService.getProjectDescriptionPrompt(any(ReadmeContext.class)))
            .thenReturn("Generate description");
        when(aiClient.chat(anyList(), anyString()))
            .thenReturn("A modern web application built with Spring Boot and React.");

        // When
        String result = aiService.generateProjectDescription(mockContext);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.length() > 10);
    }

    /**
     * 测试：空内容处理
     */
    @Test
    public void testHandleEmptyContent() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            aiService.summarizeDocument("", 100);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            aiService.answerQuestion("", "context");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            aiService.generateTags("", 5);
        });
    }

    /**
     * 测试：空上下文处理
     */
    @Test
    public void testHandleNullContext() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            aiService.generateReadme(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            aiService.generateProjectDescription(null);
        });
    }
}
