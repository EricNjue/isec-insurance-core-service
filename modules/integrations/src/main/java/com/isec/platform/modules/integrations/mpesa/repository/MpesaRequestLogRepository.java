package com.isec.platform.modules.integrations.mpesa.repository;

import com.isec.platform.modules.integrations.mpesa.domain.MpesaRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MpesaRequestLogRepository extends JpaRepository<MpesaRequestLog, Long> {
    Optional<MpesaRequestLog> findByCheckoutRequestId(String checkoutRequestId);
}
