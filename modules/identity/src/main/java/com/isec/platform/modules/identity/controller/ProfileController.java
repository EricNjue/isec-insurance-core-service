package com.isec.platform.modules.identity.controller;

import com.isec.platform.modules.identity.dto.UserProfile;
import com.isec.platform.modules.identity.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public Mono<UserProfile> getProfile() {
        return profileService.getUserProfile();
    }
}
