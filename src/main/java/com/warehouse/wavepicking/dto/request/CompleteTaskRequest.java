package com.warehouse.wavepicking.dto.request;

import jakarta.validation.constraints.NotNull;

public class CompleteTaskRequest {

    @NotNull(message = "拣货数量不能为空")
    private Integer pickedQuantity;

    public Integer getPickedQuantity() {
        return pickedQuantity;
    }

    public void setPickedQuantity(Integer pickedQuantity) {
        this.pickedQuantity = pickedQuantity;
    }
}
