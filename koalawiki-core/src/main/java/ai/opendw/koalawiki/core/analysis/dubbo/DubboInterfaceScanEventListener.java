package ai.opendw.koalawiki.core.analysis.dubbo;

import ai.opendw.koalawiki.core.cluster.DubboInterfaceService;
import ai.opendw.koalawiki.core.cluster.ServiceClusterService;
import ai.opendw.koalawiki.core.event.WarehouseSyncCompletedEvent;
import ai.opendw.koalawiki.infra.repository.cluster.ClusterWarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Dubbo接口扫描事件监听器
 * 监听仓库同步完成事件，触发Dubbo接口扫描
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DubboInterfaceScanEventListener {

    private final DubboInterfaceService dubboInterfaceService;
    private final ClusterWarehouseRepository clusterWarehouseRepository;

    /**
     * 监听仓库同步完成事件
     *
     * @param event 同步完成事件
     */
    @Async
    @EventListener
    public void onWarehouseSyncCompleted(WarehouseSyncCompletedEvent event) {
        String warehouseId = event.getWarehouseId();
        String localPath = event.getLocalPath();

        log.info("收到仓库同步完成事件，开始Dubbo接口扫描: warehouseId={}", warehouseId);

        try {
            Optional<String> clusterIdOpt = findClusterIdByWarehouse(warehouseId);

            if (!clusterIdOpt.isPresent()) {
                log.debug("仓库未关联集群，跳过Dubbo接口扫描: warehouseId={}", warehouseId);
                return;
            }

            String clusterId = clusterIdOpt.get();
            String serviceName = extractServiceName(localPath);

            DubboInterfaceScanner.ScanResult result = dubboInterfaceService.scanAndUpdate(
                    Paths.get(localPath), clusterId, warehouseId, serviceName);

            log.info("Dubbo接口扫描完成: warehouseId={}, 提供接口数={}, 消费接口数={}",
                    warehouseId, result.getProviderCount(), result.getConsumerCount());

        } catch (Exception e) {
            log.error("Dubbo接口扫描失败: warehouseId={}", warehouseId, e);
        }
    }

    /**
     * 根据仓库ID查找所属集群ID
     */
    private Optional<String> findClusterIdByWarehouse(String warehouseId) {
        return clusterWarehouseRepository.findClusterIdByWarehouseId(warehouseId);
    }

    /**
     * 从本地路径提取服务名
     */
    private String extractServiceName(String localPath) {
        if (localPath == null || localPath.isEmpty()) {
            return "unknown";
        }

        Path path = Paths.get(localPath);
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : "unknown";
    }
}
