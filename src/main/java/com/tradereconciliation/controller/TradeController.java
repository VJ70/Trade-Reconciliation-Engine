package com.tradereconciliation.controller;

import com.tradereconciliation.dto.request.TradeRequest;
import com.tradereconciliation.dto.response.TradeResponse;
import com.tradereconciliation.elasticsearch.TradeDocument;
import com.tradereconciliation.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Tag(name = "Trades", description = "Trade ingestion and query")
@SecurityRequirement(name = "bearerAuth")
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @Operation(summary = "Ingest a new trade (internal or broker)")
    public ResponseEntity<TradeResponse> ingest(@Valid @RequestBody TradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.ingestTrade(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @Operation(summary = "Get trade by ID")
    public ResponseEntity<TradeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tradeService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @Operation(summary = "List all trades (paginated)")
    public ResponseEntity<Page<TradeResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(tradeService.findAll(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @Operation(summary = "Full-text search trades via ElasticSearch")
    public ResponseEntity<List<TradeDocument>> search(@RequestParam String q) {
        return ResponseEntity.ok(tradeService.search(q));
    }
}
