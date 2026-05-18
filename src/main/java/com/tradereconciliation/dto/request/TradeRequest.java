package com.tradereconciliation.dto.request;

import com.tradereconciliation.model.Trade;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TradeRequest {

    @NotBlank(message = "Symbol is required")
    @Size(max = 10, message = "Symbol must be 10 chars or less")
    private String symbol;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Side is required")
    private Trade.Side side;

    @NotNull(message = "Source is required")
    private Trade.Source source;

    private Long brokerId;

    private String tradeRef;

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
}
