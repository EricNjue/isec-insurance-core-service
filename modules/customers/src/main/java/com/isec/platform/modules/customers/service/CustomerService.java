package com.isec.platform.modules.customers.service;

import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.modules.customers.domain.Customer;
import com.isec.platform.modules.customers.dto.CustomerRequest;
import com.isec.platform.modules.customers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer createOrUpdateCustomer(String userId, CustomerRequest request) {
        log.info("Creating or updating customer for userId: {}", userId);
        
        return customerRepository.findByUserId(userId)
                .map(existing -> {
                    log.debug("Updating existing customer for userId: {}", userId);
                    existing.setFullName(request.getFullName());
                    existing.setEmail(request.getEmail());
                    existing.setPhoneNumber(request.getPhoneNumber());
                    return customerRepository.save(existing);
                })
                .orElseGet(() -> {
                    log.debug("Creating new customer for userId: {}", userId);
                    Customer customer = Customer.builder()
                            .userId(userId)
                            .fullName(request.getFullName())
                            .email(request.getEmail())
                            .phoneNumber(request.getPhoneNumber())
                            .build();
                    return customerRepository.save(customer);
                });
    }

    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerByUserId(String userId) {
        return customerRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }
}
