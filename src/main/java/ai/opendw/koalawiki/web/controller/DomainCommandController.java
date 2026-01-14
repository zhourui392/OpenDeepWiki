package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.core.ai.AIAgent;
import ai.opendw.koalawiki.core.ai.AIAgentFactory;
import ai.opendw.koalawiki.core.git.GitPathResolver;
import ai.opendw.koalawiki.infra.entity.DomainInfoEntity;
import ai.opendw.koalawiki.infra.entity.ServiceDocumentConfigEntity;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.repository.DomainInfoRepository;
import ai.opendw.koalawiki.infra.repository.ServiceDocumentConfigRepository;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 领域命令执行Controller
 * 在领域或服务的工作目录下执行Claude CLI命令
 *
 * @author zhourui(V33215020)
 * @since 2026/01/14
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cmd")
@RequiredArgsConstructor
public class DomainCommandController {

    private final DomainInfoRepository domainRepository;
    private final ServiceDocumentConfigRepository serviceRepository;
    private final WarehouseRepository warehouseRepository;
    private final AIAgentFactory agentFactory;
    private final GitPathResolver gitPathResolver;

    /**
     * 在领域目录下执行命令
     *
     * @param domainCode 领域代码
     * @param command    命令内容
     * @return 执行结果
     */
    @PostMapping("/{domainCode}/{command}")
    public CommandResponse executeDomainCommand(
            @PathVariable String domainCode,
            @PathVariable String command,
            @RequestBody(required = false) CommandRequest request) {
        log.info("执行领域命令: domainCode={}, command={}", domainCode, command);

        DomainInfoEntity domain = domainRepository.findByCode(domainCode)
                .orElseThrow(() -> new IllegalArgumentException("领域不存在: " + domainCode));

        String workingDirectory = resolveWorkingDirectory(domain);
        if (workingDirectory == null) {
            return CommandResponse.error("无法确定领域工作目录，请确保领域下有关联仓库的服务");
        }

        return executeCommand(command, request, workingDirectory);
    }

    /**
     * 在服务目录下执行命令
     *
     * @param domainCode 领域代码
     * @param serviceId  服务ID
     * @param command    命令内容
     * @return 执行结果
     */
    @PostMapping("/{domainCode}/{serviceId}/{command}")
    public CommandResponse executeServiceCommand(
            @PathVariable String domainCode,
            @PathVariable String serviceId,
            @PathVariable String command,
            @RequestBody(required = false) CommandRequest request) {
        log.info("执行服务命令: domainCode={}, serviceId={}, command={}", domainCode, serviceId, command);

        DomainInfoEntity domain = domainRepository.findByCode(domainCode)
                .orElseThrow(() -> new IllegalArgumentException("领域不存在: " + domainCode));

        ServiceDocumentConfigEntity service = serviceRepository.findByDomainIdAndServiceId(domain.getId(), serviceId)
                .orElseThrow(() -> new IllegalArgumentException("服务不存在: " + serviceId));

        String workingDirectory = resolveServiceWorkingDirectory(service);
        if (workingDirectory == null) {
            return CommandResponse.error("无法确定服务工作目录，请确保服务关联了有效仓库");
        }

        return executeCommand(command, request, workingDirectory);
    }

    /**
     * 执行命令
     */
    private CommandResponse executeCommand(String command, CommandRequest request, String workingDirectory) {
        String prompt = "/" + command;
        if (request != null && request.getArgs() != null) {
            prompt = prompt + " " + request.getArgs();
        }

        try {
            AIAgent agent = agentFactory.getAgent(null);
            String result = agent.execute(prompt, workingDirectory);
            return CommandResponse.success(result);
        } catch (Exception e) {
            log.error("命令执行失败", e);
            return CommandResponse.error("执行失败: " + e.getMessage());
        }
    }

    /**
     * 解析领域工作目录
     * 取领域下第一个服务关联的仓库路径
     */
    private String resolveWorkingDirectory(DomainInfoEntity domain) {
        List<ServiceDocumentConfigEntity> services = serviceRepository.findByDomainId(domain.getId());
        if (services.isEmpty()) {
            return null;
        }

        return resolveServiceWorkingDirectory(services.get(0));
    }

    /**
     * 解析服务工作目录
     */
    private String resolveServiceWorkingDirectory(ServiceDocumentConfigEntity service) {
        if (service.getWarehouseId() == null) {
            return null;
        }

        WarehouseEntity warehouse = warehouseRepository.findById(service.getWarehouseId()).orElse(null);
        if (warehouse == null || warehouse.getAddress() == null) {
            return null;
        }

        return gitPathResolver.getLocalPath(warehouse.getAddress());
    }

    @Data
    public static class CommandRequest {
        private String args;
    }

    @Data
    public static class CommandResponse {
        private boolean success;
        private String message;
        private String result;

        public static CommandResponse success(String result) {
            CommandResponse response = new CommandResponse();
            response.setSuccess(true);
            response.setResult(result);
            return response;
        }

        public static CommandResponse error(String message) {
            CommandResponse response = new CommandResponse();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
    }
}
