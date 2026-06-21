package com.warehouse.wavepicking.controller;

import com.warehouse.wavepicking.common.ApiResponse;
import com.warehouse.wavepicking.dto.response.InventoryResponse;
import com.warehouse.wavepicking.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<List<InventoryResponse>> getAllInventories() {
        return ApiResponse.success(inventoryService.getAllInventories());
    }

    @GetMapping("/sku/{skuId}")
    public ApiResponse<InventoryResponse> getInventoryBySkuId(@PathVariable Long skuId) {
        return ApiResponse.success(inventoryService.getInventoryBySkuId(skuId));
    }

    @GetMapping("/out-of-stock")
    public ApiResponse<List<InventoryResponse>> getOutOfStockItems() {
        return ApiResponse.success(inventoryService.getOutOfStockItems());
    }

    @PostMapping("/sku/{skuId}")
    public ApiResponse<InventoryResponse> createInventory(@PathVariable Long skuId,
                                                           @RequestBody Map<String, Integer> body) {
        Integer quantity = body.get("quantity");
        if (quantity == null) {
            quantity = 0;
        }
        return ApiResponse.success(inventoryService.createInventory(skuId, quantity));
    }

    @PutMapping("/sku/{skuId}/add")
    public ApiResponse<InventoryResponse> addStock(@PathVariable Long skuId,
                                                    @RequestBody Map<String, Integer> body) {
        Integer quantity = body.get("quantity");
        return ApiResponse.success(inventoryService.addStock(skuId, quantity));
    }

    @PutMapping("/sku/{skuId}/reduce")
    public ApiResponse<InventoryResponse> reduceStock(@PathVariable Long skuId,
                                                       @RequestBody Map<String, Integer> body) {
        Integer quantity = body.get("quantity");
        return ApiResponse.success(inventoryService.reduceStock(skuId, quantity));
    }
}
