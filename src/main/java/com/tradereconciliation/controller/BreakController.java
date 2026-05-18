package com.tradereconciliation.controller;

import com.tradereconciliation.dto.response.BreakResponse;
import com.tradereconciliation.service.BreakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/breaks")
@RequiredArgsConstructor
@Tag(name = "Breaks", description = "Reconciliation break management")
@SecurityRequirement(name = "bearerAuth")
public class BreakController {

    private final BreakService breakService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all breaks")
    public ResponseEntity<List<BreakResponse>> list(
            @RequestParam(required = false, defaultValue = "false") boolean unresolvedOnly) {
        return ResponseEntity.ok(unresolvedOnly
                ? breakService.getUnresolvedBreaks()
                : breakService.getAllBreaks());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get break detail")
    public ResponseEntity<BreakResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(breakService.getById(id));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark a break as resolved")
    public ResponseEntity<BreakResponse> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(breakService.resolveBreak(id));
    }
}
