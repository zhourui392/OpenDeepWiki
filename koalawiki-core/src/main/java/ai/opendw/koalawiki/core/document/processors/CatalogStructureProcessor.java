package ai.opendw.koalawiki.core.document.processors;

import ai.opendw.koalawiki.core.document.pipeline.AbstractDocumentProcessor;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingContext;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 目录结构生成处理器
 * 生成文档目录结构，支持多语言目录（i18n）和导航菜单
 *
 * 参考C#实现: DocumentResultCatalogue, DocumentsHelper.ProcessCatalogueItems
 */
@Slf4j
@Component
public class CatalogStructureProcessor extends AbstractDocumentProcessor {

    public CatalogStructureProcessor() {
        super("CatalogStructureProcessor", 40);
    }

    @Override
    public boolean canProcess(DocumentProcessingContext context) {
        // 只处理目录类型
        return context.getDocumentType() == DocumentProcessingContext.DocumentType.DIRECTORY;
    }

    @Override
    protected DocumentProcessingResult doProcess(DocumentProcessingContext context) {
        log.info("生成目录结构: {}", context.getDocumentPath());

        DocumentProcessingResult result = DocumentProcessingResult.success(getName());

        try {
            // 获取目录信息（可能是优化后的）
            String catalogue = context.getParameter("optimizedCatalogue", String.class)
                .orElseGet(() -> context.getParameter("catalogue", String.class).orElse(""));

            if (catalogue.isEmpty()) {
                return DocumentProcessingResult.failure(getName(),
                    "目录结构为空");
            }

            // 解析目录结构
            DocumentCatalogue catalogueStructure = parseCatalogue(catalogue);

            // 生成导航菜单
            NavigationMenu navigationMenu = generateNavigationMenu(catalogueStructure);

            // 将结果保存到共享状态
            context.putSharedState("catalogueStructure", catalogueStructure);
            context.putSharedState("navigationMenu", navigationMenu);

            // 更新指标
            int totalItems = countItems(catalogueStructure);
            result.addOutput("totalItems", totalItems);
            result.addOutput("maxDepth", calculateMaxDepth(catalogueStructure));

            result.setMessage(String.format("成功生成目录结构，共 %d 个项目", totalItems));

            result.getMetrics()
                .setDocumentsProcessed(1)
                .setBytesProcessed(catalogue.length());

            log.info("目录结构生成完成: 项目数={}", totalItems);

        } catch (Exception e) {
            log.error("目录结构生成失败: {}", context.getDocumentPath(), e);
            return DocumentProcessingResult.failure(getName(),
                "目录结构生成异常: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 解析目录字符串为目录结构
     */
    private DocumentCatalogue parseCatalogue(String catalogue) {
        DocumentCatalogue result = new DocumentCatalogue();
        String[] lines = catalogue.split("\n");

        // 使用栈来处理层级关系
        List<CatalogueItem> stack = new ArrayList<>();
        CatalogueItem root = new CatalogueItem();
        root.setLevel(-1);
        root.setChildren(result.getItems());
        stack.add(root);

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            // 计算缩进级别
            int level = calculateIndentLevel(line);
            String content = line.trim();

            // 创建目录项
            CatalogueItem item = new CatalogueItem();
            item.setLevel(level);
            item.setName(extractName(content));
            item.setTitle(extractTitle(content));
            item.setPath(content);
            item.setChildren(new ArrayList<>());

            // 找到父节点
            while (!stack.isEmpty() && stack.get(stack.size() - 1).getLevel() >= level) {
                stack.remove(stack.size() - 1);
            }

            // 添加到父节点的子节点列表
            if (!stack.isEmpty()) {
                CatalogueItem parent = stack.get(stack.size() - 1);
                parent.getChildren().add(item);
            }

            // 将当前节点入栈
            stack.add(item);
        }

        return result;
    }

    /**
     * 计算缩进级别
     */
    private int calculateIndentLevel(String line) {
        int level = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') {
                level++;
            } else {
                break;
            }
        }
        return level / 2; // 假设每级缩进2个空格
    }

    /**
     * 从路径中提取名称
     */
    private String extractName(String path) {
        // 移除路径中的目录分隔符
        String[] parts = path.split("[/\\\\]");
        if (parts.length > 0) {
            String name = parts[parts.length - 1];
            // 移除文件扩展名
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                return name.substring(0, lastDot);
            }
            return name;
        }
        return path;
    }

    /**
     * 从路径中提取标题
     */
    private String extractTitle(String path) {
        String name = extractName(path);
        // 将下划线和连字符替换为空格
        name = name.replace("_", " ").replace("-", " ");
        // 首字母大写
        if (!name.isEmpty()) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return name;
    }

    /**
     * 生成导航菜单
     */
    private NavigationMenu generateNavigationMenu(DocumentCatalogue catalogue) {
        NavigationMenu menu = new NavigationMenu();

        for (CatalogueItem item : catalogue.getItems()) {
            NavigationItem navItem = convertToNavigationItem(item, null);
            menu.getItems().add(navItem);
        }

        return menu;
    }

    /**
     * 将目录项转换为导航项
     */
    private NavigationItem convertToNavigationItem(CatalogueItem item, String parentPath) {
        NavigationItem navItem = new NavigationItem();
        navItem.setName(item.getName());
        navItem.setTitle(item.getTitle());

        // 生成URL
        String url = parentPath != null ? parentPath + "_" + item.getTitle() : item.getTitle();
        navItem.setUrl(url.replace(" ", ""));

        navItem.setPath(item.getPath());
        navItem.setChildren(new ArrayList<>());

        // 递归处理子项
        if (item.getChildren() != null && !item.getChildren().isEmpty()) {
            for (CatalogueItem child : item.getChildren()) {
                NavigationItem childNavItem = convertToNavigationItem(child, navItem.getUrl());
                navItem.getChildren().add(childNavItem);
            }
        }

        return navItem;
    }

    /**
     * 统计目录项总数
     */
    private int countItems(DocumentCatalogue catalogue) {
        int count = 0;
        for (CatalogueItem item : catalogue.getItems()) {
            count += countItemsRecursive(item);
        }
        return count;
    }

    private int countItemsRecursive(CatalogueItem item) {
        int count = 1;
        if (item.getChildren() != null) {
            for (CatalogueItem child : item.getChildren()) {
                count += countItemsRecursive(child);
            }
        }
        return count;
    }

    /**
     * 计算最大深度
     */
    private int calculateMaxDepth(DocumentCatalogue catalogue) {
        int maxDepth = 0;
        for (CatalogueItem item : catalogue.getItems()) {
            maxDepth = Math.max(maxDepth, calculateDepthRecursive(item, 1));
        }
        return maxDepth;
    }

    private int calculateDepthRecursive(CatalogueItem item, int currentDepth) {
        int maxDepth = currentDepth;
        if (item.getChildren() != null && !item.getChildren().isEmpty()) {
            for (CatalogueItem child : item.getChildren()) {
                maxDepth = Math.max(maxDepth, calculateDepthRecursive(child, currentDepth + 1));
            }
        }
        return maxDepth;
    }

    /**
     * 文档目录结构
     */
    @Data
    public static class DocumentCatalogue {
        private List<CatalogueItem> items = new ArrayList<>();
    }

    /**
     * 目录项
     */
    @Data
    public static class CatalogueItem {
        private String name;
        private String title;
        private String path;
        private String prompt;
        private int level;
        private List<CatalogueItem> children;
    }

    /**
     * 导航菜单
     */
    @Data
    public static class NavigationMenu {
        private List<NavigationItem> items = new ArrayList<>();
    }

    /**
     * 导航项
     */
    @Data
    public static class NavigationItem {
        private String name;
        private String title;
        private String url;
        private String path;
        private int order;
        private List<NavigationItem> children;
    }
}
