package com.tradereconciliation.service;

import com.tradereconciliation.dto.response.ReconciliationResult;
import com.tradereconciliation.model.ReconciliationBreak;
import com.tradereconciliation.model.Trade;
import com.tradereconciliation.repository.BreakRepository;
import com.tradereconciliation.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock TradeRepository tradeRepository;
    @Mock BreakRepository breakRepository;
    @InjectMocks ReconciliationService reconciliationService;

    private Trade internalTrade;

    @BeforeEach
    void setUp() {
        internalTrade = Trade.builder()
                .id(1L).symbol("AAPL")
                .quantity(new BigDecimal("100.000000"))
                .price(new BigDecimal("150.000000"))
                .side(Trade.Side.BUY)
                .source(Trade.Source.INTERNAL)
                .status(Trade.Status.PENDING)
                .timestamp(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Clean match: both trades marked MATCHED, no breaks created")
    void cleanMatch_shouldMarkBothMatched() {
        Trade brokerTrade = Trade.builder()
                .id(2L).symbol("AAPL")
                .quantity(new BigDecimal("100.000000"))
                .price(new BigDecimal("150.000000"))
                .side(Trade.Side.BUY)
                .source(Trade.Source.BROKER)
                .status(Trade.Status.PENDING)
                .timestamp(Instant.now().plusSeconds(30))
                .build();

        when(tradeRepository.findPendingInternalTrades()).thenReturn(List.of(internalTrade));
        when(tradeRepository.findBrokerCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(brokerTrade));

        ReconciliationResult result = reconciliationService.reconcile();

        assertThat(result.getMatched()).isEqualTo(1);
        assertThat(result.getBreaksCreated()).isEqualTo(0);
        verify(tradeRepository).updateStatusForIds(anyList(), eq(Trade.Status.MATCHED));
        verify(breakRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Quantity mismatch > 1%: HIGH severity break created")
    void quantityMismatch_shouldCreateHighSeverityBreak() {
        Trade brokerTrade = Trade.builder()
                .id(2L).symbol("AAPL")
                .quantity(new BigDecimal("105.000000"))   // 5% diff — HIGH severity
                .price(new BigDecimal("150.000000"))
                .side(Trade.Side.BUY)
                .source(Trade.Source.BROKER)
                .status(Trade.Status.PENDING)
                .timestamp(Instant.now())
                .build();

        when(tradeRepository.findPendingInternalTrades()).thenReturn(List.of(internalTrade));
        when(tradeRepository.findBrokerCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(brokerTrade));

        ReconciliationResult result = reconciliationService.reconcile();

        assertThat(result.getBreaksCreated()).isEqualTo(1);
        verify(breakRepository).saveAll(argThat((List<ReconciliationBreak> breaks) ->
                breaks.stream().anyMatch(b ->
                        b.getBreakType() == ReconciliationBreak.BreakType.QUANTITY_MISMATCH &&
                        b.getSeverity() == ReconciliationBreak.Severity.HIGH)
        ));
    }

    @Test
    @DisplayName("No broker trade found: UNMATCHED break created")
    void noBrokerMatch_shouldCreateUnmatchedBreak() {
        when(tradeRepository.findPendingInternalTrades()).thenReturn(List.of(internalTrade));
        when(tradeRepository.findBrokerCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        ReconciliationResult result = reconciliationService.reconcile();

        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getMatched()).isEqualTo(0);
        verify(breakRepository).saveAll(argThat((List<ReconciliationBreak> breaks) ->
                breaks.stream().anyMatch(b ->
                        b.getBreakType() == ReconciliationBreak.BreakType.UNMATCHED)
        ));
    }

    @Test
    @DisplayName("Empty pending queue: reconciliation returns zeros")
    void emptyQueue_shouldReturnZeroCounts() {
        when(tradeRepository.findPendingInternalTrades()).thenReturn(List.of());

        ReconciliationResult result = reconciliationService.reconcile();

        assertThat(result.getTotalProcessed()).isEqualTo(0);
        assertThat(result.getMatched()).isEqualTo(0);
    }
}
