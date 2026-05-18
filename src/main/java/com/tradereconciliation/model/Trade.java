package com.tradereconciliation.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trades")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Trade {

    public enum Side   { BUY, SELL }
    public enum Source { INTERNAL, BROKER }
    public enum Status { PENDING, MATCHED, BREAK }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "trade_side")
    private Side side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "trade_source")
    private Source source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "trade_status")
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "trade_ref", length = 50)
    private String tradeRef;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) this.status = Status.PENDING;
    }
}
