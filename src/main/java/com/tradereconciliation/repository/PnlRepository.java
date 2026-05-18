package com.tradereconciliation.repository;

import com.tradereconciliation.model.PnlSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PnlRepository extends JpaRepository<PnlSummary, Long> {
    List<PnlSummary> findByDate(LocalDate date);
    Optional<PnlSummary> findBySymbolAndDate(String symbol, LocalDate date);
}
