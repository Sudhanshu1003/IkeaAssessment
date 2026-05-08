package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

public interface WarehouseStore {

  List<Warehouse> getAll();

  void create(Warehouse warehouse);

  void update(Warehouse warehouse);

  void remove(Warehouse warehouse);

  Warehouse findByBusinessUnitCode(String buCode);
  
  // Search methods for bonus functionality
  List<Warehouse> findByLocation(String location);
  List<Warehouse> findByCapacityBetween(Integer minCapacity, Integer maxCapacity);
  List<Warehouse> findByStockBetween(Integer minStock, Integer maxStock);
  List<Warehouse> findByArchived(Boolean archived);
}
