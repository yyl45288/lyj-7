package com.warehouse.wavepicking.dto.response;

import java.time.LocalDateTime;

public class InventoryResponse {

    private Long id;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Integer availableQuantity;
    private Integer lockedQuantity;
    private Integer totalQuantity;
    private Integer safetyStock;
    private Boolean outOfStock;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InventoryResponse() {
    }

    public InventoryResponse(Long id, Long skuId, String skuCode, String skuName, Integer availableQuantity, Integer lockedQuantity, Integer totalQuantity, Integer safetyStock, Boolean outOfStock, String location, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.availableQuantity = availableQuantity;
        this.lockedQuantity = lockedQuantity;
        this.totalQuantity = totalQuantity;
        this.safetyStock = safetyStock;
        this.outOfStock = outOfStock;
        this.location = location;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getLockedQuantity() {
        return lockedQuantity;
    }

    public void setLockedQuantity(Integer lockedQuantity) {
        this.lockedQuantity = lockedQuantity;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(Integer safetyStock) {
        this.safetyStock = safetyStock;
    }

    public Boolean getOutOfStock() {
        return outOfStock;
    }

    public void setOutOfStock(Boolean outOfStock) {
        this.outOfStock = outOfStock;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long skuId;
        private String skuCode;
        private String skuName;
        private Integer availableQuantity;
        private Integer lockedQuantity;
        private Integer totalQuantity;
        private Integer safetyStock;
        private Boolean outOfStock;
        private String location;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder skuId(Long skuId) {
            this.skuId = skuId;
            return this;
        }

        public Builder skuCode(String skuCode) {
            this.skuCode = skuCode;
            return this;
        }

        public Builder skuName(String skuName) {
            this.skuName = skuName;
            return this;
        }

        public Builder availableQuantity(Integer availableQuantity) {
            this.availableQuantity = availableQuantity;
            return this;
        }

        public Builder lockedQuantity(Integer lockedQuantity) {
            this.lockedQuantity = lockedQuantity;
            return this;
        }

        public Builder totalQuantity(Integer totalQuantity) {
            this.totalQuantity = totalQuantity;
            return this;
        }

        public Builder safetyStock(Integer safetyStock) {
            this.safetyStock = safetyStock;
            return this;
        }

        public Builder outOfStock(Boolean outOfStock) {
            this.outOfStock = outOfStock;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public InventoryResponse build() {
            return new InventoryResponse(id, skuId, skuCode, skuName, availableQuantity, lockedQuantity, totalQuantity, safetyStock, outOfStock, location, createdAt, updatedAt);
        }
    }
}
