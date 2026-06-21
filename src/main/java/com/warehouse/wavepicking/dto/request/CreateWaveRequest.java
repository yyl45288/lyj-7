package com.warehouse.wavepicking.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateWaveRequest {

    private String waveType = "NORMAL";

    private Integer maxOrderCount = 10;

    private String zone;

    private List<Long> orderIds;

    public String getWaveType() {
        return waveType;
    }

    public void setWaveType(String waveType) {
        this.waveType = waveType;
    }

    public Integer getMaxOrderCount() {
        return maxOrderCount;
    }

    public void setMaxOrderCount(Integer maxOrderCount) {
        this.maxOrderCount = maxOrderCount;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public List<Long> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<Long> orderIds) {
        this.orderIds = orderIds;
    }
}
