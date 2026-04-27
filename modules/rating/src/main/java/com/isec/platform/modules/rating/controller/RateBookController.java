package com.isec.platform.modules.rating.controller;

import com.isec.platform.modules.rating.domain.RateBook;
import com.isec.platform.modules.rating.dto.RateBookRequest;
import com.isec.platform.modules.rating.service.RateBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rating/rate-books")
@RequiredArgsConstructor
public class RateBookController {

    private final RateBookService rateBookService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public Mono<ResponseEntity<RateBook>> createRateBook(@Valid @RequestBody RateBookRequest request) {
        return rateBookService.createRateBook(request)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public Mono<ResponseEntity<RateBook>> updateRateBook(@PathVariable Long id, @Valid @RequestBody RateBookRequest request) {
        return rateBookService.updateRateBook(id, request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public Mono<ResponseEntity<RateBook>> getRateBook(@PathVariable Long id) {
        return rateBookService.getRateBook(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public Flux<RateBook> listRateBooks() {
        return rateBookService.listRateBooks();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public Mono<ResponseEntity<Void>> deleteRateBook(@PathVariable Long id) {
        return rateBookService.deleteRateBook(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
