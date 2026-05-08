package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import jakarta.inject.Inject;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CreateWarehouseUseCaseTest {

    @InjectMock
    WarehouseStore warehouseStore;

    @InjectMock
    LocationResolver locationResolver;

    @Inject
    CreateWarehouseUseCase createWarehouseUseCase;

    private Warehouse testWarehouse;

    @BeforeEach
    void setUp() {
        testWarehouse = new Warehouse();
        testWarehouse.businessUnitCode = "WH001";
        testWarehouse.location = "Stockholm";
        testWarehouse.capacity = 1000;
        testWarehouse.stock = 500;
        testWarehouse.createdAt = LocalDateTime.now();
        testWarehouse.archivedAt = null;
    }

    @Test
    void testCreateWarehouse() {
        // Given
        Location location = new Location("Stockholm", 10, 2000);
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(location);
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // When
        createWarehouseUseCase.create(testWarehouse);

        // Then
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, times(1)).create(testWarehouse);
        assertNotNull(testWarehouse.createdAt);
    }

    @Test
    void testCreateWarehouseWithExistingCode() {
        // Given
        Warehouse existingWarehouse = new Warehouse();
        existingWarehouse.businessUnitCode = "WH001";
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(existingWarehouse);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> createWarehouseUseCase.create(testWarehouse));
        assertEquals("Warehouse with business unit code 'WH001' already exists", exception.getMessage());
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, never()).resolveByIdentifier(any());
        verify(warehouseStore, never()).create(any(Warehouse.class));
    }

    @Test
    void testCreateWarehouseWithInvalidLocation() {
        // Given
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> createWarehouseUseCase.create(testWarehouse));
        assertEquals("Location 'Stockholm' is not valid", exception.getMessage());
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, never()).create(any(Warehouse.class));
    }

    @Test
    void testCreateWarehouseWithCapacityExceedingLocationMax() {
        // Given
        Location location = new Location("Stockholm", 10, 500); // Less than warehouse capacity
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(location);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> createWarehouseUseCase.create(testWarehouse));
        assertTrue(exception.getMessage().contains("exceeds location max capacity"));
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, never()).create(any(Warehouse.class));
    }

    @Test
    void testCreateWarehouseWithStockExceedingCapacity() {
        // Given
        Location location = new Location("Stockholm", 10, 2000);
        
        testWarehouse.stock = 1500; // More than capacity (1000)
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(location);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> createWarehouseUseCase.create(testWarehouse));
        assertTrue(exception.getMessage().contains("exceeds warehouse capacity"));
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, never()).create(any(Warehouse.class));
    }

    @Test
    void testCreateWarehouseWithValidData() {
        // Given
        Location location = new Location("Gothenburg", 10, 3000);
        
        testWarehouse.businessUnitCode = "WH002";
        testWarehouse.location = "Gothenburg";
        testWarehouse.capacity = 2000;
        testWarehouse.stock = 1000;
        
        when(warehouseStore.findByBusinessUnitCode("WH002")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Gothenburg")).thenReturn(location);
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // When
        createWarehouseUseCase.create(testWarehouse);

        // Then
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH002");
        verify(locationResolver, times(1)).resolveByIdentifier("Gothenburg");
        verify(warehouseStore, times(1)).create(testWarehouse);
        assertNotNull(testWarehouse.createdAt);
    }

    @Test
    void testCreateWarehouseWithNegativeCapacity() {
        // Given
        Location location = new Location("Stockholm", 10, 2000);
        testWarehouse.capacity = -100; // Negative capacity - should pass validation
        testWarehouse.stock = -200; // Stock must be <= capacity
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(location);
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // When
        createWarehouseUseCase.create(testWarehouse);

        // Then - negative capacity doesn't trigger validation in current implementation
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, times(1)).create(testWarehouse);
        assertNotNull(testWarehouse.createdAt);
    }

    @Test
    void testCreateWarehouseWithZeroCapacity() {
        // Given
        Location location = new Location("Stockholm", 10, 2000);
        testWarehouse.capacity = 0; // Zero capacity - should pass validation
        testWarehouse.stock = 0; // Stock must be <= capacity
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(location);
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // When
        createWarehouseUseCase.create(testWarehouse);

        // Then - zero capacity doesn't trigger validation in current implementation
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, times(1)).create(testWarehouse);
        assertNotNull(testWarehouse.createdAt);
    }

    @Test
    void testCreateWarehouseWithNegativeStock() {
        // Given
        Location location = new Location("Stockholm", 10, 2000);
        testWarehouse.stock = -50; // Negative stock - should pass validation
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(location);
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // When
        createWarehouseUseCase.create(testWarehouse);

        // Then - negative stock doesn't trigger validation in current implementation
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, times(1)).create(testWarehouse);
        assertNotNull(testWarehouse.createdAt);
    }

    @Test
    void testCreateWarehouseWithStockEqualToCapacity() {
        // Given
        Location location = new Location("Stockholm", 10, 2000);
        testWarehouse.capacity = 1000;
        testWarehouse.stock = 1000; // Stock equals capacity
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(location);
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // When
        createWarehouseUseCase.create(testWarehouse);

        // Then
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, times(1)).create(testWarehouse);
        assertNotNull(testWarehouse.createdAt);
    }

    @Test
    void testCreateWarehouseWithNullBusinessUnitCode() {
        // Given
        testWarehouse.businessUnitCode = null;
        
        when(warehouseStore.findByBusinessUnitCode(null)).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(new Location("Stockholm", 10, 2000));
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // When
        createWarehouseUseCase.create(testWarehouse);

        // Then - null business unit code doesn't trigger validation in current implementation
        verify(warehouseStore, times(1)).findByBusinessUnitCode(null);
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, times(1)).create(testWarehouse);
        assertNotNull(testWarehouse.createdAt);
    }

    @Test
    void testCreateWarehouseWithEmptyBusinessUnitCode() {
        // Given
        testWarehouse.businessUnitCode = "";
        
        when(warehouseStore.findByBusinessUnitCode("")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(new Location("Stockholm", 10, 2000));
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // When
        createWarehouseUseCase.create(testWarehouse);

        // Then
        verify(warehouseStore, times(1)).findByBusinessUnitCode("");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, times(1)).create(testWarehouse);
        assertNotNull(testWarehouse.createdAt);
    }

    @Test
    void testCreateWarehouseWithNullLocation() {
        // Given
        testWarehouse.location = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> createWarehouseUseCase.create(testWarehouse));
        assertTrue(exception.getMessage().contains("Location 'null' is not valid"));
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier(null);
        verify(warehouseStore, never()).create(any(Warehouse.class));
    }

    @Test
    void testCreateWarehouseWithEmptyLocation() {
        // Given
        testWarehouse.location = "";
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("")).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> createWarehouseUseCase.create(testWarehouse));
        assertTrue(exception.getMessage().contains("Location '' is not valid"));
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("");
        verify(warehouseStore, never()).create(any(Warehouse.class));
    }

    @Test
    void testCreateWarehouseWithMaxCapacityAtLocationLimit() {
        // Given
        Location location = new Location("Stockholm", 10, 1000);
        testWarehouse.capacity = 1000; // Exactly at location max
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(location);
        doNothing().when(warehouseStore).create(any(Warehouse.class));

        // When
        createWarehouseUseCase.create(testWarehouse);

        // Then
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, times(1)).create(testWarehouse);
        assertNotNull(testWarehouse.createdAt);
    }

    @Test
    void testCreateWarehouseWithExistingWarehouseDifferentCase() {
        // Given
        Warehouse existingWarehouse = new Warehouse();
        existingWarehouse.businessUnitCode = "wh001"; // Different case
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(existingWarehouse);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> createWarehouseUseCase.create(testWarehouse));
        assertTrue(exception.getMessage().contains("already exists"));
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, never()).resolveByIdentifier(anyString());
        verify(warehouseStore, never()).create(any(Warehouse.class));
    }

    @Test
    void testCreateWarehouseWithLocationResolverException() {
        // Given
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenThrow(new RuntimeException("Location service unavailable"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> createWarehouseUseCase.create(testWarehouse));
        assertEquals("Location service unavailable", exception.getMessage());
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, never()).create(any(Warehouse.class));
    }

    @Test
    void testCreateWarehouseWithWarehouseStoreException() {
        // Given
        Location location = new Location("Stockholm", 10, 2000);
        
        when(warehouseStore.findByBusinessUnitCode("WH001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("Stockholm")).thenReturn(location);
        doThrow(new RuntimeException("Database error")).when(warehouseStore).create(any(Warehouse.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> createWarehouseUseCase.create(testWarehouse));
        assertEquals("Database error", exception.getMessage());
        verify(warehouseStore, times(1)).findByBusinessUnitCode("WH001");
        verify(locationResolver, times(1)).resolveByIdentifier("Stockholm");
        verify(warehouseStore, times(1)).create(testWarehouse);
    }
}
