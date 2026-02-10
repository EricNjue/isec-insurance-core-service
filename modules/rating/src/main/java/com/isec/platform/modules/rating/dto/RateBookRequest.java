package com.isec.platform.modules.rating.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RateBookRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String versionName;
    
    @NotNull
    private LocalDateTime effectiveFrom;
    
    private LocalDateTime effectiveTo;
    
    private boolean active;
}
