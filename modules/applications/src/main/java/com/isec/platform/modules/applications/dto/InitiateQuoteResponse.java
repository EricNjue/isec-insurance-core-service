package com.isec.platform.modules.applications.dto;

import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateQuoteResponse {
    private String quoteId;
    private List<ApplicationDocumentDto> documents;
    private DoubleInsuranceCheckResponse doubleInsuranceCheck;
}
