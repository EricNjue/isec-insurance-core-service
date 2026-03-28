package com.isec.platform.modules.integrations.registry.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyRequest;
import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyResponse;
import com.isec.platform.modules.integrations.registry.entity.IntegrationCompany;
import com.isec.platform.modules.integrations.registry.mapper.IntegrationCompanyMapper;
import com.isec.platform.modules.integrations.registry.repository.IntegrationCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationCompanyServiceTest {

    @Mock
    private IntegrationCompanyRepository repository;

    @Mock
    private IntegrationCompanyMapper mapper;

    @InjectMocks
    private IntegrationCompanyService service;

    private IntegrationCompanyRequest request;
    private IntegrationCompany entity;

    @BeforeEach
    void setUp() {
        request = IntegrationCompanyRequest.builder()
                .code("SANLAM")
                .name("Sanlam Insurance")
                .active(true)
                .build();

        entity = IntegrationCompany.builder()
                .id(1L)
                .code("SANLAM")
                .name("Sanlam Insurance")
                .active(true)
                .build();
    }

    @Test
    void createIntegration_Success() {
        when(repository.existsByCode("SANLAM")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(any(IntegrationCompany.class))).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(IntegrationCompanyResponse.builder().code("SANLAM").build());

        IntegrationCompanyResponse response = service.createIntegration(request);

        assertNotNull(response);
        assertEquals("SANLAM", response.getCode());
        verify(repository).save(any(IntegrationCompany.class));
    }

    @Test
    void createIntegration_DuplicateCode_ThrowsException() {
        when(repository.existsByCode("SANLAM")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.createIntegration(request));
        verify(repository, never()).save(any());
    }

    @Test
    void getIntegrationById_NotFound_ThrowsException() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getIntegrationById(1L));
    }

    @Test
    void deleteIntegration_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.deleteIntegration(1L);

        assertTrue(entity.isDeleted());
        assertFalse(entity.isActive());
        verify(repository).save(entity);
    }
}
