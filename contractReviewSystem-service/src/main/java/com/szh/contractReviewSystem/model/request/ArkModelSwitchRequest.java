package com.szh.contractReviewSystem.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ArkModelSwitchRequest {

    @NotBlank(message = "model cannot be blank")
    private String model;
}
