package com.warehouse.wavepicking.dto.response;

import java.time.LocalDateTime;

public class PickingTaskResponse {

    private Long id;
    private String taskNo;
    private Long waveId;
    private String waveNo;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Integer quantity;
    private Integer pickedQuantity;
    private String location;
    private String status;
    private String picker;
    private String zone;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public PickingTaskResponse() {
    }

    public PickingTaskResponse(Long id, String taskNo, Long waveId, String waveNo, Long skuId, String skuCode, String skuName, Integer quantity, Integer pickedQuantity, String location, String status, String picker, String zone, Integer priority, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime startedAt, LocalDateTime completedAt) {
        this.id = id;
        this.taskNo = taskNo;
        this.waveId = waveId;
        this.waveNo = waveNo;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.quantity = quantity;
        this.pickedQuantity = pickedQuantity;
        this.location = location;
        this.status = status;
        this.picker = picker;
        this.zone = zone;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPicker() {
        return picker;
    }

    public void setPicker(String picker) {
        this.picker = picker;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
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
        private String taskNo;
        private Long waveId;
        private String waveNo;
        private Long skuId;
        private String skuCode;
        private String skuName;
        private Integer quantity;
        private Integer pickedQuantity;
        private String location;
        private String status;
        private String picker;
        private String zone;
        private Integer priority;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder taskNo(String taskNo) {
            this.taskNo = taskNo;
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

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder picker(String picker) {
            this.picker = picker;
            return this;
        }

        public Builder zone(String zone) {
            this.zone = zone;
            return this;
        }

        public Builder priority(Integer priority) {
            this.priority = priority;
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

        public Builder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public PickingTaskResponse build() {
            return new PickingTaskResponse(id, taskNo, waveId, waveNo, skuId, skuCode, skuName, quantity, pickedQuantity, location, status, picker, zone, priority, createdAt, updatedAt, startedAt, completedAt);
        }
    }
}
