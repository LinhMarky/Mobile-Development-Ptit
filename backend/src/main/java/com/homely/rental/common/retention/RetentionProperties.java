package com.homely.rental.common.retention;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "homely.retention")
@Validated
@Getter
@Setter
public class RetentionProperties {
    @Min(1) @Max(1000) private int batchSize = 200;
    @Min(1) private int pushDays = 30;
}
