package com.warehouse.wavepicking.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AssignTaskRequest {

    @NotBlank(message = "拣货员不能为空")
    private String picker;

    public String getPicker() {
        return picker;
    }

    public void setPicker(String picker) {
        this.picker = picker;
    }
}
