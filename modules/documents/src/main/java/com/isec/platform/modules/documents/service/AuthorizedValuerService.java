package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import com.isec.platform.modules.documents.dto.AuthorizedValuerRequest;
import com.isec.platform.modules.documents.repository.AuthorizedValuerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizedValuerService {

    private final AuthorizedValuerRepository repository;

    public List<AuthorizedValuer> listActive() {
        return repository.findByActiveTrue();
    }

    @Transactional
    public AuthorizedValuer create(AuthorizedValuerRequest req) {
        AuthorizedValuer valuer = AuthorizedValuer.builder()
                .companyName(req.getCompanyName())
                .contactPerson(req.getContactPerson())
                .email(req.getEmail())
                .phoneNumbers(req.getPhoneNumbers())
                .locations(req.getLocations())
                .active(req.getActive() == null ? true : req.getActive())
                .build();
        return repository.save(valuer);
    }

    @Transactional
    public AuthorizedValuer update(Long id, AuthorizedValuerRequest req) {
        AuthorizedValuer v = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Valuer not found: " + id));
        if (req.getCompanyName() != null) v.setCompanyName(req.getCompanyName());
        if (req.getContactPerson() != null) v.setContactPerson(req.getContactPerson());
        if (req.getEmail() != null) v.setEmail(req.getEmail());
        if (req.getPhoneNumbers() != null) v.setPhoneNumbers(req.getPhoneNumbers());
        if (req.getLocations() != null) v.setLocations(req.getLocations());
        if (req.getActive() != null) v.setActive(req.getActive());
        return repository.save(v);
    }

    @Transactional
    public void deactivate(Long id) {
        AuthorizedValuer v = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Valuer not found: " + id));
        v.setActive(false);
        repository.save(v);
    }
}
