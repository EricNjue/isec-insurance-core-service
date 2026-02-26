package com.isec.platform.modules.certificates.service;

import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.certificates.application.CertificateApplicationService;
import com.isec.platform.modules.certificates.domain.canonical.CertificateRequest;
import com.isec.platform.modules.certificates.domain.canonical.CertificateType;
import com.isec.platform.modules.certificates.domain.canonical.CustomerDetails;
import com.isec.platform.modules.certificates.domain.canonical.Money;
import com.isec.platform.modules.certificates.domain.canonical.PolicyDetails;
import com.isec.platform.modules.certificates.domain.canonical.VehicleDetails;
import com.isec.platform.modules.customers.domain.Customer;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.policies.domain.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private static final BigDecimal MONTHLY_PREMIUM_RATE = new BigDecimal("0.35");

    private final CertificateApplicationService certificateApplicationService;
    private final ApplicationRepository applicationRepository;
    private final CustomerService customerService;

    @Transactional
    public void processCertificateIssuance(Policy policy, BigDecimal amountPaid, String recipientEmail, String recipientPhoneNumber) {
        log.info("Processing certificate issuance for policy: {}, amount paid: {}", policy.getPolicyNumber(), amountPaid);

        BigDecimal annualPremium = policy.getTotalAnnualPremium();
        BigDecimal monthlyRequirement = annualPremium.multiply(MONTHLY_PREMIUM_RATE).setScale(2, RoundingMode.HALF_UP);

        Application application = applicationRepository.findById(policy.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found for policy: " + policy.getId()));

        Optional<Customer> customer = customerService.getCustomerByUserId(application.getUserId());

        CustomerDetails customerDetails = buildCustomerDetails(customer, recipientEmail, recipientPhoneNumber);
        VehicleDetails vehicleDetails = buildVehicleDetails(application);
        PolicyDetails policyDetails = new PolicyDetails(
                policy.getPolicyNumber(),
                policy.getStartDate(),
                policy.getExpiryDate(),
                null
        );
        Money premium = new Money(policy.getTotalAnnualPremium(), "KES");

        // Month 1 & 2 logic
        if (amountPaid.compareTo(monthlyRequirement) >= 0) {
            issueCertificate(policy, application, CertificateType.MONTH_1, policy.getStartDate(), policy.getStartDate().plusMonths(1).minusDays(1),
                    customerDetails, vehicleDetails, policyDetails, premium);

            if (amountPaid.compareTo(monthlyRequirement.multiply(new BigDecimal("2"))) >= 0) {
                issueCertificate(policy, application, CertificateType.MONTH_2, policy.getStartDate().plusMonths(1), policy.getStartDate().plusMonths(2).minusDays(1),
                        customerDetails, vehicleDetails, policyDetails, premium);
            }
        }

        // Month 3 (Annual Remainder) logic - Only if balance is zero
        if (policy.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            boolean hasMonth1 = amountPaid.compareTo(monthlyRequirement) >= 0;
            if (hasMonth1) {
                issueCertificate(policy, application, CertificateType.ANNUAL_REMAINDER, policy.getStartDate().plusMonths(2), policy.getExpiryDate(),
                        customerDetails, vehicleDetails, policyDetails, premium);
            } else {
                issueCertificate(policy, application, CertificateType.ANNUAL_FULL, policy.getStartDate(), policy.getExpiryDate(),
                        customerDetails, vehicleDetails, policyDetails, premium);
            }
        }
    }

    private void issueCertificate(Policy policy,
                                  Application application,
                                  CertificateType certificateType,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  CustomerDetails customerDetails,
                                  VehicleDetails vehicleDetails,
                                  PolicyDetails policyDetails,
                                  Money premium) {
        String idempotencyKey = deterministicKey(policy.getId(), certificateType, startDate, endDate);
        CertificateRequest request = new CertificateRequest(
                idempotencyKey,
                certificateType,
                null,
                new PolicyDetails(policyDetails.policyNumber(), startDate, endDate, policyDetails.productType()),
                customerDetails,
                vehicleDetails,
                premium
        );
        log.info("Issuing certificate type {} for policy {}", certificateType, policy.getPolicyNumber());
        certificateApplicationService.issueCertificate(request);
    }

    private CustomerDetails buildCustomerDetails(Optional<Customer> customer, String recipientEmail, String recipientPhoneNumber) {
        String firstName = "Unknown";
        String lastName = "Customer";
        String email = recipientEmail;
        String phone = recipientPhoneNumber;

        if (customer.isPresent()) {
            Customer c = customer.get();
            String[] parts = c.getFullName() == null ? new String[0] : c.getFullName().trim().split("\\s+", 2);
            if (parts.length > 0) {
                firstName = parts[0];
            }
            if (parts.length > 1) {
                lastName = parts[1];
            }
            email = c.getEmail();
            phone = c.getPhoneNumber();
        }

        return new CustomerDetails(firstName, lastName, null, email, phone, null, null, null);
    }

    private VehicleDetails buildVehicleDetails(Application application) {
        return new VehicleDetails(
                application.getRegistrationNumber(),
                application.getVehicleMake(),
                application.getVehicleModel(),
                application.getChassisNumber(),
                application.getEngineNumber(),
                null
        );
    }

    private String deterministicKey(Long policyId, CertificateType certificateType, LocalDate startDate, LocalDate endDate) {
        String input = policyId + ":" + certificateType + ":" + startDate + ":" + endDate;
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
