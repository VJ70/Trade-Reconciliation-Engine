package com.tradereconciliation.repository;

import com.tradereconciliation.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long>,
        JpaSpecificationExecutor<Trade> {

    // Fetch all PENDING internal trades for reconciliation
    @Query("SELECT t FROM Trade t WHERE t.source = 'INTERNAL' AND t.status = 'PENDING' ORDER BY t.timestamp")
    List<Trade> findPendingInternalTrades();

    // Find broker trades matching symbol + side within a timestamp window (±5 minutes)
    @Query("""
        SELECT t FROM Trade t
        WHERE t.source = 'BROKER'
          AND t.status = 'PENDING'
          AND t.symbol = :symbol
          AND t.side   = :side
          AND t.timestamp BETWEEN :from AND :to
        ORDER BY ABS(EXTRACT(EPOCH FROM (t.timestamp - :pivot)))
        """)
    List<Trade> findBrokerCandidates(
            @Param("symbol") String symbol,
            @Param("side")   Trade.Side side,
            @Param("from")   Instant from,
            @Param("to")     Instant to,
            @Param("pivot")  Instant pivot
    );

    // Bulk status update after reconciliation
    @Modifying
    @Query("UPDATE Trade t SET t.status = :status WHERE t.id IN :ids")
    void updateStatusForIds(@Param("ids") List<Long> ids, @Param("status") Trade.Status status);

    // Count by status — used in reconciliation run stats
    long countByStatus(Trade.Status status);

    // P&L source query: matched trades for a given date
    @Query("""
        SELECT t FROM Trade t
        WHERE t.status = 'MATCHED'
          AND t.source = 'INTERNAL'
          AND CAST(t.timestamp AS date) = CAST(:date AS date)
        """)
    List<Trade> findMatchedInternalTradesForDate(@Param("date") Instant date);
}
