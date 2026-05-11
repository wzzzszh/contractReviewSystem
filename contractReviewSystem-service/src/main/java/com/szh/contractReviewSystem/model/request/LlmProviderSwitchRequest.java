package com.szh.contractReviewSystem.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LlmProviderSwitchRequest {

    @NotBlank(message = "provider cannot be blank")
    private String provider;
}
