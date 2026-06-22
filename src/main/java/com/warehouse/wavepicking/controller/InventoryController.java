package com.warehouse.wavepicking.controller;

import com.warehouse.wavepicking.common.ApiResponse;
import com.warehouse.wavepicking.dto.response.InventoryResponse;
import com.warehouse.wavepicking.entity.InventoryBatch;
import com.warehouse.wavepicking.service.IInventoryService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final IInventoryService inventoryService;

    public InventoryController(IInventoryService inventoryService) {
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

    @PostMapping("/batches")
    public ApiResponse<InventoryBatch> createBatch(@RequestBody Map<String, Object> body) {
        Long skuId = Long.valueOf(body.get("skuId").toString());
        String batchNo = (String) body.get("batchNo");
        Integer quantity = Integer.valueOf(body.get("quantity").toString());
        LocalDateTime expiryDate = LocalDateTime.parse((String) body.get("expiryDate"));
        return ApiResponse.success(inventoryService.createBatch(skuId, batchNo, quantity, expiryDate));
    }

    @GetMapping("/batches/sku/{skuId}")
    public ApiResponse<List<InventoryBatch>> getBatchesBySkuId(@PathVariable Long skuId) {
        return ApiResponse.success(inventoryService.getBatchesBySkuId(skuId));
    }
}
