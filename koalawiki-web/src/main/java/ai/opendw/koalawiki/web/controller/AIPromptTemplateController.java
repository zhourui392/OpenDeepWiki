package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.infra.entity.AIPromptTemplateEntity;
import ai.opendw.koalawiki.infra.repository.AIPromptTemplateRepository;
import ai.opendw.koalawiki.web.dto.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI提示词模板管理控制器
 *
 * @author zhourui(V33215020)
 * @since 2025/11/18
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-prompt-template")
@RequiredArgsConstructor
@Validated
public class AIPromptTemplateController {

    private final AIPromptTemplateRepository templateRepository;

    @GetMapping("/list")
    public ResponseEntity<Result<List<TemplateResponse>>> list(
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) String agentType) {
        List<AIPromptTemplateEntity> templates = templateRepository.findAll();
        List<TemplateResponse> responses = new java.util.ArrayList<>();
        for (AIPromptTemplateEntity entity : templates) {
            if (docType != null && !docType.isEmpty() && entity.getPromptType() != null
                    && !entity.getPromptType().equals(docType)) {
                continue;
            }
            if (agentType != null && !agentType.isEmpty() && entity.getAgentType() != null
                    && !entity.getAgentType().equals("all") && !entity.getAgentType().equals(agentType)) {
                continue;
            }
            responses.add(toResponse(entity));
        }
        return ResponseEntity.ok(Result.success(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<TemplateResponse>> get(@PathVariable String id) {
        Optional<AIPromptTemplateEntity> template = templateRepository.findById(id);
        return template.map(t -> ResponseEntity.ok(Result.success(toResponse(t))))
                .orElse(ResponseEntity.ok(Result.error("模板不存在")));
    }

    @PostMapping
    @CacheEvict(value = "promptTemplates", allEntries = true)
    public ResponseEntity<Result<AIPromptTemplateEntity>> create(@Valid @RequestBody TemplateRequest request) {
        AIPromptTemplateEntity entity = new AIPromptTemplateEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setTemplateName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setTemplateContent(request.getTemplate());
        entity.setPromptType("custom");
        entity.setAgentType("all");

        entity = templateRepository.save(entity);
        return ResponseEntity.ok(Result.success(entity));
    }

    @PutMapping("/{id}")
    @CacheEvict(value = "promptTemplates", allEntries = true)
    public ResponseEntity<Result<AIPromptTemplateEntity>> update(
            @PathVariable String id,
            @Valid @RequestBody TemplateRequest request) {

        Optional<AIPromptTemplateEntity> opt = templateRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.ok(Result.error("模板不存在"));
        }

        AIPromptTemplateEntity entity = opt.get();
        entity.setTemplateName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setTemplateContent(request.getTemplate());

        entity = templateRepository.save(entity);
        return ResponseEntity.ok(Result.success(entity));
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "promptTemplates", allEntries = true)
    public ResponseEntity<Result<Void>> delete(@PathVariable String id) {
        templateRepository.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "删除成功"));
    }

    private TemplateResponse toResponse(AIPromptTemplateEntity entity) {
        TemplateResponse response = new TemplateResponse();
        response.setId(entity.getId());
        response.setName(entity.getTemplateName());
        response.setDescription(entity.getDescription());
        response.setTemplate(entity.getTemplateContent());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    @Data
    public static class TemplateRequest {
        private String name;
        private String description;
        private String template;
    }

    @Data
    public static class TemplateResponse {
        private String id;
        private String name;
        private String description;
        private String template;
        private java.util.Date createdAt;
        private java.util.Date updatedAt;
    }
}
