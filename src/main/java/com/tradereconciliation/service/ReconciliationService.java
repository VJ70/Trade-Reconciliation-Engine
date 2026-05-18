package com.tradereconciliation.service;

import com.tradereconciliation.dto.response.ReconciliationResult;
import com.tradereconciliation.model.ReconciliationBreak;
import com.tradereconciliation.model.Trade;
import com.tradereconciliation.repository.BreakRepository;
import com.tradereconciliation.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core reconciliation algorithm.
 *
 * Algorithm overview:
 *   For each PENDING INTERNAL trade:
 *     1. Find PENDING BROKER trades with same symbol + side within ±5-minute window
 *     2. If match found:
 *        a. Check quantity delta — if > 0.01%, create QUANTITY_MISMATCH break (HIGH if >1%)
 *        b. Check price delta   — if > 0.01%, create PRICE_MISMATCH break   (HIGH if >1%)
 *        c. If no mismatches    → mark both MATCHED
 *     3. If no broker trade found → create UNMATCHED break for the internal trade
 *
 * Complexity: O(n) internal trades × O(k) broker candidates per trade ≈ O(n) with indexed queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private static final Duration MATCH_WINDOW       = Duration.ofMinutes(5);
    private static final double   MISMATCH_THRESHOLD = 0.0001;  // 0.01%
    private static final double   HIGH_SEVERITY_PCT  = 0.01;    // 1%

    private final TradeRepository tradeRepository;
    private final BreakRepository breakRepository;

    // Last run stats — stored in memory (in production, persist to DB)
    private ReconciliationResult lastRunResult;

    @Transactional
    public ReconciliationResult reconcile() {
        long startMs = System.currentTimeMillis();
        log.info("Reconciliation run started");

        List<Trade> pendingInternal = tradeRepository.findPendingInternalTrades();
        log.info("Found {} pending internal trades", pendingInternal.size());

        int matched = 0, breaksCreated = 0, unmatched = 0;
        List<Long> matchedIds = new ArrayList<>();
        List<ReconciliationBreak> breaks = new ArrayList<>();

        for (Trade internal : pendingInternal) {
            Instant from  = internal.getTimestamp().minus(MATCH_WINDOW);
            Instant to    = internal.getTimestamp().plus(MATCH_WINDOW);

            List<Trade> candidates = tradeRepository.findBrokerCandidates(
                    internal.getSymbol(), internal.getSide(), from, to, internal.getTimestamp());

            if (candidates.isEmpty()) {
                // No broker record found — unmatched break
                breaks.add(buildBreak(internal, null,
                        ReconciliationBreak.BreakType.UNMATCHED,
                        ReconciliationBreak.Severity.HIGH,
                        internal.getQuantity(), BigDecimal.ZERO));
                internal.setStatus(Trade.Status.BREAK);
                unmatched++;
                continue;
            }

            // Take the closest timestamp match
            Trade broker = candidates.get(0);
            boolean hasMismatch = false;

            // Check quantity mismatch
            double qtyDelta = deltaPercent(internal.getQuantity(), broker.getQuantity());
            if (qtyDelta > MISMATCH_THRESHOLD) {
                ReconciliationBreak.Severity sev = qtyDelta > HIGH_SEVERITY_PCT
                        ? ReconciliationBreak.Severity.HIGH : ReconciliationBreak.Severity.LOW;
                breaks.add(buildBreak(internal, broker,
                        ReconciliationBreak.BreakType.QUANTITY_MISMATCH, sev,
                        internal.getQuantity(), broker.getQuantity()));
                hasMismatch = true;
            }

            // Check price mismatch
            double priceDelta = deltaPercent(internal.getPrice(), broker.getPrice());
            if (priceDelta > MISMATCH_THRESHOLD) {
                ReconciliationBreak.Severity sev = priceDelta > HIGH_SEVERITY_PCT
                        ? ReconciliationBreak.Severity.HIGH : ReconciliationBreak.Severity.LOW;
                breaks.add(buildBreak(internal, broker,
                        ReconciliationBreak.BreakType.PRICE_MISMATCH, sev,
                        internal.getPrice(), broker.getPrice()));
                hasMismatch = true;
            }

            if (hasMismatch) {
                internal.setStatus(Trade.Status.BREAK);
                broker.setStatus(Trade.Status.BREAK);
                breaksCreated++;
            } else {
                // Clean match
                matchedIds.add(internal.getId());
                matchedIds.add(broker.getId());
                matched++;
            }
        }

        // Bulk status updates (one query instead of N)
        if (!matchedIds.isEmpty()) {
            tradeRepository.updateStatusForIds(matchedIds, Trade.Status.MATCHED);
        }
        if (!breaks.isEmpty()) {
            breakRepository.saveAll(breaks);
        }

        long durationMs = System.currentTimeMillis() - startMs;
        log.info("Reconciliation complete: matched={} breaks={} unmatched={} duration={}ms",
                matched, breaksCreated, unmatched, durationMs);

        lastRunResult = ReconciliationResult.builder()
                .totalProcessed(pendingInternal.size())
                .matched(matched)
                .breaksCreated(breaksCreated)
                .unmatched(unmatched)
                .durationMs(durationMs)
                .runAt(Instant.now().toString())
                .build();

        return lastRunResult;
    }

    public ReconciliationResult getLastRunResult() {
        return lastRunResult;
    }

    private double deltaPercent(BigDecimal expected, BigDecimal actual) {
        if (expected.compareTo(BigDecimal.ZERO) == 0) return actual.compareTo(BigDecimal.ZERO) == 0 ? 0 : 1;
        return expected.subtract(actual).abs()
                .divide(expected, 8, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private ReconciliationBreak buildBreak(Trade internal, Trade broker,
                                           ReconciliationBreak.BreakType type,
                                           ReconciliationBreak.Severity severity,
                                           BigDecimal expected, BigDecimal actual) {
        return ReconciliationBreak.builder()
                .internalTrade(internal)
                .brokerTrade(broker)
                .breakType(type)
                .severity(severity)
                .expectedValue(expected)
                .actualValue(actual)
                .build();
    }
}
