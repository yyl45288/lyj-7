package com.warehouse.wavepicking.dto.response;

import java.time.LocalDateTime;

public class SkuResponse {

    private Long id;
    private String skuCode;
    private String skuName;
    private String category;
    private String unit;
    private Double weight;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SkuResponse() {
    }

    public SkuResponse(Long id, String skuCode, String skuName, String category, String unit, Double weight, String location, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.category = category;
        this.unit = unit;
        this.weight = weight;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
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
        private String skuCode;
        private String skuName;
        private String category;
        private String unit;
        private Double weight;
        private String location;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
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

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public Builder weight(Double weight) {
            this.weight = weight;
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

        public SkuResponse build() {
            return new SkuResponse(id, skuCode, skuName, category, unit, weight, location, createdAt, updatedAt);
        }
    }
}
