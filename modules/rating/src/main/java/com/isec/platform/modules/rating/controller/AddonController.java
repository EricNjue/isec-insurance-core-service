package com.isec.platform.modules.rating.controller;

import com.isec.platform.modules.rating.dto.AddonDto;
import com.isec.platform.modules.rating.service.AddonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rating/addons")
@RequiredArgsConstructor
@Slf4j
public class AddonController {

    private final AddonService addonService;

    @GetMapping
    public ResponseEntity<List<AddonDto>> getAvailableAddons(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(addonService.getAddonsByCategory(category));
        }
        return ResponseEntity.ok(addonService.getAvailableAddons());
    }
}
