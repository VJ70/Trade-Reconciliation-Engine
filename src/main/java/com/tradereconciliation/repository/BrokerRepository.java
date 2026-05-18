package com.tradereconciliation.repository;

import com.tradereconciliation.model.Broker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BrokerRepository extends JpaRepository<Broker, Long> {
    Optional<Broker> findByCode(String code);
}
