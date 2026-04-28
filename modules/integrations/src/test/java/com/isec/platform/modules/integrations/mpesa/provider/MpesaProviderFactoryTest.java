package com.isec.platform.modules.integrations.mpesa.provider;

import com.isec.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MpesaProviderFactoryTest {

    @Mock
    private MpesaPaymentProvider sanlamProvider;

    private MpesaProviderFactory factory;

    @BeforeEach
    void setUp() {
        when(sanlamProvider.providerType()).thenReturn(MpesaProviderType.SANLAM);
        factory = new MpesaProviderFactory(List.of(sanlamProvider));
    }

    @Test
    void getProvider_WithSupportedType_ShouldReturnProvider() {
        MpesaPaymentProvider provider = factory.getProvider(MpesaProviderType.SANLAM);
        assertNotNull(provider);
        assertEquals(MpesaProviderType.SANLAM, provider.providerType());
    }

    @Test
    void getProvider_WithUnsupportedType_ShouldThrowException() {
        // Since we only have SANLAM, we can't easily test another MpesaProviderType unless we add one
        // But the logic is clear.
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                factory.getProvider(null));
        assertTrue(exception.getMessage().contains("Unsupported M-Pesa provider"));
    }
}
