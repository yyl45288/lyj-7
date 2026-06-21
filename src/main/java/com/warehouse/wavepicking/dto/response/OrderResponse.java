package com.warehouse.wavepicking.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String orderNo;
    private String customerName;
    private String address;
    private String phone;
    private String status;
    private Boolean urgent;
    private Long waveId;
    private String waveNo;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private String remark;
    private Integer totalQuantity;

    public OrderResponse() {
    }

    public OrderResponse(Long id, String orderNo, String customerName, String address, String phone, String status, Boolean urgent, Long waveId, String waveNo, List<OrderItemResponse> items, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime completedAt, String remark, Integer totalQuantity) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.address = address;
        this.phone = phone;
        this.status = status;
        this.urgent = urgent;
        this.waveId = waveId;
        this.waveNo = waveNo;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.remark = remark;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getUrgent() {
        return urgent;
    }

    public void setUrgent(Boolean urgent) {
        this.urgent = urgent;
    }

    public Long getWaveId() {
        return waveId;
    }

    public void setWaveId(Long waveId) {
        this.waveId = waveId;
    }

    public String getWaveNo() {
        return waveNo;
    }

    public void setWaveNo(String waveNo) {
        this.waveNo = waveNo;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
        private String address;
        private String phone;
        private String status;
        private Boolean urgent;
        private Long waveId;
        private String waveNo;
        private List<OrderItemResponse> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime completedAt;
        private String remark;
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

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder urgent(Boolean urgent) {
            this.urgent = urgent;
            return this;
        }

        public Builder waveId(Long waveId) {
            this.waveId = waveId;
            return this;
        }

        public Builder waveNo(String waveNo) {
            this.waveNo = waveNo;
            return this;
        }

        public Builder items(List<OrderItemResponse> items) {
            this.items = items;
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

        public Builder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public Builder totalQuantity(Integer totalQuantity) {
            this.totalQuantity = totalQuantity;
            return this;
        }

        public OrderResponse build() {
            return new OrderResponse(id, orderNo, customerName, address, phone, status, urgent, waveId, waveNo, items, createdAt, updatedAt, completedAt, remark, totalQuantity);
        }
    }
}
