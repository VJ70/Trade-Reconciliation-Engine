package com.tradereconciliation.repository;

import com.tradereconciliation.model.ReconciliationBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BreakRepository extends JpaRepository<ReconciliationBreak, Long>,
        JpaSpecificationExecutor<ReconciliationBreak> {

    List<ReconciliationBreak> findBySeverity(ReconciliationBreak.Severity severity);

    @Query("SELECT b FROM ReconciliationBreak b WHERE b.resolvedAt IS NULL ORDER BY b.createdAt DESC")
    List<ReconciliationBreak> findAllUnresolved();

    long countByResolvedAtIsNull();
}
