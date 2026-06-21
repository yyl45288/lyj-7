package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.response.SkuResponse;
import com.warehouse.wavepicking.entity.Sku;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.SkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SkuService {

    private final SkuRepository skuRepository;

    public SkuService(SkuRepository skuRepository) {
        this.skuRepository = skuRepository;
    }

    public List<SkuResponse> getAllSkus() {
        return skuRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public SkuResponse getSkuById(Long id) {
        Sku sku = skuRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SKU_NOT_FOUND", "SKU不存在: " + id));
        return convertToResponse(sku);
    }

    public SkuResponse getSkuByCode(String skuCode) {
        Sku sku = skuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new BusinessException("SKU_NOT_FOUND", "SKU不存在: " + skuCode));
        return convertToResponse(sku);
    }

    @Transactional
    public SkuResponse createSku(Sku sku) {
        if (skuRepository.existsBySkuCode(sku.getSkuCode())) {
            throw new BusinessException("SKU_CODE_EXISTS", "SKU编码已存在: " + sku.getSkuCode());
        }
        Sku saved = skuRepository.save(sku);
        return convertToResponse(saved);
    }

    @Transactional
    public SkuResponse updateSku(Long id, Sku skuRequest) {
        Sku sku = skuRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SKU_NOT_FOUND", "SKU不存在: " + id));
        sku.setSkuName(skuRequest.getSkuName());
        sku.setCategory(skuRequest.getCategory());
        sku.setUnit(skuRequest.getUnit());
        sku.setWeight(skuRequest.getWeight());
        sku.setLocation(skuRequest.getLocation());
        Sku saved = skuRepository.save(sku);
        return convertToResponse(saved);
    }

    @Transactional
    public void deleteSku(Long id) {
        if (!skuRepository.existsById(id)) {
            throw new BusinessException("SKU_NOT_FOUND", "SKU不存在: " + id);
        }
        skuRepository.deleteById(id);
    }

    private SkuResponse convertToResponse(Sku sku) {
        return SkuResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .skuName(sku.getSkuName())
                .category(sku.getCategory())
                .unit(sku.getUnit())
                .weight(sku.getWeight())
                .location(sku.getLocation())
                .createdAt(sku.getCreatedAt())
                .updatedAt(sku.getUpdatedAt())
                .build();
    }
}
