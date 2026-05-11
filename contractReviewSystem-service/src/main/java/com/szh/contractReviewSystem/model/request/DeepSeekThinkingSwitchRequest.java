package com.szh.contractReviewSystem.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DeepSeekThinkingSwitchRequest {

    @NotNull(message = "enabled must not be null")
    private Boolean enabled;
}
