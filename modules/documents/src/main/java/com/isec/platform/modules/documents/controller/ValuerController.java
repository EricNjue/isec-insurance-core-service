package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import com.isec.platform.modules.documents.dto.AuthorizedValuerRequest;
import com.isec.platform.modules.documents.service.AuthorizedValuerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/valuers")
@RequiredArgsConstructor
public class ValuerController {

    private final AuthorizedValuerService service;

    @GetMapping
    public ResponseEntity<List<AuthorizedValuer>> listActive() {
        return ResponseEntity.ok(service.listActive());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthorizedValuer> create(@RequestBody AuthorizedValuerRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthorizedValuer> update(@PathVariable Long id, @RequestBody AuthorizedValuerRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
