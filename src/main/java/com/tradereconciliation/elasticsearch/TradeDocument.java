package com.tradereconciliation.elasticsearch;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * ES document mirrors key trade fields.
 * Stored in ES index "trades" alongside PostgreSQL for full-text search.
 * Why ES in addition to Postgres?
 *   - Full-text search on symbol/ref across 100k+ records at sub-100ms
 *   - TRC uses ES internally for time-series trade data (mentioned in interviews)
 */
@Document(indexName = "trades")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TradeDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String symbol;

    @Field(type = FieldType.Keyword)
    private String side;

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Double)
    private Double quantity;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Text)
    private String tradeRef;

    @Field(type = FieldType.Date)
    private String timestamp;

    @Field(type = FieldType.Keyword)
    private String brokerCode;
}
