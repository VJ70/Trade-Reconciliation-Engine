package com.tradereconciliation.service;

import com.tradereconciliation.model.PnlSummary;
import com.tradereconciliation.model.Trade;
import com.tradereconciliation.repository.PnlRepository;
import com.tradereconciliation.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PnlService {

    private final PnlRepository pnlRepository;
    private final TradeRepository tradeRepository;

    public List<PnlSummary> getPnlForDate(LocalDate date) {
        return pnlRepository.findByDate(date);
    }

    @Transactional
    public List<PnlSummary> calculatePnl(LocalDate date) {
        Instant dayStart = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Trade> matchedTrades = tradeRepository.findMatchedInternalTradesForDate(dayStart);
        log.info("Calculating P&L for {} from {} matched trades", date, matchedTrades.size());

        // Group by symbol, compute realized P&L = sum(price * quantity) for SELLs - BUYs
        Map<String, List<Trade>> bySymbol = matchedTrades.stream()
                .collect(Collectors.groupingBy(Trade::getSymbol));

        List<PnlSummary> results = bySymbol.entrySet().stream().map(entry -> {
            String symbol = entry.getKey();
            List<Trade> trades = entry.getValue();

            BigDecimal pnl = trades.stream()
                    .map(t -> {
                        BigDecimal value = t.getPrice().multiply(t.getQuantity());
                        return t.getSide() == Trade.Side.SELL ? value : value.negate();
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PnlSummary summary = pnlRepository.findBySymbolAndDate(symbol, date)
                    .orElse(PnlSummary.builder().symbol(symbol).date(date).build());
            summary.setRealizedPnl(pnl);
            summary.setTradeCount(trades.size());
            return pnlRepository.save(summary);
        }).collect(Collectors.toList());

        log.info("P&L calculated for {} symbols on {}", results.size(), date);
        return results;
    }
}
