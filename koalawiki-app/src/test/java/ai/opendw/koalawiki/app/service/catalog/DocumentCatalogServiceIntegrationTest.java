package ai.opendw.koalawiki.app.service.catalog;

import ai.opendw.koalawiki.app.ai.IAIService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文档目录服务集成测试（对齐当前实现与仓储接口）
 */
@ExtendWith(MockitoExtension.class)
public class DocumentCatalogServiceIntegrationTest {

    @Mock
    private DocumentCatalogRepository catalogRepository;

    @Mock
    private IAIService aiService;

    @InjectMocks
    private DocumentCatalogServiceImpl catalogService;

    private DocumentCatalogEntity mockEntity;
    private DocumentCatalog mockDomain;

    @BeforeEach
    public void setUp() {
        mockEntity = new DocumentCatalogEntity();
        mockEntity.setId("catalog-1");
        mockEntity.setWarehouseId("warehouse-1");
        mockEntity.setDocumentId("doc-1");
        mockEntity.setParentId(null);
        mockEntity.setName("README.md");
        mockEntity.setUrl("/README.md");
        mockEntity.setDescription("README file");
        mockEntity.setOrder(1);
        mockEntity.setIsCompleted(false);
        mockEntity.setIsDeleted(false);
        mockEntity.setCreatedAt(new Date());

        mockDomain = new DocumentCatalog();
        mockDomain.setId("catalog-1");
        mockDomain.setWarehouseId("warehouse-1");
        mockDomain.setDocumentId("doc-1");
        mockDomain.setName("README.md");
        mockDomain.setUrl("/README.md");
        mockDomain.setDescription("README file");
        mockDomain.setOrder(1);
    }

    @Test
    public void testCreateCatalog_Success() {
        when(catalogRepository.save(any(DocumentCatalogEntity.class)))
                .thenReturn(mockEntity);

        DocumentCatalog toCreate = new DocumentCatalog();
        toCreate.setWarehouseId("warehouse-1");
        toCreate.setName("README.md");
        toCreate.setUrl("/README.md");

        DocumentCatalog result = catalogService.createCatalog(toCreate);

        assertNotNull(result);
        assertEquals("catalog-1", result.getId());
        verify(catalogRepository, times(1)).save(any(DocumentCatalogEntity.class));
    }

    @Test
    public void testUpdateCatalog_Success() {
        when(catalogRepository.findById("catalog-1"))
                .thenReturn(Optional.of(mockEntity));
        when(catalogRepository.save(any(DocumentCatalogEntity.class)))
                .thenReturn(mockEntity);

        DocumentCatalog update = new DocumentCatalog();
        update.setName("README-UPDATED.md");
        update.setDescription("updated");

        DocumentCatalog result = catalogService.updateCatalog("catalog-1", update);

        assertNotNull(result);
        assertEquals("catalog-1", result.getId());
        verify(catalogRepository, times(1)).findById("catalog-1");
        verify(catalogRepository, times(1)).save(any(DocumentCatalogEntity.class));
    }

    @Test
    public void testGetCatalog_Success() {
        when(catalogRepository.findById("catalog-1"))
                .thenReturn(Optional.of(mockEntity));

        DocumentCatalog result = catalogService.getCatalog("catalog-1");

        assertNotNull(result);
        assertEquals("catalog-1", result.getId());
        verify(catalogRepository, times(1)).findById("catalog-1");
    }

    @Test
    public void testGetCatalogTree_Success() {
        String warehouseId = "warehouse-1";
        when(catalogRepository.findCatalogTree(warehouseId))
                .thenReturn(Collections.singletonList(mockEntity));

        DocumentCatalog tree = catalogService.getCatalogTree(warehouseId);

        assertNotNull(tree);
        assertEquals("catalog-1", tree.getId());
        verify(catalogRepository, times(1)).findCatalogTree(warehouseId);
    }

    @Test
    public void testListCatalogs_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<DocumentCatalogEntity> page = new PageImpl<>(
                Collections.singletonList(mockEntity), pageable, 1);

        when(catalogRepository.findAll(pageable)).thenReturn(page);

        Page<DocumentCatalog> result = catalogService.listCatalogs(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(catalogRepository, times(1)).findAll(pageable);
    }

    @Test
    public void testListCatalogsByWarehouse_Success() {
        String warehouseId = "warehouse-1";
        Pageable pageable = PageRequest.of(0, 10);

        when(catalogRepository.findByWarehouseIdAndIsDeleted(warehouseId, false))
                .thenReturn(Collections.singletonList(mockEntity));

        Page<DocumentCatalog> result =
                catalogService.listCatalogsByWarehouse(warehouseId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(catalogRepository, times(1))
                .findByWarehouseIdAndIsDeleted(warehouseId, false);
    }

    @Test
    public void testListCatalogsByParent_Success() {
        DocumentCatalogEntity child = new DocumentCatalogEntity();
        child.setId("child-1");
        child.setWarehouseId("warehouse-1");
        child.setParentId("catalog-1");
        child.setName("Child.md");
        child.setUrl("/Child.md");
        child.setIsDeleted(false);

        when(catalogRepository.findAll())
                .thenReturn(Arrays.asList(mockEntity, child));

        List<DocumentCatalog> children =
                catalogService.listCatalogsByParent("catalog-1");

        assertNotNull(children);
        assertEquals(1, children.size());
        assertEquals("child-1", children.get(0).getId());
        verify(catalogRepository, times(1)).findAll();
    }

    @Test
    public void testSearchCatalogs_Success() {
        String warehouseId = "warehouse-1";
        when(catalogRepository.findByWarehouseIdAndIsDeleted(warehouseId, false))
                .thenReturn(Collections.singletonList(mockEntity));

        List<DocumentCatalog> result =
                catalogService.searchCatalogs(warehouseId, "readme");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(catalogRepository, times(1))
                .findByWarehouseIdAndIsDeleted(warehouseId, false);
    }

    @Test
    public void testSortCatalogs_Alphabetical() {
        String warehouseId = "warehouse-1";

        DocumentCatalogEntity a = new DocumentCatalogEntity();
        a.setId("a");
        a.setWarehouseId(warehouseId);
        a.setName("a.md");
        a.setUrl("/a.md");
        a.setIsDeleted(false);

        DocumentCatalogEntity b = new DocumentCatalogEntity();
        b.setId("b");
        b.setWarehouseId(warehouseId);
        b.setName("b.md");
        b.setUrl("/b.md");
        b.setIsDeleted(false);

        when(catalogRepository.findByWarehouseIdAndIsDeleted(warehouseId, false))
                .thenReturn(Arrays.asList(b, a));

        List<DocumentCatalog> result =
                catalogService.sortCatalogs(warehouseId, "alphabetical");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("a.md", result.get(0).getName());
        assertEquals("b.md", result.get(1).getName());
    }

    @Test
    public void testBatchCreateCatalogs_Success() {
        DocumentCatalog c1 = new DocumentCatalog();
        c1.setWarehouseId("warehouse-1");
        c1.setName("A.md");
        DocumentCatalog c2 = new DocumentCatalog();
        c2.setWarehouseId("warehouse-1");
        c2.setName("B.md");

        when(catalogRepository.saveAll(anyList()))
                .thenReturn(Collections.singletonList(mockEntity));

        List<DocumentCatalog> result =
                catalogService.batchCreateCatalogs(Arrays.asList(c1, c2));

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(catalogRepository, times(1)).saveAll(anyList());
    }

    @Test
    public void testBatchUpdateOrder_Success() {
        DocumentCatalogEntity e1 = new DocumentCatalogEntity();
        e1.setId("c1");
        e1.setWarehouseId("warehouse-1");
        e1.setName("c1");

        when(catalogRepository.findById(anyString()))
                .thenReturn(Optional.of(e1));
        when(catalogRepository.save(any(DocumentCatalogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        catalogService.batchUpdateOrder(Arrays.asList("c1", "c2"));

        verify(catalogRepository, atLeastOnce())
                .findById(anyString());
    }

    @Test
    public void testCountByWarehouse_Success() {
        String warehouseId = "warehouse-1";
        when(catalogRepository.findByWarehouseIdAndIsDeleted(warehouseId, false))
                .thenReturn(Collections.singletonList(mockEntity));

        long count = catalogService.countByWarehouse(warehouseId);

        assertEquals(1L, count);
        verify(catalogRepository, times(1))
                .findByWarehouseIdAndIsDeleted(warehouseId, false);
    }

    @Test
    public void testExists_Success() {
        when(catalogRepository.existsById("catalog-1"))
                .thenReturn(true);

        assertTrue(catalogService.exists("catalog-1"));
        verify(catalogRepository, times(1)).existsById("catalog-1");
    }
}

