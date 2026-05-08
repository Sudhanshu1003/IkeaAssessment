package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@WithTestResource(H2DatabaseTestResource.class)
public class DbWarehouseTest {

    @Test
    @Transactional
    void testDbWarehouseEntityMethods() {
        // Test domain model conversion
        Warehouse domainWarehouse = new Warehouse();
        domainWarehouse.businessUnitCode = "TEST-001";
        domainWarehouse.location = "AMSTERDAM-001";
        domainWarehouse.capacity = 30;
        domainWarehouse.stock = 25;
        domainWarehouse.createdAt = LocalDateTime.now();

        // Test DbWarehouse constructor and methods
        DbWarehouse dbWarehouse = new DbWarehouse();
        dbWarehouse.businessUnitCode = domainWarehouse.businessUnitCode;
        dbWarehouse.location = domainWarehouse.location;
        dbWarehouse.capacity = domainWarehouse.capacity;
        dbWarehouse.stock = domainWarehouse.stock;
        dbWarehouse.createdAt = domainWarehouse.createdAt;
        dbWarehouse.version = 1L; // Set default version

        // Test getters
        assertEquals("TEST-001", dbWarehouse.businessUnitCode);
        assertEquals("AMSTERDAM-001", dbWarehouse.location);
        assertEquals(30, dbWarehouse.capacity);
        assertEquals(25, dbWarehouse.stock);
        assertNotNull(dbWarehouse.createdAt);
        assertEquals(1L, dbWarehouse.version);

        // Test toWarehouse method
        Warehouse converted = dbWarehouse.toWarehouse();
        assertEquals("TEST-001", converted.businessUnitCode);
        assertEquals("AMSTERDAM-001", converted.location);
        assertEquals(30, converted.capacity);
        assertEquals(25, converted.stock);
        assertNotNull(converted.createdAt);
    }

    @Test
    @Transactional
    void testDbWarehouseWithArchivedAt() {
        // Test archived warehouse
        DbWarehouse dbWarehouse = new DbWarehouse();
        dbWarehouse.businessUnitCode = "ARCHIVED-001";
        dbWarehouse.location = "AMSTERDAM-001";
        dbWarehouse.capacity = 30;
        dbWarehouse.stock = 25;
        dbWarehouse.createdAt = LocalDateTime.now();
        dbWarehouse.archivedAt = LocalDateTime.now();
        dbWarehouse.version = 1L;

        // Test archived warehouse conversion
        Warehouse converted = dbWarehouse.toWarehouse();
        assertEquals("ARCHIVED-001", converted.businessUnitCode);
        assertNotNull(converted.archivedAt);
    }
}
