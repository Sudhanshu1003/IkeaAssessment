package com.fulfilment.application.monolith.warehouses.integration;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.SearchWarehousesUseCase;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@WithTestResource(H2DatabaseTestResource.class)
public class WarehouseCoverageIntegrationTest {

    @Inject
    WarehouseRepository warehouseRepository;

    @Inject
    CreateWarehouseUseCase createWarehouseUseCase;

    @Inject
    ReplaceWarehouseUseCase replaceWarehouseUseCase;

    @Inject
    ArchiveWarehouseUseCase archiveWarehouseUseCase;

    @Inject
    SearchWarehousesUseCase searchWarehousesUseCase;

    @BeforeEach
    @Transactional
    void setUp() {
        warehouseRepository.deleteAll();
    }

    @Test
    @Transactional
    void testRepositoryCreateAndFind() {
        // Test repository create and find operations
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "COV-TEST-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;
        warehouse.createdAt = LocalDateTime.now();

        // Create warehouse
        warehouseRepository.create(warehouse);

        // Find warehouse
        Warehouse found = warehouseRepository.findByBusinessUnitCode("COV-TEST-001");
        assertNotNull(found);
        assertEquals("COV-TEST-001", found.businessUnitCode);
        assertEquals("AMSTERDAM-001", found.location);
        assertEquals(100, found.capacity);
        assertEquals(50, found.stock);
        assertNotNull(found.createdAt);
    }

    @Test
    @Transactional
    void testRepositoryUpdateAndRemove() {
        // Create warehouse first
        Warehouse warehouse = createTestWarehouse("COV-TEST-002", "AMSTERDAM-001", 100, 50);
        warehouseRepository.create(warehouse);

        // Update warehouse
        Warehouse found = warehouseRepository.findByBusinessUnitCode("COV-TEST-002");
        found.location = "ZWOLLE-001";
        found.capacity = 200;
        found.stock = 150;
        warehouseRepository.update(found);

        // Verify update
        Warehouse updated = warehouseRepository.findByBusinessUnitCode("COV-TEST-002");
        assertEquals("ZWOLLE-001", updated.location);
        assertEquals(200, updated.capacity);
        assertEquals(150, updated.stock);

        // Remove warehouse
        warehouseRepository.remove(updated);
        Warehouse removed = warehouseRepository.findByBusinessUnitCode("COV-TEST-002");
        assertNull(removed);
    }

    @Test
    @Transactional
    void testRepositorySearchMethods() {
        // Create test data
        warehouseRepository.create(createTestWarehouse("COV-LOC-001", "AMSTERDAM-001", 100, 50));
        warehouseRepository.create(createTestWarehouse("COV-LOC-002", "ZWOLLE-001", 200, 150));
        warehouseRepository.create(createTestWarehouse("COV-LOC-003", "AMSTERDAM-001", 300, 250));

        // Test findByLocation
        List<Warehouse> amsterdamWarehouses = warehouseRepository.findByLocation("AMSTERDAM-001");
        assertEquals(2, amsterdamWarehouses.size());

        // Test findByCapacityBetween
        List<Warehouse> capacityRange = warehouseRepository.findByCapacityBetween(150, 250);
        assertEquals(1, capacityRange.size()); // Only the 200 capacity warehouse

        // Test findByStockBetween
        List<Warehouse> stockRange = warehouseRepository.findByStockBetween(100, 200);
        assertEquals(1, stockRange.size()); // Only the 150 stock warehouse

        // Test findByArchived
        List<Warehouse> activeWarehouses = warehouseRepository.findByArchived(false);
        assertEquals(3, activeWarehouses.size());

        // Archive one warehouse
        Warehouse toArchive = warehouseRepository.findByBusinessUnitCode("COV-LOC-001");
        toArchive.archivedAt = LocalDateTime.now();
        warehouseRepository.update(toArchive);

        List<Warehouse> archivedWarehouses = warehouseRepository.findByArchived(true);
        assertEquals(1, archivedWarehouses.size());
    }

    @Test
    @Transactional
    void testCreateWarehouseUseCase() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "USE-CASE-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;

        // Create warehouse using use case
        createWarehouseUseCase.create(warehouse);

        // Verify warehouse was created
        Warehouse created = warehouseRepository.findByBusinessUnitCode("USE-CASE-001");
        assertNotNull(created);
        assertEquals("USE-CASE-001", created.businessUnitCode);
        assertEquals("AMSTERDAM-001", created.location);
        assertEquals(100, created.capacity);
        assertEquals(50, created.stock);
        assertNotNull(created.createdAt);
    }

    @Test
    @Transactional
    void testReplaceWarehouseUseCase() {
        // Create original warehouse
        Warehouse original = new Warehouse();
        original.businessUnitCode = "REPLACE-001";
        original.location = "AMSTERDAM-001";
        original.capacity = 100;
        original.stock = 50;
        createWarehouseUseCase.create(original);

        // Create replacement warehouse
        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "REPLACE-001";
        replacement.location = "ZWOLLE-001";
        replacement.capacity = 35;
        replacement.stock = 25;

        // Replace warehouse using use case
        replaceWarehouseUseCase.replace(replacement);

        // Verify warehouse was replaced
        Warehouse replaced = warehouseRepository.findByBusinessUnitCode("REPLACE-001");
        assertNotNull(replaced);
        assertEquals("ZWOLLE-001", replaced.location);
        assertEquals(35, replaced.capacity);
        assertEquals(25, replaced.stock);
        assertNotNull(replaced.createdAt); // Original createdAt should be preserved
    }

    @Test
    @Transactional
    void testArchiveWarehouseUseCase() {
        // Create warehouse to archive
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "ARCHIVE-001";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 100;
        warehouse.stock = 50;
        createWarehouseUseCase.create(warehouse);

        // Archive warehouse using use case
        archiveWarehouseUseCase.archive(warehouse);

        // Verify warehouse was archived
        Warehouse archived = warehouseRepository.findByBusinessUnitCode("ARCHIVE-001");
        assertNotNull(archived);
        assertNotNull(archived.archivedAt);
    }

    @Test
    @Transactional
    void testSearchWarehousesUseCase() {
        // Create test data
        createWarehouseUseCase.create(createTestWarehouse("SEARCH-001", "AMSTERDAM-001", 30, 20));
        createWarehouseUseCase.create(createTestWarehouse("SEARCH-002", "ZWOLLE-001", 35, 25));
        createWarehouseUseCase.create(createTestWarehouse("SEARCH-003", "AMSTERDAM-001", 25, 15));

        // Test search by location
        List<Warehouse> amsterdamResults = searchWarehousesUseCase.searchByLocation("AMSTERDAM-001");
        assertEquals(2, amsterdamResults.size());

        // Test search by capacity range
        List<Warehouse> capacityResults = searchWarehousesUseCase.searchByCapacityRange(28, 32);
        assertEquals(1, capacityResults.size()); // Only 30 capacity warehouse

        // Test search by stock range
        List<Warehouse> stockResults = searchWarehousesUseCase.searchByStockRange(18, 22);
        assertEquals(1, stockResults.size()); // Only 20 stock warehouse

        // Test search by archived status
        List<Warehouse> activeResults = searchWarehousesUseCase.searchByArchivedStatus(false);
        assertEquals(3, activeResults.size());
    }

    @Test
    @Transactional
    void testRepositoryGetAll() {
        // Create test data
        warehouseRepository.create(createTestWarehouse("GETALL-001", "AMSTERDAM-001", 100, 50));
        warehouseRepository.create(createTestWarehouse("GETALL-002", "ZWOLLE-001", 200, 150));
        warehouseRepository.create(createTestWarehouse("GETALL-003", "TILBURG-001", 300, 250));

        // Test getAll
        List<Warehouse> all = warehouseRepository.getAll();
        assertEquals(3, all.size());

        // Verify all warehouses are present
        assertTrue(all.stream().anyMatch(w -> "GETALL-001".equals(w.businessUnitCode)));
        assertTrue(all.stream().anyMatch(w -> "GETALL-002".equals(w.businessUnitCode)));
        assertTrue(all.stream().anyMatch(w -> "GETALL-003".equals(w.businessUnitCode)));
    }

    @Test
    @Transactional
    void testRepositoryErrorHandling() {
        // Test update non-existent warehouse
        Warehouse nonExistent = new Warehouse();
        nonExistent.businessUnitCode = "NON-EXISTENT";
        assertThrows(IllegalArgumentException.class, () -> warehouseRepository.update(nonExistent));

        // Test remove null warehouse
        assertThrows(IllegalArgumentException.class, () -> warehouseRepository.remove(null));

        // Test find with null business unit code
        Warehouse nullResult = warehouseRepository.findByBusinessUnitCode(null);
        assertNull(nullResult);

        // Test find with empty business unit code
        Warehouse emptyResult = warehouseRepository.findByBusinessUnitCode("");
        assertNull(emptyResult);
    }

    private Warehouse createTestWarehouse(String businessUnitCode, String location, int capacity, int stock) {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = businessUnitCode;
        warehouse.location = location;
        warehouse.capacity = capacity;
        warehouse.stock = stock;
        warehouse.createdAt = LocalDateTime.now();
        return warehouse;
    }
}
