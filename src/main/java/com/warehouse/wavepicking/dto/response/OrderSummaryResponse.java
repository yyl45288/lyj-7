package com.warehouse.wavepicking.dto.response;

public class OrderSummaryResponse {

    private Long id;
    private String orderNo;
    private String customerName;
    private String status;
    private Integer itemCount;
    private Integer totalQuantity;

    public OrderSummaryResponse() {
    }

    public OrderSummaryResponse(Long id, String orderNo, String customerName, String status, Integer itemCount, Integer totalQuantity) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.status = status;
        this.itemCount = itemCount;
        this.totalQuantity = totalQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String orderNo;
        private String customerName;
        private String status;
        private Integer itemCount;
        private Integer totalQuantity;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder orderNo(String orderNo) {
            this.orderNo = orderNo;
            return this;
        }

        public Builder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder itemCount(Integer itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        public Builder totalQuantity(Integer totalQuantity) {
            this.totalQuantity = totalQuantity;
            return this;
        }

        public OrderSummaryResponse build() {
            return new OrderSummaryResponse(id, orderNo, customerName, status, itemCount, totalQuantity);
        }
    }
}
