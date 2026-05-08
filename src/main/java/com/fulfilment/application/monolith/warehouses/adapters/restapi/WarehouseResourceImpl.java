package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.usecases.SearchWarehousesUseCase;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;
  @Inject private SearchWarehousesUseCase searchWarehousesUseCase;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    // Convert API model to domain model
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = data.getBusinessUnitCode();
    domainWarehouse.location = data.getLocation();
    domainWarehouse.capacity = data.getCapacity();
    domainWarehouse.stock = data.getStock() != null ? data.getStock() : 0;

    try {
      // Create warehouse through use case (includes validations)
      createWarehouseOperation.create(domainWarehouse);
      
      // Return the created warehouse
      return toWarehouseResponse(domainWarehouse);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    // Find warehouse by business unit code
    var domainWarehouse = warehouseRepository.findByBusinessUnitCode(id);
    
    if (domainWarehouse == null) {
      throw new WebApplicationException("Warehouse with business unit code '" + id + "' not found", 404);
    }
    
    return toWarehouseResponse(domainWarehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    // Find warehouse by business unit code
    var domainWarehouse = warehouseRepository.findByBusinessUnitCode(id);

    if (domainWarehouse == null) {
      throw new WebApplicationException("Warehouse with business unit code '" + id + "' not found", 404);
    }

    try {
      // Archive warehouse through use case (includes validations)
      archiveWarehouseOperation.archive(domainWarehouse);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    // Convert API model to domain model
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = businessUnitCode; // Use businessUnitCode from path
    domainWarehouse.location = data.getLocation();
    domainWarehouse.capacity = data.getCapacity();
    domainWarehouse.stock = data.getStock() != null ? data.getStock() : 0;

    try {
      // Replace warehouse through use case (includes validations)
      replaceWarehouseOperation.replace(domainWarehouse);

      // Return the updated warehouse
      var updated = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
      return toWarehouseResponse(updated);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }

  // Search methods for bonus functionality
  public List<Warehouse> searchWarehousesByLocation(String location) {
    List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> warehouses = searchWarehousesUseCase.searchByLocation(location);
    return warehouses.stream()
        .map(this::toWarehouseResponse)
        .toList();
  }

  public List<Warehouse> searchWarehousesByCapacityRange(Integer minCapacity, Integer maxCapacity) {
    List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> warehouses = searchWarehousesUseCase.searchByCapacityRange(minCapacity, maxCapacity);
    return warehouses.stream()
        .map(this::toWarehouseResponse)
        .toList();
  }

  public List<Warehouse> searchWarehousesByStockRange(Integer minStock, Integer maxStock) {
    List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> warehouses = searchWarehousesUseCase.searchByStockRange(minStock, maxStock);
    return warehouses.stream()
        .map(this::toWarehouseResponse)
        .toList();
  }

  public List<Warehouse> searchWarehousesByArchivedStatus(Boolean archived) {
    List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> warehouses = searchWarehousesUseCase.searchByArchivedStatus(archived);
    return warehouses.stream()
        .map(this::toWarehouseResponse)
        .toList();
  }

  @GET
  @Path("/search")
  @Produces("application/json")
  public List<Warehouse> searchWarehouses(
      @QueryParam("location") String location, 
      @QueryParam("minCapacity") Integer minCapacity, 
      @QueryParam("maxCapacity") Integer maxCapacity, 
      @QueryParam("minStock") Integer minStock, 
      @QueryParam("maxStock") Integer maxStock, 
      @QueryParam("archived") Boolean archived,
      @QueryParam("sortBy") String sortBy, 
      @QueryParam("sortOrder") String sortOrder, 
      @QueryParam("page") Integer page, 
      @QueryParam("pageSize") Integer pageSize) {
    
    try {
      // Start with base query
      java.util.List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> warehouses = warehouseRepository.getAll();
      
      // Apply filters
      if (location != null && !location.trim().isEmpty()) {
        warehouses = warehouses.stream()
            .filter(w -> w.location.equalsIgnoreCase(location))
            .toList();
      }
      
      if (minCapacity != null) {
        warehouses = warehouses.stream()
            .filter(w -> w.capacity >= minCapacity)
            .toList();
      }
      
      if (maxCapacity != null) {
        warehouses = warehouses.stream()
            .filter(w -> w.capacity <= maxCapacity)
            .toList();
      }
      
      if (minStock != null) {
        warehouses = warehouses.stream()
            .filter(w -> w.stock >= minStock)
            .toList();
      }
      
      if (maxStock != null) {
        warehouses = warehouses.stream()
            .filter(w -> w.stock <= maxStock)
            .toList();
      }
      
      if (archived != null) {
        warehouses = warehouses.stream()
            .filter(w -> (archived ? w.archivedAt != null : w.archivedAt == null))
            .toList();
      }
      
      // Apply sorting
      if ("capacity".equals(sortBy)) {
        warehouses = warehouses.stream()
            .sorted((a, b) -> Integer.compare(b.capacity, a.capacity))
            .toList();
      } else if ("stock".equals(sortBy)) {
        warehouses = warehouses.stream()
            .sorted((a, b) -> Integer.compare(b.stock, a.stock))
            .toList();
      } else if ("location".equals(sortBy)) {
        warehouses = warehouses.stream()
            .sorted((a, b) -> a.location.compareTo(b.location))
            .toList();
      }
      
      // Apply sort order
      if ("desc".equalsIgnoreCase(sortOrder)) {
        java.util.Collections.reverse(warehouses);
      }
      
      // Apply pagination
      if (page != null && pageSize != null && pageSize > 0) {
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, warehouses.size());
        warehouses = warehouses.subList(startIndex, endIndex);
      }
      
      return warehouses.stream()
          .map(this::toWarehouseResponse)
          .toList();
          
    } catch (Exception e) {
      throw new WebApplicationException("Error searching warehouses: " + e.getMessage(), 500);
    }
  }
  
  }
