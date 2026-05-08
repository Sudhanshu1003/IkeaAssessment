package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.usecases.SearchWarehousesUseCase;
import com.warehouse.api.beans.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class WarehouseResourceImplTest {

    @InjectMock
    WarehouseRepository warehouseRepository;

    @InjectMock
    CreateWarehouseOperation createWarehouseOperation;

    @InjectMock
    ArchiveWarehouseOperation archiveWarehouseOperation;

    @InjectMock
    ReplaceWarehouseOperation replaceWarehouseOperation;

    @InjectMock
    SearchWarehousesUseCase searchWarehousesUseCase;

    @Inject
    WarehouseResourceImpl warehouseResource;

    private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse domainWarehouse;
    private com.warehouse.api.beans.Warehouse apiWarehouse;

    @BeforeEach
    void setUp() {
        domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
        domainWarehouse.businessUnitCode = "WH001";
        domainWarehouse.location = "Stockholm";
        domainWarehouse.capacity = 1000;
        domainWarehouse.stock = 500;
        domainWarehouse.createdAt = LocalDateTime.now();
        domainWarehouse.archivedAt = null;

        apiWarehouse = new com.warehouse.api.beans.Warehouse();
        apiWarehouse.setBusinessUnitCode("WH001");
        apiWarehouse.setLocation("Stockholm");
        apiWarehouse.setCapacity(1000);
        apiWarehouse.setStock(500);
    }

    @Test
    void testListAllWarehousesUnits() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> warehouses = Arrays.asList(domainWarehouse);
        when(warehouseRepository.getAll()).thenReturn(warehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.listAllWarehousesUnits();

        // Then
        assertEquals(1, result.size());
        com.warehouse.api.beans.Warehouse resultWarehouse = result.get(0);
        assertEquals("WH001", resultWarehouse.getBusinessUnitCode());
        assertEquals("Stockholm", resultWarehouse.getLocation());
        assertEquals(1000, resultWarehouse.getCapacity());
        assertEquals(500, resultWarehouse.getStock());
        verify(warehouseRepository, times(1)).getAll();
    }

    @Test
    void testListAllWarehousesUnitsEmpty() {
        // Given
        when(warehouseRepository.getAll()).thenReturn(Collections.emptyList());

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.listAllWarehousesUnits();

        // Then
        assertEquals(0, result.size());
        verify(warehouseRepository, times(1)).getAll();
    }

    @Test
    void testCreateANewWarehouseUnit() {
        // Given
        doNothing().when(createWarehouseOperation).create(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));

        // When
        com.warehouse.api.beans.Warehouse result = warehouseResource.createANewWarehouseUnit(apiWarehouse);

        // Then
        assertNotNull(result);
        assertEquals("WH001", result.getBusinessUnitCode());
        assertEquals("Stockholm", result.getLocation());
        assertEquals(1000, result.getCapacity());
        assertEquals(500, result.getStock());
        verify(createWarehouseOperation, times(1)).create(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));
    }

    @Test
    void testCreateANewWarehouseUnitWithNullStock() {
        // Given
        apiWarehouse.setStock(null);
        doNothing().when(createWarehouseOperation).create(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));

        // When
        com.warehouse.api.beans.Warehouse result = warehouseResource.createANewWarehouseUnit(apiWarehouse);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getStock()); // Should default to 0
        verify(createWarehouseOperation, times(1)).create(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));
    }

    @Test
    void testCreateANewWarehouseUnitValidationError() {
        // Given
        doThrow(new IllegalArgumentException("Invalid warehouse data"))
            .when(createWarehouseOperation).create(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));

        // When & Then
        WebApplicationException exception = assertThrows(WebApplicationException.class, 
            () -> warehouseResource.createANewWarehouseUnit(apiWarehouse));
        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("Invalid warehouse data", exception.getMessage());
        verify(createWarehouseOperation, times(1)).create(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));
    }

    @Test
    void testGetAWarehouseUnitByID() {
        // Given
        when(warehouseRepository.findByBusinessUnitCode("WH001")).thenReturn(domainWarehouse);

        // When
        com.warehouse.api.beans.Warehouse result = warehouseResource.getAWarehouseUnitByID("WH001");

        // Then
        assertNotNull(result);
        assertEquals("WH001", result.getBusinessUnitCode());
        assertEquals("Stockholm", result.getLocation());
        assertEquals(1000, result.getCapacity());
        assertEquals(500, result.getStock());
        verify(warehouseRepository, times(1)).findByBusinessUnitCode("WH001");
    }

    @Test
    void testGetAWarehouseUnitByIDNotFound() {
        // Given
        when(warehouseRepository.findByBusinessUnitCode("NONEXISTENT")).thenReturn(null);

        // When & Then
        WebApplicationException exception = assertThrows(WebApplicationException.class, 
            () -> warehouseResource.getAWarehouseUnitByID("NONEXISTENT"));
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("Warehouse with business unit code 'NONEXISTENT' not found", exception.getMessage());
        verify(warehouseRepository, times(1)).findByBusinessUnitCode("NONEXISTENT");
    }

    @Test
    void testArchiveAWarehouseUnitByID() {
        // Given
        when(warehouseRepository.findByBusinessUnitCode("WH001")).thenReturn(domainWarehouse);
        doNothing().when(archiveWarehouseOperation).archive(domainWarehouse);

        // When
        warehouseResource.archiveAWarehouseUnitByID("WH001");

        // Then
        verify(warehouseRepository, times(1)).findByBusinessUnitCode("WH001");
        verify(archiveWarehouseOperation, times(1)).archive(domainWarehouse);
    }

    @Test
    void testArchiveAWarehouseUnitByIDNotFound() {
        // Given
        when(warehouseRepository.findByBusinessUnitCode("NONEXISTENT")).thenReturn(null);

        // When & Then
        WebApplicationException exception = assertThrows(WebApplicationException.class, 
            () -> warehouseResource.archiveAWarehouseUnitByID("NONEXISTENT"));
        assertEquals(404, exception.getResponse().getStatus());
        assertTrue(exception.getMessage().contains("not found"));
        verify(warehouseRepository, times(1)).findByBusinessUnitCode("NONEXISTENT");
        verify(archiveWarehouseOperation, never()).archive(any());
    }

    @Test
    void testArchiveAWarehouseUnitByIDValidationError() {
        // Given
        when(warehouseRepository.findByBusinessUnitCode("WH001")).thenReturn(domainWarehouse);
        doThrow(new IllegalArgumentException("Cannot archive warehouse"))
            .when(archiveWarehouseOperation).archive(domainWarehouse);

        // When & Then
        WebApplicationException exception = assertThrows(WebApplicationException.class, 
            () -> warehouseResource.archiveAWarehouseUnitByID("WH001"));
        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("Cannot archive warehouse", exception.getMessage());
        verify(archiveWarehouseOperation, times(1)).archive(domainWarehouse);
    }

    @Test
    void testReplaceTheCurrentActiveWarehouse() {
        // Given
        com.fulfilment.application.monolith.warehouses.domain.models.Warehouse updatedWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
        updatedWarehouse.businessUnitCode = "WH001";
        updatedWarehouse.location = "UpdatedLocation";
        updatedWarehouse.capacity = 1500;
        updatedWarehouse.stock = 800;
        updatedWarehouse.createdAt = LocalDateTime.now();
        updatedWarehouse.archivedAt = null;

        when(warehouseRepository.findByBusinessUnitCode("WH001")).thenReturn(updatedWarehouse);
        doNothing().when(replaceWarehouseOperation).replace(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));

        // When
        com.warehouse.api.beans.Warehouse result = warehouseResource.replaceTheCurrentActiveWarehouse("WH001", apiWarehouse);

        // Then
        assertNotNull(result);
        assertEquals("WH001", result.getBusinessUnitCode());
        assertEquals("UpdatedLocation", result.getLocation());
        assertEquals(1500, result.getCapacity());
        assertEquals(800, result.getStock());
        verify(replaceWarehouseOperation, times(1)).replace(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));
        verify(warehouseRepository, times(1)).findByBusinessUnitCode("WH001");
    }

    @Test
    void testReplaceTheCurrentActiveWarehouseValidationError() {
        // Given
        doThrow(new IllegalArgumentException("Cannot replace warehouse"))
            .when(replaceWarehouseOperation).replace(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));

        // When & Then
        WebApplicationException exception = assertThrows(WebApplicationException.class, 
            () -> warehouseResource.replaceTheCurrentActiveWarehouse("WH001", apiWarehouse));
        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("Cannot replace warehouse", exception.getMessage());
        verify(replaceWarehouseOperation, times(1)).replace(any(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse.class));
    }

    @Test
    void testSearchWarehousesByLocation() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByLocation("Stockholm")).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByLocation("Stockholm");

        // Then
        assertEquals(1, result.size());
        assertEquals("WH001", result.get(0).getBusinessUnitCode());
        verify(searchWarehousesUseCase, times(1)).searchByLocation("Stockholm");
    }

    @Test
    void testSearchWarehousesByLocationEmpty() {
        // Given
        when(searchWarehousesUseCase.searchByLocation("Unknown")).thenReturn(List.of());

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByLocation("Unknown");

        // Then
        assertTrue(result.isEmpty());
        verify(searchWarehousesUseCase, times(1)).searchByLocation("Unknown");
    }

    @Test
    void testSearchWarehousesByCapacityRange() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByCapacityRange(500, 1500)).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByCapacityRange(500, 1500);

        // Then
        assertEquals(1, result.size());
        assertEquals("WH001", result.get(0).getBusinessUnitCode());
        verify(searchWarehousesUseCase, times(1)).searchByCapacityRange(500, 1500);
    }

    @Test
    void testSearchWarehousesByCapacityRangeNullMin() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByCapacityRange(null, 1500)).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByCapacityRange(null, 1500);

        // Then
        assertEquals(1, result.size());
        verify(searchWarehousesUseCase, times(1)).searchByCapacityRange(null, 1500);
    }

    @Test
    void testSearchWarehousesByCapacityRangeNullMax() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByCapacityRange(500, null)).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByCapacityRange(500, null);

        // Then
        assertEquals(1, result.size());
        verify(searchWarehousesUseCase, times(1)).searchByCapacityRange(500, null);
    }

    @Test
    void testSearchWarehousesByStockRange() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByStockRange(200, 800)).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByStockRange(200, 800);

        // Then
        assertEquals(1, result.size());
        assertEquals("WH001", result.get(0).getBusinessUnitCode());
        verify(searchWarehousesUseCase, times(1)).searchByStockRange(200, 800);
    }

    @Test
    void testSearchWarehousesByStockRangeNullMin() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByStockRange(null, 800)).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByStockRange(null, 800);

        // Then
        assertEquals(1, result.size());
        verify(searchWarehousesUseCase, times(1)).searchByStockRange(null, 800);
    }

    @Test
    void testSearchWarehousesByStockRangeNullMax() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByStockRange(200, null)).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByStockRange(200, null);

        // Then
        assertEquals(1, result.size());
        verify(searchWarehousesUseCase, times(1)).searchByStockRange(200, null);
    }

    @Test
    void testSearchWarehousesByArchivedStatus() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByArchivedStatus(false)).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByArchivedStatus(false);

        // Then
        assertEquals(1, result.size());
        assertEquals("WH001", result.get(0).getBusinessUnitCode());
        verify(searchWarehousesUseCase, times(1)).searchByArchivedStatus(false);
    }

    @Test
    void testSearchWarehousesByArchivedStatusTrue() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByArchivedStatus(true)).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByArchivedStatus(true);

        // Then
        assertEquals(1, result.size());
        verify(searchWarehousesUseCase, times(1)).searchByArchivedStatus(true);
    }

    @Test
    void testSearchWarehousesByArchivedStatusNull() {
        // Given
        List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> domainWarehouses = List.of(domainWarehouse);
        when(searchWarehousesUseCase.searchByArchivedStatus(null)).thenReturn(domainWarehouses);

        // When
        List<com.warehouse.api.beans.Warehouse> result = warehouseResource.searchWarehousesByArchivedStatus(null);

        // Then
        assertEquals(1, result.size());
        verify(searchWarehousesUseCase, times(1)).searchByArchivedStatus(null);
    }

    @Test
    void testCreateANewWarehouseUnitWithNullWarehouse() {
        // Given
        com.warehouse.api.beans.Warehouse nullWarehouse = null;

        // When & Then
        assertThrows(NullPointerException.class, 
            () -> warehouseResource.createANewWarehouseUnit(nullWarehouse));
    }

    @Test
    void testGetAWarehouseUnitByIDWithNullId() {
        // Given
        when(warehouseRepository.findByBusinessUnitCode(null)).thenReturn(null);

        // When & Then
        WebApplicationException exception = assertThrows(WebApplicationException.class, 
            () -> warehouseResource.getAWarehouseUnitByID(null));
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("Warehouse with business unit code 'null' not found", exception.getMessage());
        verify(warehouseRepository, times(1)).findByBusinessUnitCode(null);
    }

    @Test
    void testGetAWarehouseUnitByIDWithEmptyId() {
        // Given
        when(warehouseRepository.findByBusinessUnitCode("")).thenReturn(null);

        // When & Then
        WebApplicationException exception = assertThrows(WebApplicationException.class, 
            () -> warehouseResource.getAWarehouseUnitByID(""));
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("Warehouse with business unit code '' not found", exception.getMessage());
        verify(warehouseRepository, times(1)).findByBusinessUnitCode("");
    }

    @Test
    void testArchiveAWarehouseUnitByIDWithNullId() {
        // Given
        when(warehouseRepository.findByBusinessUnitCode(null)).thenReturn(null);

        // When & Then
        WebApplicationException exception = assertThrows(WebApplicationException.class, 
            () -> warehouseResource.archiveAWarehouseUnitByID(null));
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("Warehouse with business unit code 'null' not found", exception.getMessage());
        verify(warehouseRepository, times(1)).findByBusinessUnitCode(null);
        verify(archiveWarehouseOperation, never()).archive(any());
    }

    @Test
    void testReplaceTheCurrentActiveWarehouseWithNullId() {
        // Given
        when(warehouseRepository.findByBusinessUnitCode(null)).thenReturn(null);

        // When & Then
        assertThrows(NullPointerException.class, 
            () -> warehouseResource.replaceTheCurrentActiveWarehouse(null, apiWarehouse));
    }

    @Test
    void testReplaceTheCurrentActiveWarehouseWithNullWarehouse() {
        // Given
        com.warehouse.api.beans.Warehouse nullWarehouse = null;

        // When & Then
        assertThrows(NullPointerException.class, 
            () -> warehouseResource.replaceTheCurrentActiveWarehouse("WH001", nullWarehouse));
    }

    }
