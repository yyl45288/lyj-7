package com.warehouse.wavepicking.controller;

import com.warehouse.wavepicking.common.ApiResponse;
import com.warehouse.wavepicking.dto.response.InventoryResponse;
import com.warehouse.wavepicking.dto.response.SkuResponse;
import com.warehouse.wavepicking.entity.Sku;
import com.warehouse.wavepicking.service.InventoryService;
import com.warehouse.wavepicking.service.SkuService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skus")
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    @GetMapping
    public ApiResponse<List<SkuResponse>> getAllSkus() {
        return ApiResponse.success(skuService.getAllSkus());
    }

    @GetMapping("/{id}")
    public ApiResponse<SkuResponse> getSkuById(@PathVariable Long id) {
        return ApiResponse.success(skuService.getSkuById(id));
    }

    @GetMapping("/code/{skuCode}")
    public ApiResponse<SkuResponse> getSkuByCode(@PathVariable String skuCode) {
        return ApiResponse.success(skuService.getSkuByCode(skuCode));
    }

    @PostMapping
    public ApiResponse<SkuResponse> createSku(@RequestBody Sku sku) {
        return ApiResponse.success(skuService.createSku(sku));
    }

    @PutMapping("/{id}")
    public ApiResponse<SkuResponse> updateSku(@PathVariable Long id, @RequestBody Sku sku) {
        return ApiResponse.success(skuService.updateSku(id, sku));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSku(@PathVariable Long id) {
        skuService.deleteSku(id);
        return ApiResponse.success();
    }
}
