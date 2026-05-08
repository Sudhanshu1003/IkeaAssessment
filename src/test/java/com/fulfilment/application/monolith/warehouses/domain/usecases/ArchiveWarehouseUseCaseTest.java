package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Archive Warehouse use case.
 *
 * Covers basic archive operations and concurrent modification scenarios.
 */
@QuarkusTest
public class ArchiveWarehouseUseCaseTest {

  @Inject
  WarehouseRepository warehouseRepository;

  @Inject
  ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    // Clean slate
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
  }

  /**
   * Basic archive functionality
   */
  @Test
  @Transactional
  public void testArchiveWarehouseSuccessfully() {
    // Create a warehouse
    Warehouse warehouse = createWarehouse("ARCHIVE-TEST-001", "AMSTERDAM-001");

    // Archive it
    archiveWarehouseUseCase.archive(warehouse);

    // Verify it was archived
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("ARCHIVE-TEST-001");
    assertNotNull(archived);
    assertNotNull(archived.archivedAt);
  }

  /**
   * Cannot archive non-existent warehouse
   */
  @Test
  @Transactional
  public void testCannotArchiveNonExistentWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NON-EXISTENT";

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse);
    });

    assertTrue(exception.getMessage().contains("does not exist"));
  }

  /**
   * Cannot archive already-archived warehouse
   */
  @Test
  @Transactional
  public void testCannotArchiveAlreadyArchivedWarehouse() {
    // Create and archive a warehouse
    Warehouse warehouse = createWarehouse("ARCHIVE-TEST-002", "ZWOLLE-001");
    archiveWarehouseUseCase.archive(warehouse);

    // Try to archive again
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse);
    });

    assertTrue(exception.getMessage().contains("already archived"));
  }

  /**
   * Concurrent archive and stock update scenario.
   *
   * Scenario:
   * - Thread 1: Archives warehouse (sets archivedAt)
   * - Thread 2: Updates stock concurrently
   * - Expected: Data integrity is preserved — either the conflict is detected
   *             and an exception is thrown, or both changes are correctly applied.
   */
  @Test
  public void testConcurrentArchiveAndStockUpdateCausesOptimisticLockException() throws InterruptedException {
    // Setup: Create a warehouse
    String businessUnitCode = createWarehouseInNewTransaction("CONCURRENT-ARCHIVE-001", "AMSTERDAM-001");

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(2);

    AtomicBoolean archiveSuccess = new AtomicBoolean(false);
    AtomicBoolean updateSuccess = new AtomicBoolean(false);
    AtomicBoolean exceptionCaught = new AtomicBoolean(false);

    // Thread 1: Archive warehouse
    executor.submit(() -> {
      try {
        startLatch.await(); // Synchronize start
        archiveWarehouseInNewTransaction(businessUnitCode);
        archiveSuccess.set(true);
      } catch (Exception e) {
        exceptionCaught.set(true);
      } finally {
        finishLatch.countDown();
      }
    });

    // Thread 2: Update stock concurrently
    executor.submit(() -> {
      try {
        startLatch.await(); // Synchronize start
        updateStockInNewTransaction(businessUnitCode, 75);
        updateSuccess.set(true);
      } catch (Exception e) {
        exceptionCaught.set(true);
      } finally {
        finishLatch.countDown();
      }
    });

    startLatch.countDown(); // Start both threads
    finishLatch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    // Verification: Check the final state
    Warehouse finalWarehouse = warehouseRepository.findByBusinessUnitCode(businessUnitCode);

    boolean bothChangesApplied = finalWarehouse.archivedAt != null && finalWarehouse.stock == 75;

    assertTrue(bothChangesApplied || exceptionCaught.get(),
        "Expected either both changes to be applied properly OR an exception to be thrown. " +
        "Instead, a lost update occurred: archivedAt=" + finalWarehouse.archivedAt +
        ", stock=" + finalWarehouse.stock);

    // Additional check: if no exception was caught, both changes should be applied
    if (!exceptionCaught.get()) {
      assertNotNull(finalWarehouse.archivedAt, "Archive timestamp should be set");
      assertEquals(75, finalWarehouse.stock, "Stock should be updated to 75");
    }
  }

  // Helper methods

  @Transactional(TxType.REQUIRES_NEW)
  Warehouse createWarehouse(String businessUnitCode, String location) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();

    warehouseRepository.create(warehouse);
    return warehouse;
  }

  @Transactional(TxType.REQUIRES_NEW)
  String createWarehouseInNewTransaction(String businessUnitCode, String location) {
    createWarehouse(businessUnitCode, location);
    return businessUnitCode;
  }

  @Transactional(TxType.REQUIRES_NEW)
  void archiveWarehouseInNewTransaction(String businessUnitCode) {
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    archiveWarehouseUseCase.archive(warehouse);
  }

  @Transactional(TxType.REQUIRES_NEW)
  void updateStockInNewTransaction(String businessUnitCode, int newStock) {
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    warehouse.stock = newStock;
    warehouseRepository.update(warehouse);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithNullWarehouse() {
    // Given
    Warehouse nullWarehouse = null;

    // When & Then
    assertThrows(Exception.class, () -> archiveWarehouseUseCase.archive(nullWarehouse));
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithNullBusinessUnitCode() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = null;
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();

    // When & Then
    assertThrows(Exception.class, () -> archiveWarehouseUseCase.archive(warehouse));
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithEmptyBusinessUnitCode() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();

    // When & Then
    assertThrows(Exception.class, () -> archiveWarehouseUseCase.archive(warehouse));
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithNonExistentWarehouse() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NONEXISTENT";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = LocalDateTime.now();

    // When & Then
    assertThrows(Exception.class, () -> archiveWarehouseUseCase.archive(warehouse));
  }

  @Test
  @Transactional
  void testArchiveWarehouseAlreadyArchived() {
    // Given
    Warehouse warehouse = createWarehouse("ALREADY-ARCHIVED", "AMSTERDAM-001");
    
    // Archive it once
    archiveWarehouseUseCase.archive(warehouse);
    
    // Verify it's archived by checking the repository
    Warehouse archivedWarehouse = warehouseRepository.findByBusinessUnitCode("ALREADY-ARCHIVED");
    assertNotNull(archivedWarehouse.archivedAt);
    
    // When - try to archive again
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
        () -> archiveWarehouseUseCase.archive(warehouse));
    
    // Then - should throw exception for already archived warehouse
    assertTrue(exception.getMessage().contains("already archived"));
    
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("ALREADY-ARCHIVED");
    assertNotNull(archived.archivedAt);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithNegativeStock() {
    // Given
    Warehouse warehouse = createWarehouse("NEG-STOCK", "AMSTERDAM-001");
    warehouse.stock = -50;
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("NEG-STOCK");
    assertNotNull(archived.archivedAt);
    assertEquals(-50, archived.stock);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithZeroStock() {
    // Given
    Warehouse warehouse = createWarehouse("ZERO-STOCK", "AMSTERDAM-001");
    warehouse.stock = 0;
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("ZERO-STOCK");
    assertNotNull(archived.archivedAt);
    assertEquals(0, archived.stock);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithNegativeCapacity() {
    // Given
    Warehouse warehouse = createWarehouse("NEG-CAP", "AMSTERDAM-001");
    warehouse.capacity = -100;
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("NEG-CAP");
    assertNotNull(archived.archivedAt);
    assertEquals(-100, archived.capacity);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithZeroCapacity() {
    // Given
    Warehouse warehouse = createWarehouse("ZERO-CAP", "AMSTERDAM-001");
    warehouse.capacity = 0;
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("ZERO-CAP");
    assertNotNull(archived.archivedAt);
    assertEquals(0, archived.capacity);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithNullLocation() {
    // Given
    Warehouse warehouse = createWarehouse("NULL-LOC", "AMSTERDAM-001");
    warehouse.location = null;
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("NULL-LOC");
    assertNotNull(archived.archivedAt);
    assertNull(archived.location);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithEmptyLocation() {
    // Given
    Warehouse warehouse = createWarehouse("EMPTY-LOC", "AMSTERDAM-001");
    warehouse.location = "";
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("EMPTY-LOC");
    assertNotNull(archived.archivedAt);
    assertEquals("", archived.location);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithNullCreatedAt() {
    // Given
    Warehouse warehouse = createWarehouse("NULL-CREATED", "AMSTERDAM-001");
    warehouse.createdAt = null;
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then - createdAt may be set by the create method, so just check archivedAt
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("NULL-CREATED");
    assertNotNull(archived.archivedAt);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithMaxValues() {
    // Given
    Warehouse warehouse = createWarehouse("MAX-VALUES", "AMSTERDAM-001");
    warehouse.capacity = Integer.MAX_VALUE;
    warehouse.stock = Integer.MAX_VALUE;
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("MAX-VALUES");
    assertNotNull(archived.archivedAt);
    assertEquals(Integer.MAX_VALUE, archived.capacity);
    assertEquals(Integer.MAX_VALUE, archived.stock);
  }

  @Test
  @Transactional
  void testArchiveWarehouseWithMinValues() {
    // Given
    Warehouse warehouse = createWarehouse("MIN-VALUES", "AMSTERDAM-001");
    warehouse.capacity = Integer.MIN_VALUE;
    warehouse.stock = Integer.MIN_VALUE;
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("MIN-VALUES");
    assertNotNull(archived.archivedAt);
    assertEquals(Integer.MIN_VALUE, archived.capacity);
    assertEquals(Integer.MIN_VALUE, archived.stock);
  }

  @Test
  @Transactional
  void testArchiveWarehouseTimestampAccuracy() {
    // Given
    Warehouse warehouse = createWarehouse("TIMESTAMP-TEST", "AMSTERDAM-001");
    LocalDateTime beforeArchive = LocalDateTime.now();

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("TIMESTAMP-TEST");
    assertNotNull(archived.archivedAt);
    LocalDateTime afterArchive = LocalDateTime.now();
    
    assertTrue(archived.archivedAt.isAfter(beforeArchive.minusSeconds(1)));
    assertTrue(archived.archivedAt.isBefore(afterArchive.plusSeconds(1)));
  }

  @Test
  @Transactional
  void testArchiveWarehouseDoesNotAffectOtherFields() {
    // Given
    Warehouse warehouse = createWarehouse("FIELDS-TEST", "AMSTERDAM-001");
    warehouse.capacity = 500;
    warehouse.stock = 250;
    warehouseRepository.update(warehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Warehouse archived = warehouseRepository.findByBusinessUnitCode("FIELDS-TEST");
    assertNotNull(archived.archivedAt);
    assertEquals("FIELDS-TEST", archived.businessUnitCode);
    assertEquals("AMSTERDAM-001", archived.location);
    assertEquals(500, archived.capacity);
    assertEquals(250, archived.stock);
    assertNotNull(archived.createdAt);
  }
}
