package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import com.isec.platform.modules.documents.dto.AuthorizedValuerRequest;
import com.isec.platform.modules.documents.service.AuthorizedValuerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/valuers")
@RequiredArgsConstructor
public class ValuerController {

    private final AuthorizedValuerService service;

    @GetMapping
    public Flux<AuthorizedValuer> listActive() {
        return service.listActive();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<AuthorizedValuer> create(@RequestBody AuthorizedValuerRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<AuthorizedValuer> update(@PathVariable Long id, @RequestBody AuthorizedValuerRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> deactivate(@PathVariable Long id) {
        return service.deactivate(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
