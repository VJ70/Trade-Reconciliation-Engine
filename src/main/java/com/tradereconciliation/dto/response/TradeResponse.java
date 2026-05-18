package com.tradereconciliation.dto.response;

import com.tradereconciliation.model.Trade;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TradeResponse {
    private Long id;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private Trade.Side side;
    private Trade.Source source;
    private Trade.Status status;
    private String tradeRef;
    private String brokerCode;
    private Instant timestamp;
    private Instant createdAt;

    public static TradeResponse from(Trade t) {
        return TradeResponse.builder()
                .id(t.getId())
                .symbol(t.getSymbol())
                .quantity(t.getQuantity())
                .price(t.getPrice())
                .side(t.getSide())
                .source(t.getSource())
                .status(t.getStatus())
                .tradeRef(t.getTradeRef())
                .brokerCode(t.getBroker() != null ? t.getBroker().getCode() : null)
                .timestamp(t.getTimestamp())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
