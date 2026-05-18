package com.tradereconciliation.dto.response;

import com.tradereconciliation.model.ReconciliationBreak;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BreakResponse {
    private Long id;
    private Long internalTradeId;
    private Long brokerTradeId;
    private String symbol;
    private ReconciliationBreak.BreakType breakType;
    private ReconciliationBreak.Severity severity;
    private BigDecimal expectedValue;
    private BigDecimal actualValue;
    private boolean resolved;
    private Instant resolvedAt;
    private Instant createdAt;

    public static BreakResponse from(ReconciliationBreak b) {
        return BreakResponse.builder()
                .id(b.getId())
                .internalTradeId(b.getInternalTrade() != null ? b.getInternalTrade().getId() : null)
                .brokerTradeId(b.getBrokerTrade() != null ? b.getBrokerTrade().getId() : null)
                .symbol(b.getInternalTrade() != null ? b.getInternalTrade().getSymbol() : null)
                .breakType(b.getBreakType())
                .severity(b.getSeverity())
                .expectedValue(b.getExpectedValue())
                .actualValue(b.getActualValue())
                .resolved(b.isResolved())
                .resolvedAt(b.getResolvedAt())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
