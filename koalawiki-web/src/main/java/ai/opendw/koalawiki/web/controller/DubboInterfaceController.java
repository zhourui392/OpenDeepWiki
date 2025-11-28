package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.core.analysis.dubbo.DubboCallChainAnalyzer;
import ai.opendw.koalawiki.core.cluster.DubboInterfaceService;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceConsumer;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceRegistry;
import ai.opendw.koalawiki.domain.dubbo.DubboMethodInfo;
import ai.opendw.koalawiki.domain.dubbo.MethodParameter;
import ai.opendw.koalawiki.web.dto.Result;
import ai.opendw.koalawiki.web.dto.cluster.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dubbo接口管理控制器
 * 提供Dubbo接口查询、搜索、消费者分析相关的REST API
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/clusters/{clusterId}/dubbo-interfaces")
@RequiredArgsConstructor
@Validated
public class DubboInterfaceController {

    private final DubboInterfaceService dubboInterfaceService;
    private final DubboCallChainAnalyzer callChainAnalyzer;

    /**
     * 获取集群接口列表
     *
     * @param clusterId 集群ID
     * @param page      页码
     * @param pageSize  每页大小
     * @return 接口分页列表
     */
    @GetMapping
    public ResponseEntity<Result<DubboInterfaceListResponse>> listInterfaces(
            @PathVariable String clusterId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize) {

        log.debug("获取集群Dubbo接口列表: clusterId={}, page={}, pageSize={}", clusterId, page, pageSize);

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<DubboInterfaceRegistry> interfaces = dubboInterfaceService.listByCluster(clusterId, pageable);

            List<DubboInterfaceResponse> items = interfaces.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            DubboInterfaceListResponse response = DubboInterfaceListResponse.builder()
                    .items(items)
                    .total(interfaces.getTotalElements())
                    .page(page)
                    .pageSize(pageSize)
                    .totalPages(interfaces.getTotalPages())
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取Dubbo接口列表失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索接口
     *
     * @param clusterId 集群ID
     * @param keyword   关键词
     * @param version   版本过滤
     * @param page      页码
     * @param pageSize  每页大小
     * @return 接口分页列表
     */
    @GetMapping("/search")
    public ResponseEntity<Result<DubboInterfaceListResponse>> searchInterfaces(
            @PathVariable String clusterId,
            @RequestParam String keyword,
            @RequestParam(required = false) String version,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize) {

        log.debug("搜索Dubbo接口: clusterId={}, keyword={}, version={}", clusterId, keyword, version);

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<DubboInterfaceRegistry> interfaces = dubboInterfaceService.search(clusterId, keyword, pageable);

            List<DubboInterfaceResponse> items = interfaces.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            DubboInterfaceListResponse response = DubboInterfaceListResponse.builder()
                    .items(items)
                    .total(interfaces.getTotalElements())
                    .page(page)
                    .pageSize(pageSize)
                    .totalPages(interfaces.getTotalPages())
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("搜索Dubbo接口失败: clusterId={}, keyword={}", clusterId, keyword, e);
            return ResponseEntity.ok(Result.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 获取接口详情
     *
     * @param clusterId   集群ID
     * @param interfaceId 接口ID
     * @return 接口详情
     */
    @GetMapping("/{interfaceId}")
    public ResponseEntity<Result<DubboInterfaceResponse>> getInterface(
            @PathVariable String clusterId,
            @PathVariable String interfaceId) {

        log.debug("获取Dubbo接口详情: clusterId={}, interfaceId={}", clusterId, interfaceId);

        try {
            return dubboInterfaceService.getById(interfaceId)
                    .map(registry -> ResponseEntity.ok(Result.success(convertToDetailResponse(registry))))
                    .orElse(ResponseEntity.ok(Result.error(404, "接口不存在")));

        } catch (Exception e) {
            log.error("获取Dubbo接口详情失败: interfaceId={}", interfaceId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取接口消费者列表
     *
     * @param clusterId   集群ID
     * @param interfaceId 接口ID
     * @return 消费者列表
     */
    @GetMapping("/{interfaceId}/consumers")
    public ResponseEntity<Result<List<DubboConsumerResponse>>> getConsumers(
            @PathVariable String clusterId,
            @PathVariable String interfaceId) {

        log.debug("获取Dubbo接口消费者: clusterId={}, interfaceId={}", clusterId, interfaceId);

        try {
            List<DubboInterfaceConsumer> consumers = dubboInterfaceService.getConsumers(interfaceId);

            List<DubboConsumerResponse> response = consumers.stream()
                    .map(this::convertToConsumerResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取Dubbo接口消费者失败: interfaceId={}", interfaceId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据仓库获取提供的接口列表
     *
     * @param clusterId   集群ID
     * @param warehouseId 仓库ID
     * @return 接口列表
     */
    @GetMapping("/by-service/{warehouseId}")
    public ResponseEntity<Result<List<DubboInterfaceResponse>>> getInterfacesByService(
            @PathVariable String clusterId,
            @PathVariable String warehouseId) {

        log.debug("根据仓库获取Dubbo接口: clusterId={}, warehouseId={}", clusterId, warehouseId);

        try {
            List<DubboInterfaceRegistry> interfaces = dubboInterfaceService.listByProvider(warehouseId);

            List<DubboInterfaceResponse> response = interfaces.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("根据仓库获取Dubbo接口失败: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热门接口
     *
     * @param clusterId 集群ID
     * @param limit     数量限制
     * @return 接口列表
     */
    @GetMapping("/top")
    public ResponseEntity<Result<List<DubboInterfaceResponse>>> getTopInterfaces(
            @PathVariable String clusterId,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {

        log.debug("获取热门Dubbo接口: clusterId={}, limit={}", clusterId, limit);

        try {
            List<DubboInterfaceRegistry> interfaces = dubboInterfaceService.getTopInterfaces(clusterId, limit);

            List<DubboInterfaceResponse> response = interfaces.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取热门Dubbo接口失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取仓库消费的接口列表
     *
     * @param clusterId   集群ID
     * @param warehouseId 仓库ID
     * @return 接口列表
     */
    @GetMapping("/consumed-by/{warehouseId}")
    public ResponseEntity<Result<List<DubboInterfaceResponse>>> getConsumedInterfaces(
            @PathVariable String clusterId,
            @PathVariable String warehouseId) {

        log.debug("获取仓库消费的Dubbo接口: clusterId={}, warehouseId={}", clusterId, warehouseId);

        try {
            List<DubboInterfaceRegistry> interfaces = dubboInterfaceService.getConsumedInterfaces(warehouseId);

            List<DubboInterfaceResponse> response = interfaces.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取仓库消费的Dubbo接口失败: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 刷新集群接口注册表
     *
     * @param clusterId 集群ID
     * @return 操作结果
     */
    @PostMapping("/refresh")
    public ResponseEntity<Result<Void>> refreshInterfaces(@PathVariable String clusterId) {

        log.info("刷新集群Dubbo接口注册表: clusterId={}", clusterId);

        try {
            dubboInterfaceService.refreshClusterInterfaces(clusterId);
            return ResponseEntity.ok(Result.success(null, "接口刷新任务已启动"));

        } catch (Exception e) {
            log.error("刷新Dubbo接口注册表失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("刷新失败: " + e.getMessage()));
        }
    }

    /**
     * 获取接口统计信息
     *
     * @param clusterId 集群ID
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Result<InterfaceStatisticsResponse>> getStatistics(@PathVariable String clusterId) {

        log.debug("获取Dubbo接口统计: clusterId={}", clusterId);

        try {
            long totalCount = dubboInterfaceService.countByCluster(clusterId);

            InterfaceStatisticsResponse response = InterfaceStatisticsResponse.builder()
                    .clusterId(clusterId)
                    .totalCount(totalCount)
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取Dubbo接口统计失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取接口调用链
     *
     * @param clusterId   集群ID
     * @param interfaceId 接口ID
     * @param maxDepth    最大深度
     * @return 调用链结果
     */
    @GetMapping("/{interfaceId}/call-chain")
    public ResponseEntity<Result<CallChainResponse>> getCallChain(
            @PathVariable String clusterId,
            @PathVariable String interfaceId,
            @RequestParam(defaultValue = "3") @Min(1) int maxDepth) {

        log.debug("获取Dubbo接口调用链: clusterId={}, interfaceId={}, maxDepth={}", clusterId, interfaceId, maxDepth);

        try {
            DubboCallChainAnalyzer.CallChainResult result = callChainAnalyzer.analyzeCallChain(interfaceId, maxDepth);

            CallChainResponse response = CallChainResponse.builder()
                    .interfaceId(result.getInterfaceId())
                    .interfaceName(result.getInterfaceName())
                    .providerServiceName(result.getProviderServiceName())
                    .upstreamCount(result.getUpstreamCount())
                    .downstreamCount(result.getDownstreamCount())
                    .mermaidCode(result.getMermaidCode())
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取Dubbo接口调用链失败: interfaceId={}", interfaceId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 转换为响应DTO
     */
    private DubboInterfaceResponse convertToResponse(DubboInterfaceRegistry registry) {
        return DubboInterfaceResponse.builder()
                .id(registry.getId())
                .interfaceName(registry.getInterfaceName())
                .simpleName(registry.getSimpleName())
                .version(registry.getVersion())
                .groupName(registry.getGroupName())
                .providerWarehouseId(registry.getProviderWarehouseId())
                .providerServiceName(registry.getProviderServiceName())
                .description(registry.getDescription())
                .deprecated(registry.getDeprecated())
                .methodCount(registry.getMethodCount())
                .consumerCount(registry.getConsumerCount())
                .createdAt(registry.getCreatedAt())
                .updatedAt(registry.getUpdatedAt())
                .build();
    }

    /**
     * 转换为详情响应DTO（包含方法信息）
     */
    private DubboInterfaceResponse convertToDetailResponse(DubboInterfaceRegistry registry) {
        List<DubboMethodResponse> methods = null;
        if (registry.getMethods() != null) {
            methods = registry.getMethods().stream()
                    .map(this::convertToMethodResponse)
                    .collect(Collectors.toList());
        }

        return DubboInterfaceResponse.builder()
                .id(registry.getId())
                .interfaceName(registry.getInterfaceName())
                .simpleName(registry.getSimpleName())
                .version(registry.getVersion())
                .groupName(registry.getGroupName())
                .providerWarehouseId(registry.getProviderWarehouseId())
                .providerServiceName(registry.getProviderServiceName())
                .description(registry.getDescription())
                .methods(methods)
                .deprecated(registry.getDeprecated())
                .deprecatedReason(registry.getDeprecatedReason())
                .sourceFile(registry.getSourceFile())
                .methodCount(registry.getMethodCount())
                .consumerCount(registry.getConsumerCount())
                .consumerServiceIds(registry.getConsumerServiceIds())
                .createdAt(registry.getCreatedAt())
                .updatedAt(registry.getUpdatedAt())
                .build();
    }

    /**
     * 转换方法信息
     */
    private DubboMethodResponse convertToMethodResponse(DubboMethodInfo method) {
        List<MethodParameterResponse> parameters = null;
        if (method.getParameters() != null) {
            parameters = method.getParameters().stream()
                    .map(this::convertToParameterResponse)
                    .collect(Collectors.toList());
        }

        return DubboMethodResponse.builder()
                .name(method.getName())
                .returnType(method.getReturnType())
                .parameters(parameters)
                .description(method.getDescription())
                .signature(method.getSignature())
                .deprecated(method.getDeprecated())
                .exceptions(method.getExceptions())
                .build();
    }

    /**
     * 转换参数信息
     */
    private MethodParameterResponse convertToParameterResponse(MethodParameter param) {
        return MethodParameterResponse.builder()
                .name(param.getName())
                .type(param.getType())
                .description(param.getDescription())
                .required(param.getRequired())
                .index(param.getIndex())
                .build();
    }

    /**
     * 转换消费者信息
     */
    private DubboConsumerResponse convertToConsumerResponse(DubboInterfaceConsumer consumer) {
        return DubboConsumerResponse.builder()
                .id(consumer.getId())
                .consumerWarehouseId(consumer.getConsumerWarehouseId())
                .consumerServiceName(consumer.getConsumerServiceName())
                .sourceClass(consumer.getSourceClass())
                .sourceField(consumer.getSourceField())
                .createdAt(consumer.getCreatedAt())
                .build();
    }

    /**
     * 接口统计响应
     */
    @lombok.Builder
    @lombok.Data
    public static class InterfaceStatisticsResponse {
        private String clusterId;
        private long totalCount;
    }
}
