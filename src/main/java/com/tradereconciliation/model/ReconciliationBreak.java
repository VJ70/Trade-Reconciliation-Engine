package com.tradereconciliation.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reconciliation_breaks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconciliationBreak {

    public enum BreakType { QUANTITY_MISMATCH, PRICE_MISMATCH, UNMATCHED }
    public enum Severity  { HIGH, LOW }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_trade_id")
    private Trade internalTrade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_trade_id")
    private Trade brokerTrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "break_type", nullable = false, columnDefinition = "break_type")
    private BreakType breakType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "break_severity")
    private Severity severity;

    @Column(name = "expected_value", precision = 18, scale = 6)
    private BigDecimal expectedValue;

    @Column(name = "actual_value", precision = 18, scale = 6)
    private BigDecimal actualValue;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }
}
