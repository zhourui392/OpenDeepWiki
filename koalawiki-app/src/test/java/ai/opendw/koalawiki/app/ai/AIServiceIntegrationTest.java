package ai.opendw.koalawiki.app.ai;

import ai.opendw.koalawiki.infra.ai.IAIClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * AI服务集成测试（对齐当前 AIServiceImpl / PromptTemplateService 实现）
 */
@ExtendWith(MockitoExtension.class)
public class AIServiceIntegrationTest {

    @Mock
    private IAIClient aiClient;

    @Mock
    private PromptTemplateService promptTemplateService;

    @InjectMocks
    private AIServiceImpl aiService;

    private ReadmeContext context;

    @BeforeEach
    public void setUp() {
        context = new ReadmeContext();
        context.setRepositoryName("test-repo");
        context.setOwner("test-user");
        context.setDescription("Test repository");
        context.setLanguage("Java");
        context.setTechStack(Collections.singletonList("Java"));
    }

    @Test
    public void testGenerateReadme_UseTemplateAndAIClient() {
        when(promptTemplateService.getReadmePrompt(any(ReadmeContext.class)))
                .thenReturn("READMe PROMPT");

        when(aiClient.complete(anyString()))
                .thenReturn("# test-repo\n\nREADME content");

        String result = aiService.generateReadme(context);

        assertNotNull(result);
        assertTrue(result.contains("test-repo"));
        verify(promptTemplateService, times(1)).getReadmePrompt(any(ReadmeContext.class));
        verify(aiClient, times(1)).complete(anyString());
    }

    @Test
    public void testGenerateReadme_FallbackWhenAIError() {
        when(promptTemplateService.getReadmePrompt(any(ReadmeContext.class)))
                .thenReturn("READMe PROMPT");

        when(aiClient.complete(anyString()))
                .thenThrow(new RuntimeException("AI error"));

        assertThrows(RuntimeException.class, () -> aiService.generateReadme(context));
    }
}
