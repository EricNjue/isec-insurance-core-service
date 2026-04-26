package com.isec.platform.modules.applications.controller;

import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.service.UnderwritingService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/underwriting")
@RequiredArgsConstructor
@Slf4j
public class UnderwritingController {

    private final UnderwritingService underwritingService;

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN')")
    public Mono<ResponseEntity<Application>> approve(@PathVariable Long applicationId,
                                               @RequestParam @NotBlank String underwriterId,
                                               @RequestParam(required = false) String comments) {
        log.info("Underwriting approve for application {} by {}", applicationId, underwriterId);
        return underwritingService.approve(applicationId, underwriterId, comments)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/decline")
    @PreAuthorize("hasAnyRole('UNDERWRITER','ADMIN')")
    public Mono<ResponseEntity<Application>> decline(@PathVariable Long applicationId,
                                               @RequestParam @NotBlank String underwriterId,
                                               @RequestParam(required = false) String comments,
                                               @RequestParam @NotBlank String reason) {
        log.info("Underwriting decline for application {} by {}", applicationId, underwriterId);
        return underwritingService.decline(applicationId, underwriterId, comments, reason)
                .map(ResponseEntity::ok);
    }
}
