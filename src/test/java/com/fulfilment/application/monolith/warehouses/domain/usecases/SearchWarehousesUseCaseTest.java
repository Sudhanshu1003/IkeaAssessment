package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class SearchWarehousesUseCaseTest {

    @InjectMock
    WarehouseStore warehouseStore;

    @Inject
    SearchWarehousesUseCase searchWarehousesUseCase;

    private Warehouse warehouse1;
    private Warehouse warehouse2;
    private Warehouse warehouse3;

    @BeforeEach
    void setUp() {
        warehouse1 = new Warehouse();
        warehouse1.businessUnitCode = "WH001";
        warehouse1.location = "Stockholm";
        warehouse1.capacity = 1000;
        warehouse1.stock = 500;
        warehouse1.createdAt = LocalDateTime.now().minusDays(10);
        warehouse1.archivedAt = null;

        warehouse2 = new Warehouse();
        warehouse2.businessUnitCode = "WH002";
        warehouse2.location = "Gothenburg";
        warehouse2.capacity = 2000;
        warehouse2.stock = 1500;
        warehouse2.createdAt = LocalDateTime.now().minusDays(5);
        warehouse2.archivedAt = null;

        warehouse3 = new Warehouse();
        warehouse3.businessUnitCode = "WH003";
        warehouse3.location = "Stockholm";
        warehouse3.capacity = 1500;
        warehouse3.stock = 800;
        warehouse3.createdAt = LocalDateTime.now().minusDays(2);
        warehouse3.archivedAt = LocalDateTime.now().minusDays(1); // Archived
    }

    @Test
    void testSearchByLocation() {
        // Given
        List<Warehouse> warehouses = Arrays.asList(warehouse1, warehouse2, warehouse3);
        when(warehouseStore.findByLocation("Stockholm")).thenReturn(Arrays.asList(warehouse1, warehouse3));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByLocation("Stockholm");

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(warehouse1));
        assertTrue(result.contains(warehouse3));
        verify(warehouseStore, times(1)).findByLocation("Stockholm");
    }

    @Test
    void testSearchByLocationNotFound() {
        // Given
        when(warehouseStore.findByLocation("Malmö")).thenReturn(Collections.emptyList());

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByLocation("Malmö");

        // Then
        assertEquals(0, result.size());
        verify(warehouseStore, times(1)).findByLocation("Malmö");
    }

    @Test
    void testSearchByCapacityRange() {
        // Given
        List<Warehouse> warehouses = Arrays.asList(warehouse1, warehouse3);
        when(warehouseStore.findByCapacityBetween(1000, 1500)).thenReturn(Arrays.asList(warehouse1, warehouse3));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByCapacityRange(1000, 1500);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(warehouse1));
        assertTrue(result.contains(warehouse3));
        verify(warehouseStore, times(1)).findByCapacityBetween(1000, 1500);
    }

    @Test
    void testSearchByCapacityRangeNoResults() {
        // Given
        when(warehouseStore.findByCapacityBetween(3000, 4000)).thenReturn(Collections.emptyList());

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByCapacityRange(3000, 4000);

        // Then
        assertEquals(0, result.size());
        verify(warehouseStore, times(1)).findByCapacityBetween(3000, 4000);
    }

    @Test
    void testSearchByStockRange() {
        // Given
        when(warehouseStore.findByStockBetween(600, 1600)).thenReturn(Arrays.asList(warehouse2, warehouse3));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByStockRange(600, 1600);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(warehouse2));
        assertTrue(result.contains(warehouse3));
        verify(warehouseStore, times(1)).findByStockBetween(600, 1600);
    }

    @Test
    void testSearchByStockRangeNoResults() {
        // Given
        when(warehouseStore.findByStockBetween(2000, 3000)).thenReturn(Collections.emptyList());

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByStockRange(2000, 3000);

        // Then
        assertEquals(0, result.size());
        verify(warehouseStore, times(1)).findByStockBetween(2000, 3000);
    }

    @Test
    void testSearchByArchivedStatusActive() {
        // Given
        when(warehouseStore.findByArchived(false)).thenReturn(Arrays.asList(warehouse1, warehouse2));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByArchivedStatus(false);

        // Then
        assertEquals(2, result.size());
        verify(warehouseStore, times(1)).findByArchived(false);
    }

    @Test
    void testSearchByArchivedStatusTrue() {
        // Given
        when(warehouseStore.findByArchived(true)).thenReturn(Arrays.asList(warehouse3));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByArchivedStatus(true);

        // Then
        assertEquals(1, result.size());
        assertEquals("WH003", result.get(0).businessUnitCode);
        verify(warehouseStore, times(1)).findByArchived(true);
    }

    @Test
    void testSearchByArchivedStatusNoResults() {
        // Given
        when(warehouseStore.findByArchived(false)).thenReturn(Collections.emptyList());

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByArchivedStatus(false);

        // Then
        assertTrue(result.isEmpty());
        verify(warehouseStore, times(1)).findByArchived(false);
    }

    @Test
    void testSearchByArchivedStatusNull() {
        // Given
        when(warehouseStore.findByArchived(null)).thenReturn(Arrays.asList(warehouse1, warehouse2, warehouse3));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByArchivedStatus(null);

        // Then
        assertEquals(3, result.size());
        verify(warehouseStore, times(1)).findByArchived(null);
    }

    @Test
    void testSearchByLocationEmptyString() {
        // Given
        when(warehouseStore.findByLocation("")).thenReturn(Collections.emptyList());

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByLocation("");

        // Then
        assertTrue(result.isEmpty());
        verify(warehouseStore, times(1)).findByLocation("");
    }

    @Test
    void testSearchByCapacityRangeNoMatch() {
        // Given
        when(warehouseStore.findByCapacityBetween(3000, 4000)).thenReturn(Collections.emptyList());

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByCapacityRange(3000, 4000);

        // Then
        assertTrue(result.isEmpty());
        verify(warehouseStore, times(1)).findByCapacityBetween(3000, 4000);
    }

    @Test
    void testSearchByCapacityRangeNullMin() {
        // Given
        when(warehouseStore.findByCapacityBetween(null, 1500)).thenReturn(Arrays.asList(warehouse1));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByCapacityRange(null, 1500);

        // Then
        assertEquals(1, result.size());
        verify(warehouseStore, times(1)).findByCapacityBetween(null, 1500);
    }

    @Test
    void testSearchByCapacityRangeNullMax() {
        // Given
        when(warehouseStore.findByCapacityBetween(1500, null)).thenReturn(Arrays.asList(warehouse2, warehouse3));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByCapacityRange(1500, null);

        // Then
        assertEquals(2, result.size());
        verify(warehouseStore, times(1)).findByCapacityBetween(1500, null);
    }

    @Test
    void testSearchByCapacityRangeEqualMinMax() {
        // Given
        when(warehouseStore.findByCapacityBetween(1000, 1000)).thenReturn(Arrays.asList(warehouse1));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByCapacityRange(1000, 1000);

        // Then
        assertEquals(1, result.size());
        assertEquals("WH001", result.get(0).businessUnitCode);
        verify(warehouseStore, times(1)).findByCapacityBetween(1000, 1000);
    }

    @Test
    void testSearchByStockRangeEqualMinMax() {
        // Given
        when(warehouseStore.findByStockBetween(500, 500)).thenReturn(Arrays.asList(warehouse1));

        // When
        List<Warehouse> result = searchWarehousesUseCase.searchByStockRange(500, 500);

        // Then
        assertEquals(1, result.size());
        assertEquals("WH001", result.get(0).businessUnitCode);
        verify(warehouseStore, times(1)).findByStockBetween(500, 500);
    }
}
