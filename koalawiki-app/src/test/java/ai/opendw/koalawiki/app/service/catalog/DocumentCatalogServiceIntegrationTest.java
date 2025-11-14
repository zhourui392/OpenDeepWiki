package ai.opendw.koalawiki.app.service.catalog;

import ai.opendw.koalawiki.app.service.DocumentCatalogServiceImpl;
import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import ai.opendw.koalawiki.infra.entity.DocumentCatalogEntity;
import ai.opendw.koalawiki.infra.repository.DocumentCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 文档目录服务集成测试
 *
 * @author OpenDeepWiki Team
 * @date 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
public class DocumentCatalogServiceIntegrationTest {

    @Mock
    private DocumentCatalogRepository catalogRepository;

    @InjectMocks
    private DocumentCatalogServiceImpl catalogService;

    private DocumentCatalogEntity mockEntity;
    private DocumentCatalog mockDomain;

    @BeforeEach
    public void setUp() {
        // 准备测试数据
        mockEntity = new DocumentCatalogEntity();
        mockEntity.setId("catalog-1");
        mockEntity.setWarehouseId("warehouse-1");
        mockEntity.setDocumentId("doc-1");
        mockEntity.setParentId(null);
        mockEntity.setPath("/README.md");
        mockEntity.setName("README.md");
        mockEntity.setType("file");
        mockEntity.setLevel(1);
        mockEntity.setOrder(1);
        mockEntity.setIsLeaf(true);
        mockEntity.setDeleted(false);
        mockEntity.setCreatedAt(LocalDateTime.now());
        mockEntity.setUpdatedAt(LocalDateTime.now());

        mockDomain = new DocumentCatalog();
        mockDomain.setId("catalog-1");
        mockDomain.setWarehouseId("warehouse-1");
        mockDomain.setDocumentId("doc-1");
        mockDomain.setPath("/README.md");
        mockDomain.setName("README.md");
        mockDomain.setType("file");
    }

    /**
     * 测试：获取仓库目录树 - 成功场景
     */
    @Test
    public void testGetCatalogTree_Success() {
        // Given
        String warehouseId = "warehouse-1";
        List<DocumentCatalogEntity> entities = new ArrayList<>();

        // 根节点
        DocumentCatalogEntity root = new DocumentCatalogEntity();
        root.setId("root");
        root.setWarehouseId(warehouseId);
        root.setParentId(null);
        root.setPath("/");
        root.setName("root");
        root.setType("directory");
        root.setLevel(0);
        entities.add(root);

        // 子节点
        DocumentCatalogEntity child = new DocumentCatalogEntity();
        child.setId("child-1");
        child.setWarehouseId(warehouseId);
        child.setParentId("root");
        child.setPath("/src");
        child.setName("src");
        child.setType("directory");
        child.setLevel(1);
        entities.add(child);

        when(catalogRepository.findByWarehouseIdAndDeleted(warehouseId, false))
            .thenReturn(entities);

        // When
        DocumentCatalog result = catalogService.getCatalogTree(warehouseId);

        // Then
        assertNotNull(result);
        assertEquals("root", result.getId());
        assertNotNull(result.getChildren());
        assertEquals(1, result.getChildren().size());
        verify(catalogRepository, times(1)).findByWarehouseIdAndDeleted(warehouseId, false);
    }

    /**
     * 测试：创建目录 - 成功场景
     */
    @Test
    public void testCreateCatalog_Success() {
        // Given
        when(catalogRepository.save(any(DocumentCatalogEntity.class)))
            .thenReturn(mockEntity);
        when(catalogRepository.findById(anyString()))
            .thenReturn(Optional.empty());

        // When
        DocumentCatalog result = catalogService.createCatalog(mockDomain);

        // Then
        assertNotNull(result);
        assertEquals("catalog-1", result.getId());
        assertEquals("/README.md", result.getPath());
        verify(catalogRepository, times(1)).save(any(DocumentCatalogEntity.class));
    }

    /**
     * 测试：更新目录 - 成功场景
     */
    @Test
    public void testUpdateCatalog_Success() {
        // Given
        String catalogId = "catalog-1";
        when(catalogRepository.findById(catalogId))
            .thenReturn(Optional.of(mockEntity));
        when(catalogRepository.save(any(DocumentCatalogEntity.class)))
            .thenReturn(mockEntity);

        mockDomain.setName("UPDATED_README.md");

        // When
        DocumentCatalog result = catalogService.updateCatalog(catalogId, mockDomain);

        // Then
        assertNotNull(result);
        verify(catalogRepository, times(1)).findById(catalogId);
        verify(catalogRepository, times(1)).save(any(DocumentCatalogEntity.class));
    }

    /**
     * 测试：更新目录 - 目录不存在
     */
    @Test
    public void testUpdateCatalog_NotFound() {
        // Given
        String catalogId = "non-existent";
        when(catalogRepository.findById(catalogId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            catalogService.updateCatalog(catalogId, mockDomain);
        });
    }

    /**
     * 测试：删除目录 - 软删除
     */
    @Test
    public void testDeleteCatalog_SoftDelete() {
        // Given
        String catalogId = "catalog-1";
        when(catalogRepository.findById(catalogId))
            .thenReturn(Optional.of(mockEntity));

        // When
        catalogService.deleteCatalog(catalogId);

        // Then
        verify(catalogRepository, times(1)).findById(catalogId);
        verify(catalogRepository, times(1)).save(any(DocumentCatalogEntity.class));
        assertTrue(mockEntity.getDeleted());
    }

    /**
     * 测试：获取子目录 - 成功场景
     */
    @Test
    public void testGetChildren_Success() {
        // Given
        String parentId = "parent-1";
        List<DocumentCatalogEntity> children = new ArrayList<>();

        DocumentCatalogEntity child1 = new DocumentCatalogEntity();
        child1.setId("child-1");
        child1.setParentId(parentId);
        child1.setName("file1.md");
        children.add(child1);

        DocumentCatalogEntity child2 = new DocumentCatalogEntity();
        child2.setId("child-2");
        child2.setParentId(parentId);
        child2.setName("file2.md");
        children.add(child2);

        when(catalogRepository.findByParentIdAndDeleted(parentId, false))
            .thenReturn(children);

        // When
        List<DocumentCatalog> result = catalogService.getChildren(parentId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(catalogRepository, times(1)).findByParentIdAndDeleted(parentId, false);
    }

    /**
     * 测试：分页查询 - 成功场景
     */
    @Test
    public void testListCatalogs_Success() {
        // Given
        String warehouseId = "warehouse-1";
        Pageable pageable = PageRequest.of(0, 10);

        List<DocumentCatalogEntity> entities = Arrays.asList(mockEntity);
        Page<DocumentCatalogEntity> page = new PageImpl<>(entities, pageable, 1);

        when(catalogRepository.findByWarehouseIdAndDeleted(warehouseId, false, pageable))
            .thenReturn(page);

        // When
        Page<DocumentCatalog> result = catalogService.listCatalogs(warehouseId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(catalogRepository, times(1)).findByWarehouseIdAndDeleted(warehouseId, false, pageable);
    }

    /**
     * 测试：统计目录数量 - 成功场景
     */
    @Test
    public void testCountCatalogs_Success() {
        // Given
        String warehouseId = "warehouse-1";
        when(catalogRepository.countByWarehouseIdAndDeleted(warehouseId, false))
            .thenReturn(42L);

        // When
        long count = catalogService.countCatalogs(warehouseId);

        // Then
        assertEquals(42L, count);
        verify(catalogRepository, times(1)).countByWarehouseIdAndDeleted(warehouseId, false);
    }

    /**
     * 测试：刷新目录 - 成功场景
     */
    @Test
    public void testRefreshCatalog_Success() {
        // Given
        String warehouseId = "warehouse-1";
        List<DocumentCatalogEntity> oldCatalogs = Arrays.asList(mockEntity);

        when(catalogRepository.findByWarehouseIdAndDeleted(warehouseId, false))
            .thenReturn(oldCatalogs);

        // When
        catalogService.refreshCatalog(warehouseId);

        // Then
        verify(catalogRepository, times(1)).findByWarehouseIdAndDeleted(warehouseId, false);
        // 应该标记旧目录为删除
    }

    /**
     * 测试：搜索目录 - 成功场景
     */
    @Test
    public void testSearchCatalog_Success() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setWarehouseId("warehouse-1");
        request.setKeyword("README");
        request.setScope(SearchScope.TITLE);
        request.setMaxResults(10);

        List<DocumentCatalogEntity> entities = Arrays.asList(mockEntity);
        when(catalogRepository.findByWarehouseIdAndNameContainingAndDeleted(
            request.getWarehouseId(), request.getKeyword(), false))
            .thenReturn(entities);

        // When
        SearchResult result = catalogService.search(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(1, result.getTotalCount());
        assertTrue(result.getItems().size() > 0);
    }

    /**
     * 测试：排序目录 - 字母顺序
     */
    @Test
    public void testSortCatalog_Alphabetical() {
        // Given
        String warehouseId = "warehouse-1";
        List<DocumentCatalogEntity> unsorted = new ArrayList<>();

        DocumentCatalogEntity b = new DocumentCatalogEntity();
        b.setId("b");
        b.setName("b.md");
        unsorted.add(b);

        DocumentCatalogEntity a = new DocumentCatalogEntity();
        a.setId("a");
        a.setName("a.md");
        unsorted.add(a);

        when(catalogRepository.findByWarehouseIdAndDeleted(warehouseId, false))
            .thenReturn(unsorted);

        CatalogSortStrategy strategy = new AlphabeticalSortStrategy();

        // When
        List<DocumentCatalog> result = catalogService.sortCatalog(warehouseId, strategy);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("a.md", result.get(0).getName());
        assertEquals("b.md", result.get(1).getName());
    }

    /**
     * 测试：过滤目录 - 按类型
     */
    @Test
    public void testFilterCatalog_ByType() {
        // Given
        String warehouseId = "warehouse-1";
        FilterCriteria criteria = new FilterCriteria();
        List<String> includeTypes = new ArrayList<>();
        includeTypes.add("file");
        criteria.setIncludeTypes(includeTypes);

        List<DocumentCatalogEntity> allEntities = new ArrayList<>();

        DocumentCatalogEntity file = new DocumentCatalogEntity();
        file.setType("file");
        file.setName("file.md");
        allEntities.add(file);

        DocumentCatalogEntity dir = new DocumentCatalogEntity();
        dir.setType("directory");
        dir.setName("dir");
        allEntities.add(dir);

        when(catalogRepository.findByWarehouseIdAndDeleted(warehouseId, false))
            .thenReturn(allEntities);

        // When
        List<DocumentCatalog> result = catalogService.filterCatalog(warehouseId, criteria);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("file", result.get(0).getType());
    }

    /**
     * 测试：批量创建目录
     */
    @Test
    public void testBatchCreateCatalogs_Success() {
        // Given
        List<DocumentCatalog> catalogs = new ArrayList<>();
        catalogs.add(mockDomain);

        DocumentCatalog catalog2 = new DocumentCatalog();
        catalog2.setWarehouseId("warehouse-1");
        catalog2.setPath("/src/main.java");
        catalog2.setName("main.java");
        catalogs.add(catalog2);

        when(catalogRepository.saveAll(anyList()))
            .thenReturn(Arrays.asList(mockEntity));

        // When
        List<DocumentCatalog> result = catalogService.batchCreateCatalogs(catalogs);

        // Then
        assertNotNull(result);
        assertTrue(result.size() > 0);
        verify(catalogRepository, times(1)).saveAll(anyList());
    }

    /**
     * 测试：批量删除目录
     */
    @Test
    public void testBatchDeleteCatalogs_Success() {
        // Given
        List<String> catalogIds = Arrays.asList("catalog-1", "catalog-2");

        when(catalogRepository.findById("catalog-1"))
            .thenReturn(Optional.of(mockEntity));
        when(catalogRepository.findById("catalog-2"))
            .thenReturn(Optional.of(mockEntity));

        // When
        catalogService.batchDeleteCatalogs(catalogIds);

        // Then
        verify(catalogRepository, times(catalogIds.size())).findById(anyString());
        verify(catalogRepository, times(1)).saveAll(anyList());
    }

    /**
     * 测试：获取目录路径
     */
    @Test
    public void testGetCatalogPath_Success() {
        // Given
        String catalogId = "catalog-1";
        when(catalogRepository.findById(catalogId))
            .thenReturn(Optional.of(mockEntity));

        // When
        String path = catalogService.getCatalogPath(catalogId);

        // Then
        assertNotNull(path);
        assertEquals("/README.md", path);
    }

    /**
     * 测试：移动目录
     */
    @Test
    public void testMoveCatalog_Success() {
        // Given
        String catalogId = "catalog-1";
        String newParentId = "new-parent";

        when(catalogRepository.findById(catalogId))
            .thenReturn(Optional.of(mockEntity));
        when(catalogRepository.save(any(DocumentCatalogEntity.class)))
            .thenReturn(mockEntity);

        // When
        DocumentCatalog result = catalogService.moveCatalog(catalogId, newParentId);

        // Then
        assertNotNull(result);
        verify(catalogRepository, times(1)).findById(catalogId);
        verify(catalogRepository, times(1)).save(any(DocumentCatalogEntity.class));
    }
}
