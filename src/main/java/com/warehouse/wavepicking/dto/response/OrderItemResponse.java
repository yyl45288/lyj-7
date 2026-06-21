package com.warehouse.wavepicking.dto.response;

public class OrderItemResponse {

    private Long id;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Integer quantity;
    private Integer pickedQuantity;
    private String location;

    public OrderItemResponse() {
    }

    public OrderItemResponse(Long id, Long skuId, String skuCode, String skuName, Integer quantity, Integer pickedQuantity, String location) {
        this.id = id;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.quantity = quantity;
        this.pickedQuantity = pickedQuantity;
        this.location = location;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPickedQuantity() {
        return pickedQuantity;
    }

    public void setPickedQuantity(Integer pickedQuantity) {
        this.pickedQuantity = pickedQuantity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long skuId;
        private String skuCode;
        private String skuName;
        private Integer quantity;
        private Integer pickedQuantity;
        private String location;

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

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder pickedQuantity(Integer pickedQuantity) {
            this.pickedQuantity = pickedQuantity;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public OrderItemResponse build() {
            return new OrderItemResponse(id, skuId, skuCode, skuName, quantity, pickedQuantity, location);
        }
    }
}
