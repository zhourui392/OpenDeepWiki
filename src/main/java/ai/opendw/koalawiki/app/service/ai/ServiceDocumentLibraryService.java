package ai.opendw.koalawiki.app.service.ai;

import ai.opendw.koalawiki.domain.ai.ServiceDocumentLibrary;
import ai.opendw.koalawiki.infra.entity.ServiceDocumentConfigEntity;
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
 * 服务文档库领域服务
 *
 * @author zhourui(V33215020)
 * @since 2025/11/21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceDocumentLibraryService {

    private final ServiceDocumentConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    /**
     * 查询仓库下的所有服务配置
     *
     * @param warehouseId 仓库ID
     * @return 服务配置列表
     */
    public List<ServiceDocumentLibrary> listByWarehouse(String warehouseId) {
        return configRepository.findByWarehouseId(warehouseId).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    /**
     * 根据服务ID查询配置
     *
     * @param warehouseId 仓库ID
     * @param serviceId 服务ID
     * @return 服务配置
     */
    public ServiceDocumentLibrary getByServiceId(String warehouseId, String serviceId) {
        return configRepository.findByWarehouseIdAndServiceId(warehouseId, serviceId)
                .map(this::toModel)
                .orElse(null);
    }

    /**
     * 创建服务配置
     *
     * @param library 服务配置
     * @return 创建后的服务配置
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceDocumentLibrary create(ServiceDocumentLibrary library) {
        validateServiceId(library.getWarehouseId(), library.getServiceId());

        ServiceDocumentConfigEntity entity = toEntity(library);
        entity.setId(UUID.randomUUID().toString());
        entity = configRepository.save(entity);

        log.info("创建服务配置成功: warehouseId={}, serviceId={}",
                library.getWarehouseId(), library.getServiceId());
        return toModel(entity);
    }

    /**
     * 更新服务配置
     *
     * @param library 服务配置
     * @return 更新后的服务配置
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceDocumentLibrary update(ServiceDocumentLibrary library) {
        ServiceDocumentConfigEntity entity = configRepository.findById(library.getId())
                .orElseThrow(() -> new IllegalArgumentException("服务配置不存在: " + library.getId()));

        entity.setServiceName(library.getServiceName());
        entity.setDescription(library.getDescription());
        entity.setDocType(library.getDocType());
        entity.setPromptTemplateId(library.getPromptTemplateId());
        entity.setAgentType(library.getAgentType());
        entity.setSourceGlobs(serializeGlobs(library.getSourceGlobs()));
        entity.setEnabled(library.getEnabled());

        entity = configRepository.save(entity);
        log.info("更新服务配置成功: id={}, serviceId={}", library.getId(), library.getServiceId());
        return toModel(entity);
    }

    /**
     * 删除服务配置
     *
     * @param id 配置ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        configRepository.deleteById(id);
        log.info("删除服务配置成功: id={}", id);
    }

    /**
     * 根据服务配置解析匹配的源文件
     *
     * @param warehouseId 仓库ID
     * @param serviceId 服务ID
     * @param allFiles 所有文件列表
     * @return 匹配的文件列表
     */
    public List<String> resolveSources(String warehouseId, String serviceId, List<String> allFiles) {
        ServiceDocumentLibrary config = getByServiceId(warehouseId, serviceId);
        if (config == null || config.getSourceGlobs() == null || config.getSourceGlobs().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> matched = new ArrayList<>();
        for (String file : allFiles) {
            for (String glob : config.getSourceGlobs()) {
                if (matchGlob(file, glob)) {
                    matched.add(file);
                    break;
                }
            }
        }
        return matched;
    }

    /**
     * 简单的glob匹配
     */
    private boolean matchGlob(String path, String glob) {
        String regex = glob.replace("**", ".*").replace("*", "[^/]*").replace("?", ".");
        return path.matches(regex);
    }

    /**
     * 校验服务ID是否已存在
     */
    private void validateServiceId(String warehouseId, String serviceId) {
        if (configRepository.existsByWarehouseIdAndServiceId(warehouseId, serviceId)) {
            throw new IllegalArgumentException("服务ID已存在: " + serviceId);
        }
    }

    /**
     * 实体转领域模型
     */
    private ServiceDocumentLibrary toModel(ServiceDocumentConfigEntity entity) {
        ServiceDocumentLibrary library = new ServiceDocumentLibrary();
        library.setId(entity.getId());
        library.setWarehouseId(entity.getWarehouseId());
        library.setDomainId(entity.getDomainId());
        library.setServiceId(entity.getServiceId());
        library.setServiceName(entity.getServiceName());
        library.setDescription(entity.getDescription());
        library.setDocumentContent(entity.getDocumentContent());
        library.setDocType(entity.getDocType());
        library.setPromptTemplateId(entity.getPromptTemplateId());
        library.setAgentType(entity.getAgentType());
        library.setSourceGlobs(deserializeGlobs(entity.getSourceGlobs()));
        library.setEnabled(entity.getEnabled());
        library.setCreatedAt(entity.getCreatedAt());
        library.setUpdatedAt(entity.getUpdatedAt());
        return library;
    }

    /**
     * 领域模型转实体
     */
    private ServiceDocumentConfigEntity toEntity(ServiceDocumentLibrary library) {
        ServiceDocumentConfigEntity entity = new ServiceDocumentConfigEntity();
        entity.setWarehouseId(library.getWarehouseId());
        entity.setDomainId(library.getDomainId());
        entity.setServiceId(library.getServiceId());
        entity.setServiceName(library.getServiceName());
        entity.setDescription(library.getDescription());
        entity.setDocumentContent(library.getDocumentContent());
        entity.setDocType(library.getDocType());
        entity.setPromptTemplateId(library.getPromptTemplateId());
        entity.setAgentType(library.getAgentType());
        entity.setSourceGlobs(serializeGlobs(library.getSourceGlobs()));
        entity.setEnabled(library.getEnabled());
        return entity;
    }

    /**
     * 序列化sourceGlobs为JSON字符串
     */
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

    /**
     * 反序列化JSON字符串为sourceGlobs
     */
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
