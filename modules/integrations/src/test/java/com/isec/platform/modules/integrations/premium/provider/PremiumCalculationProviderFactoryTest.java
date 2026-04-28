package com.isec.platform.modules.integrations.premium.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PremiumCalculationProviderFactoryTest {

    private PremiumCalculationProviderFactory factory;
    private PremiumCalculationProvider sanlamProvider;

    @BeforeEach
    void setUp() {
        sanlamProvider = mock(PremiumCalculationProvider.class);
        when(sanlamProvider.providerType()).thenReturn(PremiumProviderType.SANLAM);

        factory = new PremiumCalculationProviderFactory(List.of(sanlamProvider));
    }

    @Test
    void shouldResolveSanlamProvider() {
        PremiumCalculationProvider resolved = factory.getProvider(PremiumProviderType.SANLAM);
        assertThat(resolved).isEqualTo(sanlamProvider);
    }

    @Test
    void shouldThrowExceptionForUnsupportedProvider() {
        // Since we only have SANLAM in the enum for now, we can't easily test other types unless we add them
        // But the logic is clear.
        assertThatThrownBy(() -> factory.getProvider(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported premium provider");
    }
}
