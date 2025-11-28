package ai.opendw.koalawiki.core.ai.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 上下文压缩器
 * 用于压缩AI上下文以满足大小限制
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@Component
public class ContextCompressor {

    private static final int DEFAULT_MAX_DESCRIPTION_LENGTH = 200;
    private static final int DEFAULT_MAX_SERVICES_PER_DOMAIN = 10;
    private static final int DEFAULT_MAX_INTERFACES_PER_SERVICE = 10;
    private static final int DEFAULT_MAX_METHODS_PER_INTERFACE = 5;
    private static final int DEFAULT_MAX_PARAMETERS = 3;

    /**
     * 压缩上下文到目标大小
     *
     * @param context   原始上下文
     * @param maxSizeKb 最大大小（KB）
     * @return 压缩后的上下文
     */
    public AIContext compress(AIContext context, int maxSizeKb) {
        int maxSizeChars = maxSizeKb * 1024;
        int currentSize = context.calculateEstimatedSize();

        if (currentSize <= maxSizeChars) {
            return context;
        }

        log.info("开始压缩上下文: 当前大小={}chars, 目标大小={}chars", currentSize, maxSizeChars);

        AIContext compressed = new AIContext();
        compressed.setClusterId(context.getClusterId());
        compressed.setTargetServiceId(context.getTargetServiceId());
        compressed.setContextLevel(context.getContextLevel());
        compressed.setGeneratedAt(context.getGeneratedAt());

        compressed.setGlobalIndex(context.getGlobalIndex());

        currentSize = compressed.calculateEstimatedSize();
        if (currentSize >= maxSizeChars) {
            compressGlobalIndex(compressed.getGlobalIndex());
            return compressed;
        }

        if (context.getDomainSummary() != null) {
            DomainSummary domainSummary = compressDomainSummary(context.getDomainSummary());
            compressed.setDomainSummary(domainSummary);

            currentSize = compressed.calculateEstimatedSize();
            if (currentSize >= maxSizeChars) {
                return compressed;
            }
        }

        if (context.getServiceDetail() != null) {
            ServiceDetail serviceDetail = compressServiceDetail(context.getServiceDetail());
            compressed.setServiceDetail(serviceDetail);

            currentSize = compressed.calculateEstimatedSize();
            if (currentSize >= maxSizeChars) {
                return compressed;
            }
        }

        int remainingSpace = maxSizeChars - currentSize;
        int perServiceBudget = remainingSpace / Math.max(1, context.getRelatedServices().size());

        for (ServiceSummary summary : context.getRelatedServices()) {
            ServiceSummary compressedSummary = compressServiceSummary(summary, perServiceBudget);
            compressed.addRelatedService(compressedSummary);

            if (compressed.calculateEstimatedSize() >= maxSizeChars) {
                break;
            }
        }

        log.info("上下文压缩完成: 最终大小={}chars", compressed.calculateEstimatedSize());
        return compressed;
    }

    /**
     * 压缩全局索引
     *
     * @param index 全局索引
     */
    private void compressGlobalIndex(GlobalServiceIndex index) {
        if (index == null) {
            return;
        }

        List<ServiceBrief> services = index.getServices();
        if (services != null && services.size() > 20) {
            index.setServices(services.subList(0, 20));
        }

        for (ServiceBrief brief : index.getServices()) {
            brief.setDescription(truncate(brief.getDescription(), 50));
        }

        List<InterfaceBrief> interfaces = index.getHotInterfaces();
        if (interfaces != null && interfaces.size() > 10) {
            index.setHotInterfaces(interfaces.subList(0, 10));
        }
    }

    /**
     * 压缩领域摘要
     *
     * @param summary 领域摘要
     * @return 压缩后的摘要
     */
    private DomainSummary compressDomainSummary(DomainSummary summary) {
        if (summary == null) {
            return null;
        }

        DomainSummary compressed = new DomainSummary();
        compressed.setDomainId(summary.getDomainId());
        compressed.setDomainName(summary.getDomainName());
        compressed.setDomainCode(summary.getDomainCode());
        compressed.setDescription(truncate(summary.getDescription(), DEFAULT_MAX_DESCRIPTION_LENGTH));
        compressed.setOwner(summary.getOwner());

        List<ServiceBrief> services = summary.getServices();
        if (services != null) {
            int limit = Math.min(services.size(), DEFAULT_MAX_SERVICES_PER_DOMAIN);
            for (int i = 0; i < limit; i++) {
                ServiceBrief brief = services.get(i);
                brief.setDescription(truncate(brief.getDescription(), 80));
                compressed.getServices().add(brief);
            }

            if (services.size() > limit) {
                log.debug("领域服务列表被截断: 原始={}, 保留={}", services.size(), limit);
            }
        }

        List<InterfaceBrief> interfaces = summary.getCoreInterfaces();
        if (interfaces != null) {
            int limit = Math.min(interfaces.size(), DEFAULT_MAX_INTERFACES_PER_SERVICE);
            compressed.setCoreInterfaces(interfaces.subList(0, limit));
        }

        compressed.setOutboundDependencies(summary.getOutboundDependencies());

        return compressed;
    }

    /**
     * 压缩服务详情
     *
     * @param detail 服务详情
     * @return 压缩后的详情
     */
    private ServiceDetail compressServiceDetail(ServiceDetail detail) {
        if (detail == null) {
            return null;
        }

        ServiceDetail compressed = new ServiceDetail();
        compressed.setServiceId(detail.getServiceId());
        compressed.setServiceName(detail.getServiceName());
        compressed.setDescription(truncate(detail.getDescription(), DEFAULT_MAX_DESCRIPTION_LENGTH));
        compressed.setDomainName(detail.getDomainName());
        compressed.setTechStack(detail.getTechStack());
        compressed.setOwner(detail.getOwner());

        List<ServiceDetail.InterfaceInfo> provided = detail.getProvidedInterfaces();
        if (provided != null) {
            int limit = Math.min(provided.size(), DEFAULT_MAX_INTERFACES_PER_SERVICE);
            for (int i = 0; i < limit; i++) {
                ServiceDetail.InterfaceInfo info = provided.get(i);
                compressInterfaceInfo(info);
                compressed.getProvidedInterfaces().add(info);
            }

            if (provided.size() > limit) {
                log.debug("提供的接口列表被截断: 原始={}, 保留={}", provided.size(), limit);
            }
        }

        List<ServiceDetail.InterfaceInfo> consumed = detail.getConsumedInterfaces();
        if (consumed != null) {
            int limit = Math.min(consumed.size(), DEFAULT_MAX_INTERFACES_PER_SERVICE);
            compressed.setConsumedInterfaces(consumed.subList(0, limit));
        }

        return compressed;
    }

    /**
     * 压缩接口信息
     *
     * @param info 接口信息
     */
    private void compressInterfaceInfo(ServiceDetail.InterfaceInfo info) {
        info.setDescription(truncate(info.getDescription(), 100));

        List<ServiceDetail.MethodSignature> methods = info.getMethods();
        if (methods != null && methods.size() > DEFAULT_MAX_METHODS_PER_INTERFACE) {
            info.setMethods(methods.subList(0, DEFAULT_MAX_METHODS_PER_INTERFACE));
        }

        if (methods != null) {
            for (ServiceDetail.MethodSignature method : info.getMethods()) {
                compressMethodSignature(method);
            }
        }
    }

    /**
     * 压缩方法签名
     *
     * @param method 方法签名
     */
    private void compressMethodSignature(ServiceDetail.MethodSignature method) {
        List<String> params = method.getParameterTypes();
        if (params != null && params.size() > DEFAULT_MAX_PARAMETERS) {
            method.setParameterTypes(params.subList(0, DEFAULT_MAX_PARAMETERS));
        }

        if (params != null) {
            for (int i = 0; i < method.getParameterTypes().size(); i++) {
                String param = method.getParameterTypes().get(i);
                method.getParameterTypes().set(i, simplifyGenericType(param));
            }
        }

        method.setReturnType(simplifyGenericType(method.getReturnType()));
    }

    /**
     * 压缩服务摘要
     *
     * @param summary 服务摘要
     * @param budget  字符预算
     * @return 压缩后的摘要
     */
    private ServiceSummary compressServiceSummary(ServiceSummary summary, int budget) {
        if (summary == null) {
            return null;
        }

        ServiceSummary compressed = new ServiceSummary();
        compressed.setServiceId(summary.getServiceId());
        compressed.setServiceName(summary.getServiceName());
        compressed.setDomainName(summary.getDomainName());
        compressed.setRelationType(summary.getRelationType());

        int descLength = Math.min(100, budget / 3);
        compressed.setDescription(truncate(summary.getDescription(), descLength));

        int maxInterfaces = Math.min(5, budget / 100);
        List<String> interfaces = summary.getRelatedInterfaces();
        if (interfaces != null && interfaces.size() > maxInterfaces) {
            compressed.setRelatedInterfaces(interfaces.subList(0, maxInterfaces));
        } else {
            compressed.setRelatedInterfaces(interfaces);
        }

        return compressed;
    }

    /**
     * 简化泛型类型
     *
     * @param type 类型字符串
     * @return 简化后的类型
     */
    private String simplifyGenericType(String type) {
        if (type == null) {
            return null;
        }

        int depth = 0;
        StringBuilder result = new StringBuilder();
        for (char c : type.toCharArray()) {
            if (c == '<') {
                depth++;
                if (depth == 1) {
                    result.append(c);
                }
            } else if (c == '>') {
                if (depth == 1) {
                    result.append("...");
                    result.append(c);
                }
                depth--;
            } else if (depth <= 1) {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * 截断文本
     *
     * @param text   文本
     * @param maxLen 最大长度
     * @return 截断后的文本
     */
    private String truncate(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }
}
