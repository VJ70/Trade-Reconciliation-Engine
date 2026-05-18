package com.tradereconciliation.service;

import com.tradereconciliation.dto.request.TradeRequest;
import com.tradereconciliation.dto.response.TradeResponse;
import com.tradereconciliation.elasticsearch.TradeDocument;
import com.tradereconciliation.elasticsearch.TradeSearchRepository;
import com.tradereconciliation.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.tradereconciliation.model.Broker;
import com.tradereconciliation.model.Trade;
import com.tradereconciliation.repository.BrokerRepository;
import com.tradereconciliation.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;
    private final BrokerRepository brokerRepository;
    private final TradeSearchRepository tradeSearchRepository;

    @Transactional
    public TradeResponse ingestTrade(TradeRequest request) {
        Broker broker = null;
        if (request.getBrokerId() != null) {
            broker = brokerRepository.findById(request.getBrokerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Broker not found: " + request.getBrokerId()));
        }

        Trade trade = Trade.builder()
                .symbol(request.getSymbol().toUpperCase())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .side(request.getSide())
                .source(request.getSource())
                .broker(broker)
                .tradeRef(request.getTradeRef())
                .timestamp(request.getTimestamp())
                .status(Trade.Status.PENDING)
                .build();

        trade = tradeRepository.save(trade);
        indexInElasticSearch(trade);
        log.debug("Ingested trade id={} symbol={} source={}", trade.getId(), trade.getSymbol(), trade.getSource());
        return TradeResponse.from(trade);
    }

    public TradeResponse getById(Long id) {
        return tradeRepository.findById(id)
                .map(TradeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found: " + id));
    }

    public Page<TradeResponse> findAll(Pageable pageable) {
        return tradeRepository.findAll(pageable).map(TradeResponse::from);
    }

    // ElasticSearch full-text search
    public List<TradeDocument> search(String query) {
        return tradeSearchRepository.findBySymbol(query.toUpperCase());
    }

    private void indexInElasticSearch(Trade trade) {
        try {
            TradeDocument doc = TradeDocument.builder()
                    .id(trade.getId().toString())
                    .symbol(trade.getSymbol())
                    .side(trade.getSide().name())
                    .source(trade.getSource().name())
                    .status(trade.getStatus().name())
                    .quantity(trade.getQuantity().doubleValue())
                    .price(trade.getPrice().doubleValue())
                    .tradeRef(trade.getTradeRef())
                    .timestamp(trade.getTimestamp().toString())
                    .brokerCode(trade.getBroker() != null ? trade.getBroker().getCode() : null)
                    .build();
            tradeSearchRepository.save(doc);
        } catch (Exception e) {
            // ES failure must not roll back the DB transaction
            log.warn("ElasticSearch indexing failed for trade {}: {}", trade.getId(), e.getMessage());
        }
    }
}
