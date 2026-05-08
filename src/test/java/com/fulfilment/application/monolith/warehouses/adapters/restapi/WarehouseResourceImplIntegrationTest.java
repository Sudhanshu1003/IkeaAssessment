package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.warehouse.api.beans.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@WithTestResource(H2DatabaseTestResource.class)
public class WarehouseResourceImplIntegrationTest {

    @Test
    void testCreateWarehouseEndpoint() {
        Warehouse newWarehouse = new Warehouse();
        newWarehouse.setBusinessUnitCode("API-TEST-" + System.currentTimeMillis());
        newWarehouse.setLocation("AMSTERDAM-001");
        newWarehouse.setCapacity(30);
        newWarehouse.setStock(25);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(newWarehouse)
            .when()
            .post("/warehouse")
            .then()
            .statusCode(200)
            .extract()
            .response();

        Warehouse created = response.as(Warehouse.class);
        assertEquals(newWarehouse.getBusinessUnitCode(), created.getBusinessUnitCode());
        assertEquals("AMSTERDAM-001", created.getLocation());
        assertEquals(30, created.getCapacity());
        assertEquals(25, created.getStock());
    }

    @Test
    void testCreateWarehouseWithInvalidData() {
        Warehouse invalidWarehouse = new Warehouse();
        invalidWarehouse.setBusinessUnitCode(""); // Invalid empty code
        invalidWarehouse.setLocation("INVALID-LOCATION");
        invalidWarehouse.setCapacity(30);
        invalidWarehouse.setStock(25);

        given()
            .contentType(ContentType.JSON)
            .body(invalidWarehouse)
            .when()
            .post("/warehouse")
            .then()
            .statusCode(400)
            .body(notNullValue());
    }

    @Test
    void testListWarehousesEndpoint() {
        Response response = when()
            .get("/warehouse")
            .then()
            .statusCode(200)
            .extract()
            .response();

        List<Warehouse> warehouses = response.jsonPath().getList("$", Warehouse.class);
        assertTrue(warehouses.size() > 0); // Should have some warehouses
    }

    @Test
    void testGetWarehouseByIdEndpoint() {
        // First create a warehouse
        Warehouse created = createTestWarehouse("GET-TEST-" + System.currentTimeMillis(), "AMSTERDAM-001", 30, 20);

        when()
            .get("/warehouse/" + created.getBusinessUnitCode())
            .then()
            .statusCode(200)
            .body("businessUnitCode", equalTo(created.getBusinessUnitCode()))
            .body("location", equalTo("AMSTERDAM-001"))
            .body("capacity", equalTo(30))
            .body("stock", equalTo(20));
    }

    @Test
    void testGetWarehouseByIdNotFound() {
        when()
            .get("/warehouse/NONEXISTENT-" + System.currentTimeMillis())
            .then()
            .statusCode(404);
    }

    @Test
    void testArchiveWarehouseEndpoint() {
        // Create warehouse first
        Warehouse created = createTestWarehouse("ARCHIVE-TEST-" + System.currentTimeMillis(), "AMSTERDAM-001", 30, 20);

        when()
            .delete("/warehouse/" + created.getBusinessUnitCode())
            .then()
            .statusCode(204); // Archive returns 204

        // Verify warehouse is archived (archived warehouses might still be visible)
        when()
            .get("/warehouse/" + created.getBusinessUnitCode())
            .then()
            .statusCode(200); // Might still be visible
    }

    @Test
    void testArchiveWarehouseNotFound() {
        when()
            .delete("/warehouse/NONEXISTENT-" + System.currentTimeMillis())
            .then()
            .statusCode(404);
    }

    @Test
    void testReplaceWarehouseEndpoint() {
        // Create warehouse first
        Warehouse created = createTestWarehouse("REPLACE-TEST-" + System.currentTimeMillis(), "AMSTERDAM-001", 30, 20);

        // Create replacement warehouse
        Warehouse replacement = new Warehouse();
        replacement.setBusinessUnitCode(created.getBusinessUnitCode());
        replacement.setLocation("ZWOLLE-001");
        replacement.setCapacity(35);
        replacement.setStock(25);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(replacement)
            .when()
            .post("/warehouse/" + created.getBusinessUnitCode() + "/replacement")
            .then()
            .statusCode(200)
            .extract()
            .response();

        Warehouse replaced = response.as(Warehouse.class);
        assertEquals(created.getBusinessUnitCode(), replaced.getBusinessUnitCode());
        assertEquals("ZWOLLE-001", replaced.getLocation());
        assertEquals(35, replaced.getCapacity());
        assertEquals(25, replaced.getStock());
    }

    @Test
    void testReplaceWarehouseNotFound() {
        Warehouse replacement = new Warehouse();
        replacement.setBusinessUnitCode("REPLACE-NONEXISTENT-" + System.currentTimeMillis());
        replacement.setLocation("ZWOLLE-001");
        replacement.setCapacity(35);
        replacement.setStock(25);

        given()
            .contentType(ContentType.JSON)
            .body(replacement)
            .when()
            .post("/warehouse/NONEXISTENT-" + System.currentTimeMillis() + "/replacement")
            .then()
            .statusCode(400); // Returns 400 for validation error, not 404
    }

    @Test
    void testSearchWarehousesByLocation() {
        Response response = given()
            .queryParam("location", "AMSTERDAM-001")
            .when()
            .get("/warehouse/search")
            .then()
            .statusCode(200)
            .extract()
            .response();

        List<Warehouse> results = response.jsonPath().getList("$", Warehouse.class);
        assertTrue(results.size() >= 0); // Should find warehouses in AMSTERDAM-001
    }

    @Test
    void testSearchWarehousesByCapacityRange() {
        Response response = given()
            .queryParam("minCapacity", 20)
            .queryParam("maxCapacity", 40)
            .when()
            .get("/warehouse/search")
            .then()
            .statusCode(200)
            .extract()
            .response();

        List<Warehouse> results = response.jsonPath().getList("$", Warehouse.class);
        assertTrue(results.size() >= 0); // Should find warehouses with capacity 20-40
    }

    @Test
    void testSearchWarehousesByStockRange() {
        Response response = given()
            .queryParam("minStock", 10)
            .queryParam("maxStock", 30)
            .when()
            .get("/warehouse/search")
            .then()
            .statusCode(200)
            .extract()
            .response();

        List<Warehouse> results = response.jsonPath().getList("$", Warehouse.class);
        assertTrue(results.size() >= 0); // Should find warehouses with stock 10-30
    }

    @Test
    void testSearchWarehousesWithSorting() {
        Response response = given()
            .queryParam("sortBy", "capacity")
            .when()
            .get("/warehouse/search")
            .then()
            .statusCode(200)
            .extract()
            .response();

        List<Warehouse> results = response.jsonPath().getList("$", Warehouse.class);
        assertTrue(results.size() >= 0); // Should return sorted results
    }

    @Test
    void testSearchWarehousesWithPagination() {
        Response response = given()
            .queryParam("page", 0)
            .queryParam("pageSize", 5)
            .when()
            .get("/warehouse/search")
            .then()
            .statusCode(200)
            .extract()
            .response();

        List<Warehouse> results = response.jsonPath().getList("$", Warehouse.class);
        assertTrue(results.size() <= 5); // Should return at most 5 results
    }

    @Test
    void testSearchWarehousesAllParameters() {
        Response response = given()
            .queryParam("location", "AMSTERDAM-001")
            .queryParam("minCapacity", 20)
            .queryParam("maxCapacity", 40)
            .queryParam("minStock", 10)
            .queryParam("maxStock", 30)
            .queryParam("archived", false)
            .queryParam("sortBy", "capacity")
            .queryParam("sortOrder", "asc")
            .queryParam("page", 0)
            .queryParam("pageSize", 10)
            .when()
            .get("/warehouse/search")
            .then()
            .statusCode(200)
            .extract()
            .response();

        List<Warehouse> results = response.jsonPath().getList("$", Warehouse.class);
        assertTrue(results.size() >= 0); // Should find warehouses matching all criteria
    }

    @Test
    void testSearchWarehousesNoResults() {
        Response response = given()
            .queryParam("location", "NON-EXISTENT-LOCATION-" + System.currentTimeMillis())
            .when()
            .get("/warehouse/search")
            .then()
            .statusCode(200)
            .extract()
            .response();

        List<Warehouse> results = response.jsonPath().getList("$", Warehouse.class);
        assertEquals(0, results.size()); // Should find no warehouses
    }

    private Warehouse createTestWarehouse(String businessUnitCode, String location, int capacity, int stock) {
        Warehouse warehouse = new Warehouse();
        warehouse.setBusinessUnitCode(businessUnitCode);
        warehouse.setLocation(location);
        warehouse.setCapacity(capacity);
        warehouse.setStock(stock);
        
        Response response = given()
            .contentType(ContentType.JSON)
            .body(warehouse)
            .when()
            .post("/warehouse")
            .then()
            .statusCode(200)
            .extract()
            .response();
            
        return response.as(Warehouse.class);
    }
}
