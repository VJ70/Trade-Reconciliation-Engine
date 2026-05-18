package com.tradereconciliation.controller;

import com.tradereconciliation.model.PnlSummary;
import com.tradereconciliation.service.PnlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pnl")
@RequiredArgsConstructor
@Tag(name = "P&L", description = "Daily P&L calculation and retrieval")
@SecurityRequirement(name = "bearerAuth")
public class PnlController {

    private final PnlService pnlService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @Operation(summary = "Get P&L for a given date")
    public ResponseEntity<List<PnlSummary>> getPnl(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(pnlService.getPnlForDate(date));
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger P&L calculation for a date (ADMIN only)")
    public ResponseEntity<List<PnlSummary>> calculate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(pnlService.calculatePnl(date));
    }
}
