package com.isec.platform.modules.applications.dto.motor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaInitiationRequest {
    private String phoneNumber; // Optional override
}
