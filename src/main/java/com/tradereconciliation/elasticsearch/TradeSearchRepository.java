package com.tradereconciliation.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TradeSearchRepository extends ElasticsearchRepository<TradeDocument, String> {
    List<TradeDocument> findBySymbol(String symbol);
    List<TradeDocument> findByStatus(String status);
}
