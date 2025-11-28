package ai.opendw.koalawiki.core.ai.context;

import ai.opendw.koalawiki.core.cluster.BusinessDomainService;
import ai.opendw.koalawiki.core.cluster.DubboInterfaceService;
import ai.opendw.koalawiki.core.cluster.ServiceClusterService;
import ai.opendw.koalawiki.domain.cluster.BusinessDomain;
import ai.opendw.koalawiki.domain.cluster.ContextLevel;
import ai.opendw.koalawiki.domain.cluster.ServiceCluster;
import ai.opendw.koalawiki.domain.cluster.ServiceInfo;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceConsumer;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 微服务AI上下文构建器
 * 针对50+服务场景优化的分层上下文构建
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroserviceContextBuilder {

    private final ServiceClusterService clusterService;
    private final BusinessDomainService domainService;
    private final DubboInterfaceService interfaceService;

    private static final int MAX_SERVICES_IN_L1 = 100;
    private static final int MAX_INTERFACES_IN_L1 = 20;
    private static final int MAX_SERVICES_IN_DOMAIN = 50;
    private static final int MAX_INTERFACES_IN_DOMAIN = 30;
    private static final int MAX_RELATED_SERVICES = 10;

    /**
     * 构建分层上下文
     *
     * @param clusterId       集群ID
     * @param targetServiceId 目标服务ID（可选）
     * @param contextLevel    上下文级别（L1-L4）
     * @return AI上下文
     */
    public AIContext buildLayeredContext(String clusterId, String targetServiceId, ContextLevel contextLevel) {
        log.info("构建分层上下文: clusterId={}, targetServiceId={}, level={}",
                clusterId, targetServiceId, contextLevel);

        AIContext context = new AIContext();
        context.setClusterId(clusterId);
        context.setTargetServiceId(targetServiceId);
        context.setContextLevel(contextLevel);
        context.setGeneratedAt(new Date());

        context.setGlobalIndex(buildGlobalIndex(clusterId));

        if (contextLevel.ordinal() >= ContextLevel.L2.ordinal() && targetServiceId != null) {
            String domainId = findDomainByService(clusterId, targetServiceId);
            if (domainId != null) {
                context.setDomainSummary(buildDomainSummary(domainId));
            }
        }

        if (contextLevel.ordinal() >= ContextLevel.L3.ordinal() && targetServiceId != null) {
            context.setServiceDetail(buildServiceDetail(targetServiceId));
        }

        if (contextLevel.ordinal() >= ContextLevel.L4.ordinal() && targetServiceId != null) {
            List<ServiceSummary> related = buildRelatedServices(targetServiceId);
            for (ServiceSummary summary : related) {
                context.addRelatedService(summary);
            }
        }

        context.calculateEstimatedSize();
        log.info("上下文构建完成: size={} chars", context.getEstimatedSize());

        return context;
    }

    /**
     * 按需构建上下文（根据查询意图智能选择）
     *
     * @param clusterId 集群ID
     * @param query     用户查询
     * @param hints     提示信息
     * @return AI上下文
     */
    public AIContext buildOnDemandContext(String clusterId, String query, List<String> hints) {
        log.info("按需构建上下文: clusterId={}, query={}", clusterId, query);

        QueryIntent intent = analyzeQueryIntent(query);
        List<String> relevantServices = findRelevantServices(clusterId, query, hints);

        AIContext context = new AIContext();
        context.setClusterId(clusterId);
        context.setContextLevel(intent.recommendedContextLevel());
        context.setGeneratedAt(new Date());

        context.setGlobalIndex(buildCompactGlobalIndex(clusterId));

        for (String serviceId : relevantServices) {
            context.addServiceSummary(buildServiceSummary(serviceId));
        }

        if (intent.needsInterfaceDetail()) {
            List<String> interfaceNames = extractInterfaceNames(query);
            for (String interfaceName : interfaceNames) {
                InterfaceDetail detail = buildInterfaceDetail(clusterId, interfaceName);
                if (detail != null) {
                    context.addInterfaceDetail(detail);
                }
            }
        }

        context.calculateEstimatedSize();
        return context;
    }

    /**
     * 构建全局服务索引（L1层）
     *
     * @param clusterId 集群ID
     * @return 全局索引
     */
    @Cacheable(value = "globalIndex", key = "#clusterId", unless = "#result == null")
    public GlobalServiceIndex buildGlobalIndex(String clusterId) {
        log.debug("构建全局服务索引: clusterId={}", clusterId);

        Optional<ServiceCluster> clusterOpt = clusterService.getById(clusterId);
        if (!clusterOpt.isPresent()) {
            log.warn("集群不存在: {}", clusterId);
            return null;
        }

        ServiceCluster cluster = clusterOpt.get();
        GlobalServiceIndex index = new GlobalServiceIndex();
        index.setClusterId(clusterId);
        index.setClusterName(cluster.getName());
        index.setTechStack(cluster.getTechStack());
        index.setTotalServices(cluster.getWarehouseIds() != null ? cluster.getWarehouseIds().size() : 0);

        List<BusinessDomain> domains = cluster.getDomains();
        if (domains != null) {
            List<DomainBrief> domainBriefs = domains.stream()
                    .map(this::toDomainBrief)
                    .collect(Collectors.toList());
            index.setDomains(domainBriefs);
        }

        List<ServiceBrief> serviceBriefs = buildServiceBriefs(cluster);
        if (serviceBriefs.size() > MAX_SERVICES_IN_L1) {
            serviceBriefs = serviceBriefs.subList(0, MAX_SERVICES_IN_L1);
        }
        index.setServices(serviceBriefs);

        List<DubboInterfaceRegistry> topInterfaces = interfaceService.getTopInterfaces(clusterId, MAX_INTERFACES_IN_L1);
        List<InterfaceBrief> hotInterfaces = topInterfaces.stream()
                .map(this::toInterfaceBrief)
                .collect(Collectors.toList());
        index.setHotInterfaces(hotInterfaces);

        index.setTotalInterfaces((int) interfaceService.countByCluster(clusterId));

        return index;
    }

    /**
     * 构建精简版全局索引
     *
     * @param clusterId 集群ID
     * @return 精简版索引
     */
    public GlobalServiceIndex buildCompactGlobalIndex(String clusterId) {
        GlobalServiceIndex full = buildGlobalIndex(clusterId);
        if (full == null) {
            return null;
        }

        GlobalServiceIndex compact = new GlobalServiceIndex();
        compact.setClusterId(full.getClusterId());
        compact.setClusterName(full.getClusterName());
        compact.setTechStack(full.getTechStack());
        compact.setTotalServices(full.getTotalServices());
        compact.setTotalInterfaces(full.getTotalInterfaces());
        compact.setDomains(full.getDomains());

        return compact;
    }

    /**
     * 构建领域摘要（L2层）
     *
     * @param domainId 领域ID
     * @return 领域摘要
     */
    @Cacheable(value = "domainSummary", key = "#domainId", unless = "#result == null")
    public DomainSummary buildDomainSummary(String domainId) {
        log.debug("构建领域摘要: domainId={}", domainId);

        Optional<BusinessDomain> domainOpt = domainService.getById(domainId);
        if (!domainOpt.isPresent()) {
            log.warn("领域不存在: {}", domainId);
            return null;
        }

        BusinessDomain domain = domainOpt.get();
        DomainSummary summary = new DomainSummary();
        summary.setDomainId(domainId);
        summary.setDomainName(domain.getName());
        summary.setDomainCode(domain.getCode());
        summary.setDescription(domain.getDescription());
        summary.setOwner(domain.getOwner());

        List<ServiceInfo> services = domain.getServices();
        if (services != null) {
            List<ServiceBrief> serviceBriefs = services.stream()
                    .limit(MAX_SERVICES_IN_DOMAIN)
                    .map(this::toServiceBrief)
                    .collect(Collectors.toList());
            summary.setServices(serviceBriefs);

            List<InterfaceBrief> coreInterfaces = new ArrayList<>();
            for (ServiceInfo service : services) {
                List<DubboInterfaceRegistry> interfaces = interfaceService.listByProvider(service.getWarehouseId());
                for (DubboInterfaceRegistry reg : interfaces) {
                    if (coreInterfaces.size() >= MAX_INTERFACES_IN_DOMAIN) {
                        break;
                    }
                    coreInterfaces.add(toInterfaceBrief(reg));
                }
                if (coreInterfaces.size() >= MAX_INTERFACES_IN_DOMAIN) {
                    break;
                }
            }
            summary.setCoreInterfaces(coreInterfaces);
        }

        return summary;
    }

    /**
     * 构建服务详情（L3层）
     *
     * @param serviceId 服务ID（仓库ID）
     * @return 服务详情
     */
    public ServiceDetail buildServiceDetail(String serviceId) {
        log.debug("构建服务详情: serviceId={}", serviceId);

        ServiceDetail detail = new ServiceDetail();
        detail.setServiceId(serviceId);

        List<DubboInterfaceRegistry> providedInterfaces = interfaceService.listByProvider(serviceId);
        for (DubboInterfaceRegistry registry : providedInterfaces) {
            ServiceDetail.InterfaceInfo info = new ServiceDetail.InterfaceInfo();
            info.setInterfaceName(registry.getInterfaceName());
            info.setDescription(registry.getDescription());
            info.setDeprecated(registry.getDeprecated() != null && registry.getDeprecated());

            if (registry.getMethods() != null) {
                List<ServiceDetail.MethodSignature> methods = registry.getMethods().stream()
                        .map(m -> {
                            ServiceDetail.MethodSignature sig = new ServiceDetail.MethodSignature();
                            sig.setName(m.getName());
                            sig.setReturnType(m.getReturnType());
                            sig.setDescription(m.getDescription());
                            if (m.getParameters() != null) {
                                sig.setParameterTypes(m.getParameters().stream()
                                        .map(p -> p.getType())
                                        .collect(Collectors.toList()));
                            }
                            return sig;
                        })
                        .collect(Collectors.toList());
                info.setMethods(methods);
            }

            detail.getProvidedInterfaces().add(info);
        }

        List<DubboInterfaceRegistry> consumedInterfaces = interfaceService.getConsumedInterfaces(serviceId);
        for (DubboInterfaceRegistry registry : consumedInterfaces) {
            ServiceDetail.InterfaceInfo info = new ServiceDetail.InterfaceInfo();
            info.setInterfaceName(registry.getInterfaceName());
            info.setProviderServiceName(registry.getProviderServiceName());
            info.setDescription(registry.getDescription());
            detail.getConsumedInterfaces().add(info);
        }

        return detail;
    }

    /**
     * 构建相关服务列表（L4层）
     *
     * @param targetServiceId 目标服务ID
     * @return 相关服务列表
     */
    public List<ServiceSummary> buildRelatedServices(String targetServiceId) {
        log.debug("构建相关服务: targetServiceId={}", targetServiceId);

        List<ServiceSummary> related = new ArrayList<>();

        List<DubboInterfaceRegistry> providedInterfaces = interfaceService.listByProvider(targetServiceId);
        Set<String> upstreamServiceIds = new HashSet<>();
        for (DubboInterfaceRegistry registry : providedInterfaces) {
            List<DubboInterfaceConsumer> consumers = interfaceService.getConsumers(registry.getId());
            for (DubboInterfaceConsumer consumer : consumers) {
                upstreamServiceIds.add(consumer.getConsumerWarehouseId());
            }
        }

        int count = 0;
        for (String upstreamId : upstreamServiceIds) {
            if (count >= MAX_RELATED_SERVICES / 2) {
                break;
            }
            ServiceSummary summary = buildServiceSummary(upstreamId);
            if (summary != null) {
                summary.setRelationType(ServiceSummary.RelationType.UPSTREAM);
                related.add(summary);
                count++;
            }
        }

        List<DubboInterfaceRegistry> consumedInterfaces = interfaceService.getConsumedInterfaces(targetServiceId);
        Set<String> downstreamServiceIds = new HashSet<>();
        for (DubboInterfaceRegistry registry : consumedInterfaces) {
            if (registry.getProviderWarehouseId() != null) {
                downstreamServiceIds.add(registry.getProviderWarehouseId());
            }
        }

        count = 0;
        for (String downstreamId : downstreamServiceIds) {
            if (count >= MAX_RELATED_SERVICES / 2) {
                break;
            }
            ServiceSummary summary = buildServiceSummary(downstreamId);
            if (summary != null) {
                summary.setRelationType(ServiceSummary.RelationType.DOWNSTREAM);
                related.add(summary);
                count++;
            }
        }

        return related;
    }

    /**
     * 构建服务摘要
     *
     * @param serviceId 服务ID
     * @return 服务摘要
     */
    public ServiceSummary buildServiceSummary(String serviceId) {
        ServiceSummary summary = new ServiceSummary();
        summary.setServiceId(serviceId);

        List<DubboInterfaceRegistry> interfaces = interfaceService.listByProvider(serviceId);
        List<String> interfaceNames = interfaces.stream()
                .map(DubboInterfaceRegistry::getInterfaceName)
                .limit(5)
                .collect(Collectors.toList());
        summary.setRelatedInterfaces(interfaceNames);

        return summary;
    }

    /**
     * 构建接口详情
     *
     * @param clusterId     集群ID
     * @param interfaceName 接口名
     * @return 接口详情
     */
    public InterfaceDetail buildInterfaceDetail(String clusterId, String interfaceName) {
        Optional<DubboInterfaceRegistry> registryOpt = interfaceService.getByName(clusterId, interfaceName, null, null);
        if (!registryOpt.isPresent()) {
            return null;
        }

        DubboInterfaceRegistry registry = registryOpt.get();
        InterfaceDetail detail = new InterfaceDetail();
        detail.setId(registry.getId());
        detail.setInterfaceName(registry.getInterfaceName());
        detail.setVersion(registry.getVersion());
        detail.setGroup(registry.getGroupName());
        detail.setDescription(registry.getDescription());
        detail.setProviderServiceName(registry.getProviderServiceName());
        detail.setProviderWarehouseId(registry.getProviderWarehouseId());
        detail.setMethods(registry.getMethods());
        detail.setDeprecated(registry.getDeprecated() != null && registry.getDeprecated());
        detail.setDeprecatedReason(registry.getDeprecatedReason());

        List<DubboInterfaceConsumer> consumers = interfaceService.getConsumers(registry.getId());
        List<InterfaceDetail.ConsumerInfo> consumerInfos = consumers.stream()
                .map(c -> {
                    InterfaceDetail.ConsumerInfo info = new InterfaceDetail.ConsumerInfo();
                    info.setWarehouseId(c.getConsumerWarehouseId());
                    info.setServiceName(c.getConsumerServiceName());
                    info.setSourceClass(c.getSourceClass());
                    return info;
                })
                .collect(Collectors.toList());
        detail.setConsumers(consumerInfos);

        return detail;
    }

    /**
     * 分析查询意图
     *
     * @param query 查询文本
     * @return 查询意图
     */
    public QueryIntent analyzeQueryIntent(String query) {
        QueryIntent intent = new QueryIntent();
        String lowerQuery = query.toLowerCase();

        if (containsAny(lowerQuery, "概览", "overview", "全局", "集群")) {
            intent.setIntentType(QueryIntent.IntentType.OVERVIEW_QUERY);
            intent.setConfidence(0.8);
        } else if (containsAny(lowerQuery, "领域", "domain", "业务域")) {
            intent.setIntentType(QueryIntent.IntentType.DOMAIN_QUERY);
            intent.setConfidence(0.8);
        } else if (containsAny(lowerQuery, "接口", "interface", "api", "dubbo")) {
            intent.setIntentType(QueryIntent.IntentType.INTERFACE_QUERY);
            intent.setConfidence(0.8);
        } else if (containsAny(lowerQuery, "调用链", "链路", "call chain", "trace")) {
            intent.setIntentType(QueryIntent.IntentType.CALL_CHAIN_QUERY);
            intent.setConfidence(0.8);
        } else if (containsAny(lowerQuery, "影响", "impact", "变更", "依赖")) {
            intent.setIntentType(QueryIntent.IntentType.IMPACT_ANALYSIS);
            intent.setConfidence(0.8);
        } else if (containsAny(lowerQuery, "集成", "对接", "integrate", "设计")) {
            intent.setIntentType(QueryIntent.IntentType.INTEGRATION_DESIGN);
            intent.setConfidence(0.7);
        } else if (containsAny(lowerQuery, "服务", "service", "微服务")) {
            intent.setIntentType(QueryIntent.IntentType.SERVICE_QUERY);
            intent.setConfidence(0.7);
        } else {
            intent.setIntentType(QueryIntent.IntentType.GENERAL);
            intent.setConfidence(0.5);
        }

        intent.setIdentifiedInterfaces(extractInterfaceNames(query));

        return intent;
    }

    /**
     * 查找相关服务
     *
     * @param clusterId 集群ID
     * @param query     查询文本
     * @param hints     提示
     * @return 服务ID列表
     */
    public List<String> findRelevantServices(String clusterId, String query, List<String> hints) {
        List<String> result = new ArrayList<>();

        if (hints != null) {
            result.addAll(hints);
        }

        return result.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 提取接口名
     *
     * @param text 文本
     * @return 接口名列表
     */
    public List<String> extractInterfaceNames(String text) {
        List<String> interfaces = new ArrayList<>();

        Pattern pattern = Pattern.compile("([a-zA-Z][a-zA-Z0-9]*\\.)+[A-Z][a-zA-Z0-9]*(?:Service|Api|Facade|Interface)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            interfaces.add(matcher.group());
        }

        return interfaces;
    }

    /**
     * 根据服务查找所属领域
     *
     * @param clusterId 集群ID
     * @param serviceId 服务ID
     * @return 领域ID
     */
    private String findDomainByService(String clusterId, String serviceId) {
        List<BusinessDomain> domains = domainService.listByClusterId(clusterId);
        for (BusinessDomain domain : domains) {
            if (domain.getServices() != null) {
                for (ServiceInfo service : domain.getServices()) {
                    if (serviceId.equals(service.getWarehouseId())) {
                        return domain.getId();
                    }
                }
            }
        }
        return null;
    }

    private List<ServiceBrief> buildServiceBriefs(ServiceCluster cluster) {
        List<ServiceBrief> briefs = new ArrayList<>();

        if (cluster.getDomains() != null) {
            for (BusinessDomain domain : cluster.getDomains()) {
                if (domain.getServices() != null) {
                    for (ServiceInfo service : domain.getServices()) {
                        ServiceBrief brief = toServiceBrief(service);
                        brief.setDomainId(domain.getId());
                        brief.setDomainName(domain.getName());
                        briefs.add(brief);
                    }
                }
            }
        }

        return briefs;
    }

    private DomainBrief toDomainBrief(BusinessDomain domain) {
        DomainBrief brief = new DomainBrief();
        brief.setId(domain.getId());
        brief.setName(domain.getName());
        brief.setCode(domain.getCode());
        brief.setServiceCount(domain.getServices() != null ? domain.getServices().size() : 0);
        return brief;
    }

    private ServiceBrief toServiceBrief(ServiceInfo service) {
        ServiceBrief brief = new ServiceBrief();
        brief.setId(service.getWarehouseId());
        brief.setName(service.getServiceName());
        brief.setDescription(service.getDescription());
        brief.setServiceType(service.getType());
        brief.setInterfaceCount((int) interfaceService.countByProvider(service.getWarehouseId()));
        return brief;
    }

    private InterfaceBrief toInterfaceBrief(DubboInterfaceRegistry registry) {
        InterfaceBrief brief = new InterfaceBrief();
        brief.setId(registry.getId());
        brief.setInterfaceName(registry.getInterfaceName());
        brief.setVersion(registry.getVersion());
        brief.setProviderServiceName(registry.getProviderServiceName());
        brief.setProviderWarehouseId(registry.getProviderWarehouseId());
        brief.setDescription(registry.getDescription());
        brief.setDeprecated(registry.getDeprecated() != null && registry.getDeprecated());
        brief.setMethodCount(registry.getMethods() != null ? registry.getMethods().size() : 0);

        List<String> consumerIds = registry.getConsumerServiceIds();
        brief.setConsumerCount(consumerIds != null ? consumerIds.size() : 0);

        return brief;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
