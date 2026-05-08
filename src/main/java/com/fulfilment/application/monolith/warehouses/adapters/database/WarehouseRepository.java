package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  @Transactional
  public List<Warehouse> getAll() {
    return listAll().stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    // Create new DbWarehouse entity
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt != null ? warehouse.createdAt : LocalDateTime.now();
    dbWarehouse.archivedAt = warehouse.archivedAt;
    
    // Persist the entity
    persist(dbWarehouse);
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    // Find existing warehouse
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse == null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' does not exist");
    }
    
    // Update fields
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    
    // Use EntityManager to merge the entity
    getEntityManager().merge(dbWarehouse);
    getEntityManager().flush();
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    // Validation: warehouse must not be null
    if (warehouse == null) {
      throw new IllegalArgumentException("Warehouse cannot be null");
    }
    
    // Validation: business unit code must not be null or empty
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.trim().isEmpty()) {
      throw new IllegalArgumentException("Business unit code cannot be null or empty");
    }
    
    // Find existing warehouse
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse == null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' does not exist");
    }
    
    // Delete the warehouse
    delete(dbWarehouse);
  }
  
  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    // Validation: business unit code must not be null or empty
    if (buCode == null || buCode.trim().isEmpty()) {
      return null;
    }
    
    DbWarehouse dbWarehouse = find("businessUnitCode", buCode).firstResult();
    return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
  }
  
  // Search methods for bonus functionality
  @Transactional
  public List<Warehouse> findByLocation(String location) {
    if (location == null || location.trim().isEmpty()) {
      return List.of();
    }
    return list("location", location).stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }
  
  @Transactional
  public List<Warehouse> findByCapacityBetween(Integer minCapacity, Integer maxCapacity) {
    if (minCapacity != null && maxCapacity != null && minCapacity > maxCapacity) {
      return List.of();
    }
    if (minCapacity == null && maxCapacity == null) {
      return listAll().stream()
          .map(DbWarehouse::toWarehouse)
          .toList();
    }
    if (minCapacity == null) {
      return list("capacity <= ?1", maxCapacity).stream()
          .map(DbWarehouse::toWarehouse)
          .toList();
    }
    if (maxCapacity == null) {
      return list("capacity >= ?1", minCapacity).stream()
          .map(DbWarehouse::toWarehouse)
          .toList();
    }
    return list("capacity between ?1 and ?2", minCapacity, maxCapacity).stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }
  
  @Transactional
  public List<Warehouse> findByStockBetween(Integer minStock, Integer maxStock) {
    if (minStock != null && maxStock != null && minStock > maxStock) {
      return List.of();
    }
    if (minStock == null && maxStock == null) {
      return listAll().stream()
          .map(DbWarehouse::toWarehouse)
          .toList();
    }
    if (minStock == null) {
      return list("stock <= ?1", maxStock).stream()
          .map(DbWarehouse::toWarehouse)
          .toList();
    }
    if (maxStock == null) {
      return list("stock >= ?1", minStock).stream()
          .map(DbWarehouse::toWarehouse)
          .toList();
    }
    return list("stock between ?1 and ?2", minStock, maxStock).stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }
  
  @Transactional
  public List<Warehouse> findByArchived(Boolean archived) {
    if (archived == null) {
      return listAll().stream()
          .map(DbWarehouse::toWarehouse)
          .toList();
    } else if (archived) {
      return list("archivedAt is not null").stream()
          .map(DbWarehouse::toWarehouse)
          .toList();
    } else {
      return list("archivedAt is null").stream()
          .map(DbWarehouse::toWarehouse)
          .toList();
    }
  }
  
  // Additional utility methods
  
  @Transactional
  public long countByArchived(Boolean archived) {
    if (archived == null) {
      return count();
    } else if (archived) {
      return count("archivedAt is not null");
    } else {
      return count("archivedAt is null");
    }
  }
  
  @Transactional
  public boolean existsByBusinessUnitCode(String businessUnitCode) {
    if (businessUnitCode == null || businessUnitCode.trim().isEmpty()) {
      return false;
    }
    return count("businessUnitCode", businessUnitCode) > 0;
  }
  
  @Transactional
  public Optional<Warehouse> findByBusinessUnitCodeOptional(String buCode) {
    return Optional.ofNullable(findByBusinessUnitCode(buCode));
  }
}
