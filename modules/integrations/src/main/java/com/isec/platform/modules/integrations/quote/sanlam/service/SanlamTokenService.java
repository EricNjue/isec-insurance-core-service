package com.isec.platform.modules.integrations.quote.sanlam.service;

import com.isec.platform.common.utils.JwtTokenUtil;
import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SanlamTokenService {

    private final SanlamClient sanlamClient;

    /**
     * Resolves the Sanlam user ID from the access token's 'sub' claim.
     * Uses cached token if available.
     *
     * @return Mono containing the numeric user ID (sub claim)
     */
    public Mono<Long> resolveSanlamUserId() {
        return sanlamClient.getAccessToken()
                .map(token -> {
                    String sub = JwtTokenUtil.extractSubject(token);
                    try {
                        Long userId = Long.valueOf(sub);
                        log.info("Resolved Sanlam draftQuoteUserId from access token subject. userId={}", userId);
                        return userId;
                    } catch (NumberFormatException e) {
                        log.error("Sanlam token 'sub' claim is not numeric: {}", sub);
                        throw new IllegalArgumentException("Sanlam token 'sub' claim must be numeric");
                    }
                })
                .doOnError(e -> log.error("Failed to resolve Sanlam user ID from token: {}", e.getMessage()));
    }
}
