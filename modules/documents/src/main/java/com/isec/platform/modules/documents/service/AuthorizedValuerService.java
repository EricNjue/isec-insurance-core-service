package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import com.isec.platform.modules.documents.dto.AuthorizedValuerRequest;
import com.isec.platform.modules.documents.repository.AuthorizedValuerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizedValuerService {

    private final AuthorizedValuerRepository repository;

    public Flux<AuthorizedValuer> listActive() {
        return repository.findByActiveTrue();
    }

    public Mono<AuthorizedValuer> create(AuthorizedValuerRequest req) {
        AuthorizedValuer valuer = AuthorizedValuer.builder()
                .companyName(req.getCompanyName())
                .contactPerson(req.getContactPerson())
                .email(req.getEmail())
                .phoneNumbers(req.getPhoneNumbers())
                .locations(req.getLocations())
                .active(req.getActive() == null ? true : req.getActive())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return repository.save(valuer);
    }

    public Mono<AuthorizedValuer> update(Long id, AuthorizedValuerRequest req) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Valuer not found: " + id)))
                .flatMap(v -> {
                    if (req.getCompanyName() != null) v.setCompanyName(req.getCompanyName());
                    if (req.getContactPerson() != null) v.setContactPerson(req.getContactPerson());
                    if (req.getEmail() != null) v.setEmail(req.getEmail());
                    if (req.getPhoneNumbers() != null) v.setPhoneNumbers(req.getPhoneNumbers());
                    if (req.getLocations() != null) v.setLocations(req.getLocations());
                    if (req.getActive() != null) v.setActive(req.getActive());
                    v.setUpdatedAt(LocalDateTime.now());
                    return repository.save(v);
                });
    }

    public Mono<Void> deactivate(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Valuer not found: " + id)))
                .flatMap(v -> {
                    v.setActive(false);
                    v.setUpdatedAt(LocalDateTime.now());
                    return repository.save(v);
                }).then();
    }
}
