package com.szh.contractReviewSystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class AuthJwtProperties {

    private String secret = "contract-review-dev-secret-change-me";

    private long accessExpirationMinutes = 30;

    private long refreshExpirationDays = 7;
}
