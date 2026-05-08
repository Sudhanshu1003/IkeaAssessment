package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SearchWarehousesUseCase {
    
    private final WarehouseStore warehouseStore;
    
    public SearchWarehousesUseCase(WarehouseStore warehouseStore) {
        this.warehouseStore = warehouseStore;
    }
    
    public java.util.List<Warehouse> searchByLocation(String location) {
        return warehouseStore.findByLocation(location);
    }
    
    public java.util.List<Warehouse> searchByCapacityRange(Integer minCapacity, Integer maxCapacity) {
        return warehouseStore.findByCapacityBetween(minCapacity, maxCapacity);
    }
    
    public java.util.List<Warehouse> searchByStockRange(Integer minStock, Integer maxStock) {
        return warehouseStore.findByStockBetween(minStock, maxStock);
    }
    
    public java.util.List<Warehouse> searchByArchivedStatus(Boolean archived) {
        return warehouseStore.findByArchived(archived);
    }
}
