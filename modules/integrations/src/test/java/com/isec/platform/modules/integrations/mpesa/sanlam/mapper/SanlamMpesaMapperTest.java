package com.isec.platform.modules.integrations.mpesa.sanlam.mapper;

import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkStatusResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanlamMpesaMapperTest {

    private final SanlamMpesaMapper mapper = new SanlamMpesaMapper();

    @Test
    void mapStatus_Success_ShouldReturnSUCCESS() {
        SanlamStkStatusResponse response = SanlamStkStatusResponse.builder()
                .status("success")
                .build();
        assertEquals(MpesaPaymentStatus.SUCCESS, mapper.toMpesaPaymentStatusResponse(response, "checkout123").getStatus());
    }

    @Test
    void mapStatus_PendingCode_ShouldReturnPENDING() {
        SanlamStkStatusResponse response = SanlamStkStatusResponse.builder()
                .status("failed")
                .raw(new SanlamStkStatusResponse.RawResponse(false, "Processing", null, null, "4999"))
                .build();
        assertEquals(MpesaPaymentStatus.PENDING, mapper.toMpesaPaymentStatusResponse(response, "checkout123").getStatus());
    }

    @Test
    void mapStatus_TimeoutCode_ShouldReturnTIMEOUT() {
        SanlamStkStatusResponse response = SanlamStkStatusResponse.builder()
                .status("failed")
                .raw(new SanlamStkStatusResponse.RawResponse(false, "Timeout", null, null, "1037"))
                .build();
        assertEquals(MpesaPaymentStatus.TIMEOUT, mapper.toMpesaPaymentStatusResponse(response, "checkout123").getStatus());
    }

    @Test
    void mapStatus_CancelledCode_ShouldReturnCANCELLED() {
        SanlamStkStatusResponse response = SanlamStkStatusResponse.builder()
                .status("failed")
                .raw(new SanlamStkStatusResponse.RawResponse(false, "Cancelled", null, null, "1032"))
                .build();
        assertEquals(MpesaPaymentStatus.CANCELLED, mapper.toMpesaPaymentStatusResponse(response, "checkout123").getStatus());
    }

    @Test
    void mapStatus_OtherFailedCode_ShouldReturnFAILED() {
        SanlamStkStatusResponse response = SanlamStkStatusResponse.builder()
                .status("failed")
                .raw(new SanlamStkStatusResponse.RawResponse(false, "Error", null, null, "1234"))
                .build();
        assertEquals(MpesaPaymentStatus.FAILED, mapper.toMpesaPaymentStatusResponse(response, "checkout123").getStatus());
    }

    @Test
    void mapStatus_NullErrorCode_ShouldReturnUNKNOWN() {
        SanlamStkStatusResponse response = SanlamStkStatusResponse.builder()
                .status("failed")
                .build();
        assertEquals(MpesaPaymentStatus.UNKNOWN, mapper.toMpesaPaymentStatusResponse(response, "checkout123").getStatus());
    }
}
