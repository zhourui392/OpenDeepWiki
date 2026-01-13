package ai.opendw.koalawiki.app.task;

import ai.opendw.koalawiki.app.service.ai.DomainDocumentService;
import ai.opendw.koalawiki.core.git.GitRepositoryInfo;
import ai.opendw.koalawiki.core.git.GitService;
import ai.opendw.koalawiki.core.git.GitPathResolver;
import ai.opendw.koalawiki.infra.entity.DomainInfoEntity;
import ai.opendw.koalawiki.infra.entity.ServiceDocumentConfigEntity;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.repository.DomainInfoRepository;
import ai.opendw.koalawiki.infra.repository.ServiceDocumentConfigRepository;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 领域文档同步定时任务
 * 每天凌晨2点执行，检测服务仓库更新并重新生成文档
 *
 * @author zhourui(V33215020)
 * @since 2026/01/13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainDocSyncTask {

    private final DomainInfoRepository domainRepository;
    private final ServiceDocumentConfigRepository serviceRepository;
    private final WarehouseRepository warehouseRepository;
    private final GitService gitService;
    private final GitPathResolver pathResolver;
    private final DomainDocumentService domainDocumentService;

    @Value("${koalawiki.doc-sync.enabled:true}")
    private boolean syncEnabled;

    /**
     * 每天凌晨2点执行文档同步
     */
    @Scheduled(cron = "${koalawiki.doc-sync.cron:0 0 2 * * ?}")
    public void executeDailySync() {
        if (!syncEnabled) {
            log.debug("文档同步任务已禁用");
            return;
        }

        log.info("开始执行领域文档同步任务");
        List<DomainInfoEntity> domains = domainRepository.findAll();

        for (DomainInfoEntity domain : domains) {
            syncDomainDocuments(domain);
        }
        log.info("领域文档同步任务执行完成");
    }

    /**
     * 同步单个领域的文档
     *
     * @param domain 领域实体
     */
    private void syncDomainDocuments(DomainInfoEntity domain) {
        if (domain.getCode() == null) {
            return;
        }

        List<ServiceDocumentConfigEntity> services =
                serviceRepository.findByDomainIdAndEnabled(domain.getId(), true);
        boolean domainNeedsUpdate = false;

        for (ServiceDocumentConfigEntity service : services) {
            if (syncServiceDocument(domain, service)) {
                domainNeedsUpdate = true;
            }
        }

        if (domainNeedsUpdate) {
            generateDomainDocument(domain);
        }
    }

    /**
     * 同步单个服务的文档
     *
     * @param domain  领域实体
     * @param service 服务配置实体
     * @return 是否有更新
     */
    private boolean syncServiceDocument(DomainInfoEntity domain, ServiceDocumentConfigEntity service) {
        WarehouseEntity warehouse = warehouseRepository.findById(service.getWarehouseId()).orElse(null);
        if (warehouse == null) {
            return false;
        }

        String localPath = pathResolver.getLocalPathByDomain(domain.getCode(), warehouse.getName());

        try {
            GitRepositoryInfo repoInfo = gitService.pullRepository(localPath, null);
            String latestCommitId = repoInfo.getLatestCommitId();

            if (latestCommitId != null && !latestCommitId.equals(service.getLastCommitId())) {
                domainDocumentService.generateServiceDocument(service.getId(), "");
                service.setLastCommitId(latestCommitId);
                serviceRepository.save(service);
                log.info("服务文档已更新: serviceId={}, serviceName={}", service.getId(), service.getServiceName());
                return true;
            }
        } catch (Exception e) {
            log.error("同步服务文档失败: serviceId={}, error={}", service.getId(), e.getMessage());
        }
        return false;
    }

    /**
     * 生成领域文档
     *
     * @param domain 领域实体
     */
    private void generateDomainDocument(DomainInfoEntity domain) {
        try {
            domainDocumentService.generateDomainDocument(domain.getId(), "");
            log.info("领域文档已更新: domainId={}, domainName={}", domain.getId(), domain.getName());
        } catch (Exception e) {
            log.error("生成领域文档失败: domainId={}, error={}", domain.getId(), e.getMessage());
        }
    }
}
