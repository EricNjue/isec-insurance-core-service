package com.isec.platform.modules.integrations.quote.sanlam.service;

import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SanlamTokenServiceTest {

    @Mock
    private SanlamClient sanlamClient;

    @InjectMocks
    private SanlamTokenService sanlamTokenService;

    private String createToken(String sub) {
        String payload = "{\"sub\":\"" + sub + "\"}";
        String encodedPayload = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "header." + encodedPayload + ".signature";
    }

    @Test
    void shouldResolveNumericUserId() {
        String token = createToken("12345");
        when(sanlamClient.getAccessToken()).thenReturn(Mono.just(token));

        StepVerifier.create(sanlamTokenService.resolveSanlamUserId())
                .expectNext(12345L)
                .verifyComplete();
    }

    @Test
    void shouldFailForNonNumericSub() {
        String token = createToken("not-a-number");
        when(sanlamClient.getAccessToken()).thenReturn(Mono.just(token));

        StepVerifier.create(sanlamTokenService.resolveSanlamUserId())
                .expectErrorMatches(e -> e instanceof IllegalArgumentException && 
                        e.getMessage().contains("must be numeric"))
                .verify();
    }

    @Test
    void shouldFailForMissingSub() {
        String payload = "{\"name\":\"John Doe\"}";
        String encodedPayload = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String token = "header." + encodedPayload + ".signature";
        
        when(sanlamClient.getAccessToken()).thenReturn(Mono.just(token));

        StepVerifier.create(sanlamTokenService.resolveSanlamUserId())
                .expectErrorMatches(e -> e instanceof IllegalArgumentException && 
                        e.getMessage().contains("missing 'sub' claim"))
                .verify();
    }

    @Test
    void shouldFailForBlankSub() {
        String token = createToken("");
        when(sanlamClient.getAccessToken()).thenReturn(Mono.just(token));

        StepVerifier.create(sanlamTokenService.resolveSanlamUserId())
                .expectErrorMatches(e -> e instanceof IllegalArgumentException && 
                        e.getMessage().contains("missing 'sub' claim"))
                .verify();
    }
}
