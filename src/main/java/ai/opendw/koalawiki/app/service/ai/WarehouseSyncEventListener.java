package ai.opendw.koalawiki.app.service.ai;

import ai.opendw.koalawiki.core.event.WarehouseSyncCompletedEvent;
import ai.opendw.koalawiki.domain.ai.AIDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 仓库同步事件监听器
 *
 * <p>职责: 监听仓库同步完成事件,自动触发文档生成</p>
 *
 * @author zhourui(V33215020)
 * @since 2025/11/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseSyncEventListener {

    private final DocumentGenerationService documentGenerationService;

    @Value("${koalawiki.doc.auto-generate:true}")
    private boolean autoGenerateEnabled;

    @Value("${koalawiki.doc.agent-type:claude}")
    private String defaultAgentType;

    /**
     * 监听仓库同步完成事件,自动生成文档
     *
     * @param event 仓库同步完成事件
     */
    @Async
    @EventListener
    public void handleWarehouseSyncCompleted(WarehouseSyncCompletedEvent event) {
        if (!autoGenerateEnabled) {
            log.debug("自动文档生成已禁用,跳过: warehouseId={}", event.getWarehouseId());
            return;
        }

        log.info("收到仓库同步完成事件,准备生成文档: warehouseId={}, syncRecordId={}, localPath={}",
                event.getWarehouseId(), event.getSyncRecordId(), event.getLocalPath());

        try {
            AIDocument document = documentGenerationService.generateForProject(
                    event.getWarehouseId(),
                    event.getLocalPath(),
                    defaultAgentType
            );

            log.info("仓库文档自动生成成功: warehouseId={}, documentId={}, title={}",
                    event.getWarehouseId(), document.getId(), document.getTitle());

        } catch (Exception e) {
            log.error("仓库文档自动生成失败: warehouseId={}, localPath={}",
                    event.getWarehouseId(), event.getLocalPath(), e);
        }
    }
}
