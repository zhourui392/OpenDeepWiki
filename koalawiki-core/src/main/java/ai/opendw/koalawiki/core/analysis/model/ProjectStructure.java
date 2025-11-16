package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目结构数据模型
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
public class ProjectStructure {

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目路径
     */
    private String projectPath;

    /**
     * 模块列表
     */
    private List<ModuleInfo> modules = new ArrayList<>();

    /**
     * 所有入口点
     */
    private List<EntryPoint> entryPoints = new ArrayList<>();

    /**
     * 类信息Map (key: 全限定类名)
     */
    private Map<String, ClassInfo> classes = new HashMap<>();

    /**
     * 添加模块
     */
    public void addModule(ModuleInfo module) {
        this.modules.add(module);
    }

    /**
     * 添加入口点
     */
    public void addEntryPoint(EntryPoint entryPoint) {
        this.entryPoints.add(entryPoint);
    }

    /**
     * 添加类信息
     */
    public void addClass(ClassInfo classInfo) {
        this.classes.put(classInfo.getFullClassName(), classInfo);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("moduleCount", modules.size());
        stats.put("entryPointCount", entryPoints.size());
        stats.put("classCount", classes.size());

        // 按类型统计入口
        Map<EntryType, Long> entryTypeCount = new HashMap<>();
        for (EntryPoint ep : entryPoints) {
            entryTypeCount.merge(ep.getType(), 1L, Long::sum);
        }
        stats.put("entryTypeCount", entryTypeCount);

        return stats;
    }
}
