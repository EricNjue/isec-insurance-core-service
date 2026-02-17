package com.isec.platform.modules.identity.controller;

import com.isec.platform.modules.identity.dto.UserProfile;
import com.isec.platform.modules.identity.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<UserProfile> getProfile() {
        return ResponseEntity.ok(profileService.getUserProfile());
    }
}
