package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.core.ai.context.AIContext;
import ai.opendw.koalawiki.core.ai.context.ContextCompressor;
import ai.opendw.koalawiki.core.ai.context.MicroserviceContextBuilder;
import ai.opendw.koalawiki.domain.cluster.ContextLevel;
import ai.opendw.koalawiki.web.dto.Result;
import ai.opendw.koalawiki.web.dto.ai.context.AIContextRequest;
import ai.opendw.koalawiki.web.dto.ai.context.AIContextResponse;
import ai.opendw.koalawiki.web.dto.ai.context.OnDemandContextRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;

/**
 * AI上下文控制器
 * 提供微服务AI上下文构建和查询接口
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai-context")
@RequiredArgsConstructor
public class AIContextController {

    private final MicroserviceContextBuilder contextBuilder;
    private final ContextCompressor contextCompressor;

    /**
     * 获取分层AI上下文
     *
     * @param clusterId       集群ID
     * @param targetServiceId 目标服务ID（可选）
     * @param level           上下文级别
     * @param maxSizeKb       最大大小（KB）
     * @param format          输出格式
     * @return AI上下文
     */
    @GetMapping("/clusters/{clusterId}")
    public Result<AIContextResponse> getLayeredContext(
            @PathVariable String clusterId,
            @RequestParam(required = false) String targetServiceId,
            @RequestParam(defaultValue = "L2") String level,
            @RequestParam(defaultValue = "50") Integer maxSizeKb,
            @RequestParam(defaultValue = "JSON") String format) {

        log.info("获取分层AI上下文: clusterId={}, targetServiceId={}, level={}",
                clusterId, targetServiceId, level);

        ContextLevel contextLevel = parseContextLevel(level);

        AIContext context = contextBuilder.buildLayeredContext(clusterId, targetServiceId, contextLevel);
        if (context == null) {
            return Result.error("集群不存在: " + clusterId);
        }

        boolean compressed = false;
        if (context.getEstimatedSize() > maxSizeKb * 1024) {
            context = contextCompressor.compress(context, maxSizeKb);
            compressed = true;
        }

        boolean includeMarkdown = "MARKDOWN".equalsIgnoreCase(format);
        AIContextResponse response = AIContextResponse.from(context, includeMarkdown, compressed);

        return Result.success(response);
    }

    /**
     * 获取全局服务索引（L1层）
     *
     * @param clusterId 集群ID
     * @return 全局索引
     */
    @GetMapping("/clusters/{clusterId}/global-index")
    public Result<Object> getGlobalIndex(@PathVariable String clusterId) {
        log.info("获取全局服务索引: clusterId={}", clusterId);

        AIContext context = contextBuilder.buildLayeredContext(clusterId, null, ContextLevel.L1);
        if (context == null || context.getGlobalIndex() == null) {
            return Result.error("集群不存在: " + clusterId);
        }

        return Result.success(context.getGlobalIndex());
    }

    /**
     * 获取领域摘要（L2层）
     *
     * @param clusterId 集群ID
     * @param domainId  领域ID
     * @return 领域摘要
     */
    @GetMapping("/clusters/{clusterId}/domains/{domainId}")
    public Result<Object> getDomainSummary(
            @PathVariable String clusterId,
            @PathVariable String domainId) {

        log.info("获取领域摘要: clusterId={}, domainId={}", clusterId, domainId);

        Object summary = contextBuilder.buildDomainSummary(domainId);
        if (summary == null) {
            return Result.error("领域不存在: " + domainId);
        }

        return Result.success(summary);
    }

    /**
     * 获取服务详情（L3层）
     *
     * @param serviceId 服务ID
     * @return 服务详情
     */
    @GetMapping("/services/{serviceId}")
    public Result<Object> getServiceDetail(@PathVariable String serviceId) {
        log.info("获取服务详情: serviceId={}", serviceId);

        Object detail = contextBuilder.buildServiceDetail(serviceId);
        if (detail == null) {
            return Result.error("服务不存在: " + serviceId);
        }

        return Result.success(detail);
    }

    /**
     * 按需构建上下文
     *
     * @param request 请求参数
     * @return AI上下文
     */
    @PostMapping("/on-demand")
    public Result<AIContextResponse> buildOnDemandContext(@Valid @RequestBody OnDemandContextRequest request) {
        log.info("按需构建上下文: clusterId={}, query={}",
                request.getClusterId(), request.getQuery());

        AIContext context = contextBuilder.buildOnDemandContext(
                request.getClusterId(),
                request.getQuery(),
                request.getHints() != null ? request.getHints() : Collections.emptyList()
        );

        if (context == null) {
            return Result.error("无法构建上下文");
        }

        boolean compressed = false;
        Integer maxSizeKb = request.getMaxSizeKb();
        if (maxSizeKb != null && context.getEstimatedSize() > maxSizeKb * 1024) {
            context = contextCompressor.compress(context, maxSizeKb);
            compressed = true;
        }

        AIContextResponse response = AIContextResponse.from(context, true, compressed);
        return Result.success(response);
    }

    /**
     * 获取Markdown格式上下文
     *
     * @param clusterId       集群ID
     * @param targetServiceId 目标服务ID
     * @param level           上下文级别
     * @return Markdown文本
     */
    @GetMapping(value = "/clusters/{clusterId}/markdown", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getMarkdownContext(
            @PathVariable String clusterId,
            @RequestParam(required = false) String targetServiceId,
            @RequestParam(defaultValue = "L2") String level) {

        ContextLevel contextLevel = parseContextLevel(level);
        AIContext context = contextBuilder.buildLayeredContext(clusterId, targetServiceId, contextLevel);

        if (context == null) {
            return "# 错误\n\n集群不存在: " + clusterId;
        }

        return context.toMarkdown();
    }

    /**
     * 获取接口详情
     *
     * @param clusterId     集群ID
     * @param interfaceName 接口名
     * @return 接口详情
     */
    @GetMapping("/clusters/{clusterId}/interfaces")
    public Result<Object> getInterfaceDetail(
            @PathVariable String clusterId,
            @RequestParam String interfaceName) {

        log.info("获取接口详情: clusterId={}, interfaceName={}", clusterId, interfaceName);

        Object detail = contextBuilder.buildInterfaceDetail(clusterId, interfaceName);
        if (detail == null) {
            return Result.error("接口不存在: " + interfaceName);
        }

        return Result.success(detail);
    }

    /**
     * 分析查询意图
     *
     * @param query 查询文本
     * @return 查询意图
     */
    @GetMapping("/analyze-intent")
    public Result<Object> analyzeIntent(@RequestParam String query) {
        return Result.success(contextBuilder.analyzeQueryIntent(query));
    }

    /**
     * 解析上下文级别
     *
     * @param level 级别字符串
     * @return 上下文级别枚举
     */
    private ContextLevel parseContextLevel(String level) {
        if (level == null || level.isEmpty()) {
            return ContextLevel.L2;
        }

        try {
            return ContextLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无效的上下文级别: {}, 使用默认值L2", level);
            return ContextLevel.L2;
        }
    }
}
