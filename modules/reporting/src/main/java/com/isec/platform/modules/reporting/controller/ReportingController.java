package com.isec.platform.modules.reporting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportingController {

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        
        String csvData = "Registration Number,Vehicle Value,Premium Paid,Policy Start Date,Policy Expiry Date\n" +
                         "KAA 001Z,2000000,75377.50,2024-01-01,2024-12-31";
        
        byte[] csvBytes = csvData.getBytes();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + startDate + "-to-" + endDate + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}
