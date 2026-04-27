package com.isec.platform.modules.customers.service;

import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.modules.customers.domain.Customer;
import com.isec.platform.modules.customers.dto.CustomerRequest;
import com.isec.platform.modules.customers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Mono<Customer> createOrUpdateCustomer(String userId, CustomerRequest request) {
        log.info("Creating or updating customer for userId: {}", userId);
        
        return customerRepository.findByUserId(userId)
                .flatMap(existing -> {
                    log.debug("Updating existing customer for userId: {}", userId);
                    existing.setFullName(request.getFullName());
                    existing.setEmail(request.getEmail());
                    existing.setPhoneNumber(request.getPhoneNumber());
                    existing.setPhysicalAddress(request.getPhysicalAddress());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return customerRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Creating new customer for userId: {}", userId);
                    Customer customer = Customer.builder()
                            .userId(userId)
                            .fullName(request.getFullName())
                            .email(request.getEmail())
                            .phoneNumber(request.getPhoneNumber())
                            .physicalAddress(request.getPhysicalAddress())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return customerRepository.save(customer);
                }));
    }

    public Mono<Customer> getCustomerByUserId(String userId) {
        return customerRepository.findByUserId(userId);
    }

    public Mono<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer", id)));
    }
}
