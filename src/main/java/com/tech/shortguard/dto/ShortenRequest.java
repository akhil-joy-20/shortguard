package com.tech.shortguard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortenRequest {

    @NotBlank(message = "URL cannot be empty")
    @Pattern(
            regexp = "^(http|https)://.*",
            message = "URL must start with http:// or https://"
    )
    private String url;

    @Min(value = 1, message = "Expiry days must be at least 1")
    @Max(value = 365, message = "Expiry days cannot exceed 365")
    private int expiryDays = 30;

}
