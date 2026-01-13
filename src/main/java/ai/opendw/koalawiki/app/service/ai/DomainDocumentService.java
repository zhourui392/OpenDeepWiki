package ai.opendw.koalawiki.app.service.ai;

import ai.opendw.koalawiki.core.ai.AIAgent;
import ai.opendw.koalawiki.core.ai.AIAgentFactory;
import ai.opendw.koalawiki.core.ai.FilePromptTemplateService;
import ai.opendw.koalawiki.domain.ai.DomainInfo;
import ai.opendw.koalawiki.domain.ai.ServiceDocumentLibrary;
import ai.opendw.koalawiki.infra.entity.DomainInfoEntity;
import ai.opendw.koalawiki.infra.entity.ServiceDocumentConfigEntity;
import ai.opendw.koalawiki.infra.repository.DomainInfoRepository;
import ai.opendw.koalawiki.infra.repository.ServiceDocumentConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 领域文档服务
 * 管理领域和服务，支持通过提示词生成文档
 *
 * @author zhourui(V33215020)
 * @since 2025/01/13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainDocumentService {

    private final DomainInfoRepository domainRepository;
    private final ServiceDocumentConfigRepository serviceRepository;
    private final FilePromptTemplateService promptService;
    private final AIAgentFactory agentFactory;
    private final ObjectMapper objectMapper;

    // ==================== 领域管理 ====================

    /**
     * 查询仓库下的所有领域
     */
    public List<DomainInfo> listDomains(String warehouseId) {
        return domainRepository.findByWarehouseId(warehouseId).stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    /**
     * 获取领域详情（包含服务列表）
     */
    public DomainInfo getDomainWithServices(String domainId) {
        DomainInfoEntity entity = domainRepository.findById(domainId).orElse(null);
        if (entity == null) {
            return null;
        }
        DomainInfo domain = toDomainModel(entity);
        domain.setServices(listServicesByDomain(domainId));
        return domain;
    }

    /**
     * 创建领域
     *
     * @param warehouseId 仓库ID
     * @param name        领域名称
     * @param description 领域描述
     * @param code        领域代码
     * @return 创建的领域信息
     */
    @Transactional(rollbackFor = Exception.class)
    public DomainInfo createDomain(String warehouseId, String name, String description, String code) {
        DomainInfoEntity entity = new DomainInfoEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setWarehouseId(warehouseId);
        entity.setName(name);
        entity.setDescription(description);
        entity.setCode(code);
        entity = domainRepository.save(entity);
        log.info("创建领域成功: warehouseId={}, name={}", warehouseId, name);
        return toDomainModel(entity);
    }

    /**
     * 更新领域
     *
     * @param domainId    领域ID
     * @param name        领域名称
     * @param description 领域描述
     * @param code        领域代码
     * @return 更新后的领域信息
     */
    @Transactional(rollbackFor = Exception.class)
    public DomainInfo updateDomain(String domainId, String name, String description, String code) {
        DomainInfoEntity entity = domainRepository.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("领域不存在: " + domainId));
        entity.setName(name);
        entity.setDescription(description);
        entity.setCode(code);
        entity = domainRepository.save(entity);
        return toDomainModel(entity);
    }

    /**
     * 删除领域
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDomain(String domainId) {
        domainRepository.deleteById(domainId);
        log.info("删除领域成功: domainId={}", domainId);
    }

    // ==================== 服务管理 ====================

    /**
     * 查询领域下的服务
     */
    public List<ServiceDocumentLibrary> listServicesByDomain(String domainId) {
        return serviceRepository.findByDomainId(domainId).stream()
                .map(this::toServiceModel)
                .collect(Collectors.toList());
    }

    /**
     * 创建服务
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceDocumentLibrary createService(String warehouseId, String domainId,
                                                 String serviceId, String serviceName,
                                                 String description, List<String> sourceGlobs) {
        if (serviceRepository.existsByWarehouseIdAndServiceId(warehouseId, serviceId)) {
            throw new IllegalArgumentException("服务ID已存在: " + serviceId);
        }

        ServiceDocumentConfigEntity entity = new ServiceDocumentConfigEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setWarehouseId(warehouseId);
        entity.setDomainId(domainId);
        entity.setServiceId(serviceId);
        entity.setServiceName(serviceName);
        entity.setDescription(description);
        entity.setSourceGlobs(serializeGlobs(sourceGlobs));
        entity.setDocType("SERVICE");
        entity.setEnabled(true);
        entity = serviceRepository.save(entity);
        log.info("创建服务成功: domainId={}, serviceId={}", domainId, serviceId);
        return toServiceModel(entity);
    }

    /**
     * 更新服务
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceDocumentLibrary updateService(String id, String serviceName,
                                                 String description, List<String> sourceGlobs) {
        ServiceDocumentConfigEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("服务不存在: " + id));
        entity.setServiceName(serviceName);
        entity.setDescription(description);
        entity.setSourceGlobs(serializeGlobs(sourceGlobs));
        entity = serviceRepository.save(entity);
        return toServiceModel(entity);
    }

    /**
     * 删除服务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteService(String id) {
        serviceRepository.deleteById(id);
        log.info("删除服务成功: id={}", id);
    }

    // ==================== 文档生成 ====================

    /**
     * 生成服务文档
     */
    @Transactional(rollbackFor = Exception.class)
    public String generateServiceDocument(String serviceId, String sourceCode) {
        ServiceDocumentConfigEntity entity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("服务不存在: " + serviceId));

        // 获取领域名称
        String domainName = "";
        if (entity.getDomainId() != null) {
            DomainInfoEntity domain = domainRepository.findById(entity.getDomainId()).orElse(null);
            if (domain != null) {
                domainName = domain.getName();
            }
        }

        // 渲染提示词
        String prompt = promptService.renderServiceDocPrompt(
                entity.getServiceName(),
                entity.getDescription(),
                domainName,
                sourceCode
        );

        // 调用AI生成
        AIAgent agent = agentFactory.getAgent(entity.getAgentType());
        String document = agent.execute(prompt);

        // 保存文档
        entity.setDocumentContent(document);
        serviceRepository.save(entity);

        log.info("服务文档生成成功: serviceId={}", serviceId);
        return document;
    }

    /**
     * 生成领域文档
     */
    @Transactional(rollbackFor = Exception.class)
    public String generateDomainDocument(String domainId, String sourceCodeSummary) {
        DomainInfoEntity entity = domainRepository.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("领域不存在: " + domainId));

        // 获取服务列表
        List<ServiceDocumentConfigEntity> services = serviceRepository.findByDomainId(domainId);
        String serviceList = services.stream()
                .map(s -> "- " + s.getServiceName() + ": " + (s.getDescription() != null ? s.getDescription() : ""))
                .collect(Collectors.joining("\n"));

        // 渲染提示词
        String prompt = promptService.renderDomainDocPrompt(
                entity.getName(),
                entity.getDescription(),
                serviceList,
                sourceCodeSummary
        );

        // 调用AI生成
        AIAgent agent = agentFactory.getAgent(null);
        String document = agent.execute(prompt);

        // 保存文档
        entity.setDocumentContent(document);
        domainRepository.save(entity);

        log.info("领域文档生成成功: domainId={}", domainId);
        return document;
    }

    // ==================== 转换方法 ====================

    private DomainInfo toDomainModel(DomainInfoEntity entity) {
        DomainInfo model = new DomainInfo();
        model.setId(entity.getId());
        model.setWarehouseId(entity.getWarehouseId());
        model.setName(entity.getName());
        model.setDescription(entity.getDescription());
        model.setCode(entity.getCode());
        model.setDocumentContent(entity.getDocumentContent());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        return model;
    }

    private ServiceDocumentLibrary toServiceModel(ServiceDocumentConfigEntity entity) {
        ServiceDocumentLibrary model = new ServiceDocumentLibrary();
        model.setId(entity.getId());
        model.setWarehouseId(entity.getWarehouseId());
        model.setDomainId(entity.getDomainId());
        model.setServiceId(entity.getServiceId());
        model.setServiceName(entity.getServiceName());
        model.setDescription(entity.getDescription());
        model.setDocumentContent(entity.getDocumentContent());
        model.setDocType(entity.getDocType());
        model.setPromptTemplateId(entity.getPromptTemplateId());
        model.setAgentType(entity.getAgentType());
        model.setSourceGlobs(deserializeGlobs(entity.getSourceGlobs()));
        model.setEnabled(entity.getEnabled());
        model.setLastCommitId(entity.getLastCommitId());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        return model;
    }

    private String serializeGlobs(List<String> globs) {
        if (globs == null || globs.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(globs);
        } catch (JsonProcessingException e) {
            log.error("序列化sourceGlobs失败", e);
            return null;
        }
    }

    private List<String> deserializeGlobs(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("反序列化sourceGlobs失败", e);
            return new ArrayList<>();
        }
    }
}
