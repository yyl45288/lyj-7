package com.warehouse.wavepicking.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class WaveResponse {

    private Long id;
    private String waveNo;
    private String status;
    private String waveType;
    private Integer totalOrderCount;
    private Integer totalSkuCount;
    private Integer totalQuantity;
    private String zone;
    private String operator;
    private List<OrderSummaryResponse> orders;
    private List<PickingTaskResponse> pickingTasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime releasedAt;
    private LocalDateTime completedAt;

    public WaveResponse() {
    }

    public WaveResponse(Long id, String waveNo, String status, String waveType, Integer totalOrderCount, Integer totalSkuCount, Integer totalQuantity, String zone, String operator, List<OrderSummaryResponse> orders, List<PickingTaskResponse> pickingTasks, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime releasedAt, LocalDateTime completedAt) {
        this.id = id;
        this.waveNo = waveNo;
        this.status = status;
        this.waveType = waveType;
        this.totalOrderCount = totalOrderCount;
        this.totalSkuCount = totalSkuCount;
        this.totalQuantity = totalQuantity;
        this.zone = zone;
        this.operator = operator;
        this.orders = orders;
        this.pickingTasks = pickingTasks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.releasedAt = releasedAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWaveNo() {
        return waveNo;
    }

    public void setWaveNo(String waveNo) {
        this.waveNo = waveNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWaveType() {
        return waveType;
    }

    public void setWaveType(String waveType) {
        this.waveType = waveType;
    }

    public Integer getTotalOrderCount() {
        return totalOrderCount;
    }

    public void setTotalOrderCount(Integer totalOrderCount) {
        this.totalOrderCount = totalOrderCount;
    }

    public Integer getTotalSkuCount() {
        return totalSkuCount;
    }

    public void setTotalSkuCount(Integer totalSkuCount) {
        this.totalSkuCount = totalSkuCount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<OrderSummaryResponse> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderSummaryResponse> orders) {
        this.orders = orders;
    }

    public List<PickingTaskResponse> getPickingTasks() {
        return pickingTasks;
    }

    public void setPickingTasks(List<PickingTaskResponse> pickingTasks) {
        this.pickingTasks = pickingTasks;
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

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String waveNo;
        private String status;
        private String waveType;
        private Integer totalOrderCount;
        private Integer totalSkuCount;
        private Integer totalQuantity;
        private String zone;
        private String operator;
        private List<OrderSummaryResponse> orders;
        private List<PickingTaskResponse> pickingTasks;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime releasedAt;
        private LocalDateTime completedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder waveNo(String waveNo) {
            this.waveNo = waveNo;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder waveType(String waveType) {
            this.waveType = waveType;
            return this;
        }

        public Builder totalOrderCount(Integer totalOrderCount) {
            this.totalOrderCount = totalOrderCount;
            return this;
        }

        public Builder totalSkuCount(Integer totalSkuCount) {
            this.totalSkuCount = totalSkuCount;
            return this;
        }

        public Builder totalQuantity(Integer totalQuantity) {
            this.totalQuantity = totalQuantity;
            return this;
        }

        public Builder zone(String zone) {
            this.zone = zone;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder orders(List<OrderSummaryResponse> orders) {
            this.orders = orders;
            return this;
        }

        public Builder pickingTasks(List<PickingTaskResponse> pickingTasks) {
            this.pickingTasks = pickingTasks;
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

        public Builder releasedAt(LocalDateTime releasedAt) {
            this.releasedAt = releasedAt;
            return this;
        }

        public Builder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public WaveResponse build() {
            return new WaveResponse(id, waveNo, status, waveType, totalOrderCount, totalSkuCount, totalQuantity, zone, operator, orders, pickingTasks, createdAt, updatedAt, releasedAt, completedAt);
        }
    }
}
