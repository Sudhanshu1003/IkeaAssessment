package com.fulfilment.application.monolith.warehouses.integration;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.warehouse.api.beans.Warehouse;
import com.warehouse.api.WarehouseResource;
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
public class WarehouseIntegrationTest {

    @Inject
    WarehouseResource warehouseResource;

    @Inject
    WarehouseRepository warehouseRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up before each test
        warehouseRepository.deleteAll();
    }

    @Test
    @Transactional
    void testCreateWarehouseIntegration() {
        // Given
        Warehouse newWarehouse = new Warehouse();
        newWarehouse.setBusinessUnitCode("INT001");
        newWarehouse.setLocation("AMSTERDAM-001");
        newWarehouse.setCapacity(80);
        newWarehouse.setStock(40);

        // When
        Warehouse created = warehouseResource.createANewWarehouseUnit(newWarehouse);

        // Then
        assertNotNull(created);
        assertEquals("INT001", created.getBusinessUnitCode());
        assertEquals("AMSTERDAM-001", created.getLocation());
        assertEquals(80, created.getCapacity());
        assertEquals(40, created.getStock());

        // Verify in database
        com.fulfilment.application.monolith.warehouses.domain.models.Warehouse dbWarehouse = warehouseRepository.findByBusinessUnitCode("INT001");
        assertNotNull(dbWarehouse);
        assertEquals("INT001", dbWarehouse.businessUnitCode);
        assertEquals("AMSTERDAM-001", dbWarehouse.location);
        assertEquals(80, dbWarehouse.capacity);
        assertEquals(40, dbWarehouse.stock);
    }

    @Test
    @Transactional
    void testListAllWarehousesIntegration() {
        // Given
        createTestWarehouse("WH001", "AMSTERDAM-001", 80, 40);
        createTestWarehouse("WH002", "ZWOLLE-001", 35, 20);

        // When
        List<Warehouse> warehouses = warehouseResource.listAllWarehousesUnits();

        // Then
        assertEquals(2, warehouses.size());
        assertTrue(warehouses.stream().anyMatch(w -> "WH001".equals(w.getBusinessUnitCode())));
        assertTrue(warehouses.stream().anyMatch(w -> "WH002".equals(w.getBusinessUnitCode())));
    }

    @Test
    @Transactional
    void testGetWarehouseByIdIntegration() {
        // Given
        createTestWarehouse("WH001", "AMSTERDAM-001", 80, 40);

        // When
        Warehouse found = warehouseResource.getAWarehouseUnitByID("WH001");

        // Then
        assertNotNull(found);
        assertEquals("WH001", found.getBusinessUnitCode());
        assertEquals("AMSTERDAM-001", found.getLocation());
        assertEquals(80, found.getCapacity());
        assertEquals(40, found.getStock());
    }

    @Test
    @Transactional
    void testArchiveWarehouseIntegration() {
        // Given
        createTestWarehouse("WH001", "AMSTERDAM-001", 80, 40);

        // When
        warehouseResource.archiveAWarehouseUnitByID("WH001");

        // Then
        com.fulfilment.application.monolith.warehouses.domain.models.Warehouse archived = warehouseRepository.findByBusinessUnitCode("WH001");
        assertNotNull(archived.archivedAt);
    }

    @Test
    @Transactional
    void testReplaceWarehouseIntegration() {
        // Given
        createTestWarehouse("WH001", "AMSTERDAM-001", 80, 40);

        Warehouse replacement = new Warehouse();
        replacement.setBusinessUnitCode("WH001");
        replacement.setLocation("ZWOLLE-001");
        replacement.setCapacity(35);
        replacement.setStock(20);

        // When
        warehouseResource.replaceTheCurrentActiveWarehouse("WH001", replacement);

        // Then
        com.fulfilment.application.monolith.warehouses.domain.models.Warehouse updated = warehouseRepository.findByBusinessUnitCode("WH001");
        assertEquals("ZWOLLE-001", updated.location);
        assertEquals(35, updated.capacity);
        assertEquals(20, updated.stock);
    }

    private void createTestWarehouse(String businessUnitCode, String location, int capacity, int stock) {
        Warehouse warehouse = new Warehouse();
        warehouse.setBusinessUnitCode(businessUnitCode);
        warehouse.setLocation(location);
        warehouse.setCapacity(capacity);
        warehouse.setStock(stock);
        warehouseResource.createANewWarehouseUnit(warehouse);
    }
}
