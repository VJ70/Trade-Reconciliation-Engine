package com.tradereconciliation.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconciliationResult {
    private int totalProcessed;
    private int matched;
    private int breaksCreated;
    private int unmatched;
    private long durationMs;
    private String runAt;
}
