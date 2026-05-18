package com.tradereconciliation.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pnl_summary", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PnlSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "realized_pnl", nullable = false, precision = 18, scale = 6)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "trade_count", nullable = false)
    @Builder.Default
    private Integer tradeCount = 0;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.calculatedAt = Instant.now();
    }
}
