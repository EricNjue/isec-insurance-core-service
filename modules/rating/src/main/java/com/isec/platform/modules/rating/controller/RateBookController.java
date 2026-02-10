package com.isec.platform.modules.rating.controller;

import com.isec.platform.modules.rating.domain.RateBook;
import com.isec.platform.modules.rating.dto.RateBookRequest;
import com.isec.platform.modules.rating.service.RateBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rating/rate-books")
@RequiredArgsConstructor
public class RateBookController {

    private final RateBookService rateBookService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public ResponseEntity<RateBook> createRateBook(@Valid @RequestBody RateBookRequest request) {
        return ResponseEntity.ok(rateBookService.createRateBook(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public ResponseEntity<RateBook> updateRateBook(@PathVariable Long id, @Valid @RequestBody RateBookRequest request) {
        return ResponseEntity.ok(rateBookService.updateRateBook(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public ResponseEntity<RateBook> getRateBook(@PathVariable Long id) {
        return ResponseEntity.ok(rateBookService.getRateBook(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public ResponseEntity<List<RateBook>> listRateBooks() {
        return ResponseEntity.ok(rateBookService.listRateBooks());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public ResponseEntity<Void> deleteRateBook(@PathVariable Long id) {
        rateBookService.deleteRateBook(id);
        return ResponseEntity.noContent().build();
    }
}
