package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@WithTestResource(H2DatabaseTestResource.class)
public class WarehouseRepositoryTest {

    @Inject
    WarehouseRepository warehouseRepository;

    private Warehouse testWarehouse;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up before each test
        warehouseRepository.deleteAll();
        
        // Create test warehouse
        testWarehouse = new Warehouse();
        testWarehouse.businessUnitCode = "TEST001";
        testWarehouse.location = "TestLocation";
        testWarehouse.capacity = 1000;
        testWarehouse.stock = 500;
        testWarehouse.createdAt = LocalDateTime.now();
        testWarehouse.archivedAt = null;
    }

    @Test
    @Transactional
    void testCreateWarehouse() {
        // When
        warehouseRepository.create(testWarehouse);

        // Then
        Warehouse created = warehouseRepository.findByBusinessUnitCode("TEST001");
        assertNotNull(created);
        assertEquals("TEST001", created.businessUnitCode);
        assertEquals("TestLocation", created.location);
        assertEquals(1000, created.capacity);
        assertEquals(500, created.stock);
    }

    @Test
    @Transactional
    void testGetAllWarehouses() {
        // Given
        warehouseRepository.create(testWarehouse);
        
        Warehouse secondWarehouse = new Warehouse();
        secondWarehouse.businessUnitCode = "TEST002";
        secondWarehouse.location = "TestLocation2";
        secondWarehouse.capacity = 2000;
        secondWarehouse.stock = 1500;
        secondWarehouse.createdAt = LocalDateTime.now();
        secondWarehouse.archivedAt = null;
        warehouseRepository.create(secondWarehouse);

        // When
        List<Warehouse> warehouses = warehouseRepository.getAll();

        // Then
        assertEquals(2, warehouses.size());
        assertTrue(warehouses.stream().anyMatch(w -> "TEST001".equals(w.businessUnitCode)));
        assertTrue(warehouses.stream().anyMatch(w -> "TEST002".equals(w.businessUnitCode)));
    }

    @Test
    @Transactional
    void testFindByBusinessUnitCode() {
        // Given
        warehouseRepository.create(testWarehouse);

        // When
        Warehouse found = warehouseRepository.findByBusinessUnitCode("TEST001");

        // Then
        assertNotNull(found);
        assertEquals("TEST001", found.businessUnitCode);
        assertEquals("TestLocation", found.location);
    }

    @Test
    @Transactional
    void testFindByBusinessUnitCodeNotFound() {
        // When
        Warehouse found = warehouseRepository.findByBusinessUnitCode("NONEXISTENT");

        // Then
        assertNull(found);
    }

    @Test
    @Transactional
    void testUpdateWarehouse() {
        // Given
        warehouseRepository.create(testWarehouse);
        Warehouse created = warehouseRepository.findByBusinessUnitCode("TEST001");
        
        // Update some fields
        created.location = "UpdatedLocation";
        created.capacity = 1500;
        created.stock = 800;

        // When
        warehouseRepository.update(created);

        // Then
        Warehouse updated = warehouseRepository.findByBusinessUnitCode("TEST001");
        assertNotNull(updated);
        assertEquals("UpdatedLocation", updated.location);
        assertEquals(1500, updated.capacity);
        assertEquals(800, updated.stock);
    }

    @Test
    @Transactional
    void testFindByLocation() {
        // Given
        warehouseRepository.create(testWarehouse);
        
        Warehouse secondWarehouse = new Warehouse();
        secondWarehouse.businessUnitCode = "TEST002";
        secondWarehouse.location = "TestLocation"; // Same location
        secondWarehouse.capacity = 2000;
        secondWarehouse.stock = 1500;
        secondWarehouse.createdAt = LocalDateTime.now();
        secondWarehouse.archivedAt = null;
        warehouseRepository.create(secondWarehouse);

        // When
        List<Warehouse> found = warehouseRepository.findByLocation("TestLocation");

        // Then
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(w -> "TEST001".equals(w.businessUnitCode)));
        assertTrue(found.stream().anyMatch(w -> "TEST002".equals(w.businessUnitCode)));
    }

    @Test
    @Transactional
    void testFindByLocationNotFound() {
        // Given
        warehouseRepository.create(testWarehouse);

        // When
        List<Warehouse> found = warehouseRepository.findByLocation("NonExistentLocation");

        // Then
        assertEquals(0, found.size());
    }

    @Test
    @Transactional
    void testFindByCapacityRange() {
        // Given
        warehouseRepository.create(testWarehouse);
        
        Warehouse secondWarehouse = new Warehouse();
        secondWarehouse.businessUnitCode = "TEST002";
        secondWarehouse.location = "TestLocation2";
        secondWarehouse.capacity = 1500;
        secondWarehouse.stock = 1500;
        secondWarehouse.createdAt = LocalDateTime.now();
        secondWarehouse.archivedAt = null;
        warehouseRepository.create(secondWarehouse);

        // When
        List<Warehouse> found = warehouseRepository.findByCapacityBetween(800, 1200);

        // Then
        assertEquals(1, found.size());
        assertEquals("TEST001", found.get(0).businessUnitCode);
    }

    @Test
    @Transactional
    void testFindByStockRange() {
        // Given
        warehouseRepository.create(testWarehouse);
        
        Warehouse secondWarehouse = new Warehouse();
        secondWarehouse.businessUnitCode = "TEST002";
        secondWarehouse.location = "TestLocation2";
        secondWarehouse.capacity = 2000;
        secondWarehouse.stock = 1200;
        secondWarehouse.createdAt = LocalDateTime.now();
        secondWarehouse.archivedAt = null;
        warehouseRepository.create(secondWarehouse);

        // When
        List<Warehouse> found = warehouseRepository.findByStockBetween(600, 1300);

        // Then
        assertEquals(1, found.size());
        assertEquals("TEST002", found.get(0).businessUnitCode);
    }

    @Test
    @Transactional
    void testFindByArchivedStatusActive() {
        // Given
        warehouseRepository.create(testWarehouse);
        
        Warehouse archivedWarehouse = new Warehouse();
        archivedWarehouse.businessUnitCode = "TEST002";
        archivedWarehouse.location = "TestLocation2";
        archivedWarehouse.capacity = 2000;
        archivedWarehouse.stock = 1500;
        archivedWarehouse.createdAt = LocalDateTime.now();
        archivedWarehouse.archivedAt = LocalDateTime.now(); // Archived
        warehouseRepository.create(archivedWarehouse);

        // When
        List<Warehouse> found = warehouseRepository.findByArchived(false);

        // Then
        assertEquals(1, found.size());
        assertEquals("TEST001", found.get(0).businessUnitCode);
    }

    @Test
    @Transactional
    void testFindByArchivedStatusArchived() {
        // Given
        warehouseRepository.create(testWarehouse);
        
        Warehouse archivedWarehouse = new Warehouse();
        archivedWarehouse.businessUnitCode = "TEST002";
        archivedWarehouse.location = "TestLocation2";
        archivedWarehouse.capacity = 2000;
        archivedWarehouse.stock = 1500;
        archivedWarehouse.createdAt = LocalDateTime.now();
        archivedWarehouse.archivedAt = LocalDateTime.now(); // Archived
        warehouseRepository.create(archivedWarehouse);

        // When
        List<Warehouse> found = warehouseRepository.findByArchived(true);

        // Then
        assertEquals(1, found.size());
        assertEquals("TEST002", found.get(0).businessUnitCode);
    }

    @Test
    @Transactional
    void testDeleteWarehouse() {
        // Given
        warehouseRepository.create(testWarehouse);
        assertEquals(1, warehouseRepository.getAll().size());

        // When
        warehouseRepository.remove(testWarehouse);

        // Then
        assertEquals(0, warehouseRepository.getAll().size());
    }

    @Test
    @Transactional
    void testDeleteAll() {
        // Given
        warehouseRepository.create(testWarehouse);
        
        Warehouse secondWarehouse = new Warehouse();
        secondWarehouse.businessUnitCode = "TEST002";
        secondWarehouse.location = "TestLocation2";
        secondWarehouse.capacity = 2000;
        secondWarehouse.stock = 1500;
        secondWarehouse.createdAt = LocalDateTime.now();
        secondWarehouse.archivedAt = null;
        warehouseRepository.create(secondWarehouse);

        assertEquals(2, warehouseRepository.getAll().size());

        // When
        warehouseRepository.deleteAll();

        // Then
        assertEquals(0, warehouseRepository.getAll().size());
    }
}
