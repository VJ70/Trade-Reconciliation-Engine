package com.tradereconciliation.controller;

import com.tradereconciliation.dto.response.ReconciliationResult;
import com.tradereconciliation.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reconcile")
@RequiredArgsConstructor
@Tag(name = "Reconciliation", description = "Trigger and monitor reconciliation runs")
@SecurityRequirement(name = "bearerAuth")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger a full reconciliation run (ADMIN only)")
    public ResponseEntity<ReconciliationResult> run() {
        return ResponseEntity.ok(reconciliationService.reconcile());
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get last reconciliation run stats")
    public ResponseEntity<ReconciliationResult> status() {
        ReconciliationResult result = reconciliationService.getLastRunResult();
        if (result == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(result);
    }
}
