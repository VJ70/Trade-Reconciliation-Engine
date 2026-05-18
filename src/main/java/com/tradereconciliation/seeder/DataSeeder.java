package com.tradereconciliation.seeder;

import com.tradereconciliation.model.*;
import com.tradereconciliation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Seeds the DB on startup with realistic trade data:
 *   - 3 brokers (from Flyway migration)
 *   - 2 users: admin / trader
 *   - 500 internal trades + 500 broker trades
 *   - ~15% intentional mismatches → ~75 breaks after reconciliation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final TradeRepository tradeRepository;
    private final BrokerRepository brokerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-data:true}")
    private boolean seedData;

    private static final String[] SYMBOLS = {"AAPL", "TSLA", "GOOGL", "MSFT", "AMZN"};
    private static final Random RANDOM = new Random(42);

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedData) return;
        if (tradeRepository.count() > 0) {
            log.info("Data already seeded, skipping");
            return;
        }

        seedUsers();
        seedTrades();
        log.info("Data seeding complete");
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.ADMIN)
                    .build());
        }
        if (!userRepository.existsByUsername("trader")) {
            userRepository.save(User.builder()
                    .username("trader")
                    .password(passwordEncoder.encode("trader123"))
                    .role(User.Role.TRADER)
                    .build());
        }
        log.info("Seeded users: admin (ADMIN), trader (TRADER)");
    }

    private void seedTrades() {
        List<Broker> brokers = brokerRepository.findAll();
        if (brokers.isEmpty()) {
            log.warn("No brokers found — skipping trade seed");
            return;
        }

        List<Trade> trades = new ArrayList<>();
        Instant baseTime = Instant.now().minus(7, ChronoUnit.DAYS);

        for (int i = 0; i < 500; i++) {
            String symbol   = SYMBOLS[RANDOM.nextInt(SYMBOLS.length)];
            Trade.Side side = RANDOM.nextBoolean() ? Trade.Side.BUY : Trade.Side.SELL;
            BigDecimal qty  = randomDecimal(10, 1000);
            BigDecimal price = randomDecimal(50, 500);
            Instant ts = baseTime.plus(RANDOM.nextInt(7 * 24 * 60), ChronoUnit.MINUTES);
            Broker broker = brokers.get(RANDOM.nextInt(brokers.size()));

            // Internal trade
            trades.add(Trade.builder()
                    .symbol(symbol).quantity(qty).price(price)
                    .side(side).source(Trade.Source.INTERNAL)
                    .broker(broker).status(Trade.Status.PENDING)
                    .tradeRef("INT-" + String.format("%05d", i))
                    .timestamp(ts).build());

            // Broker trade — 85% clean match, 15% with mismatch
            boolean mismatch = RANDOM.nextDouble() < 0.15;
            BigDecimal brokerQty   = mismatch && RANDOM.nextBoolean()
                    ? qty.multiply(BigDecimal.valueOf(1 + (RANDOM.nextDouble() * 0.05)))
                            .setScale(6, RoundingMode.HALF_UP)
                    : qty;
            BigDecimal brokerPrice = mismatch && RANDOM.nextBoolean()
                    ? price.multiply(BigDecimal.valueOf(1 + (RANDOM.nextDouble() * 0.03)))
                            .setScale(6, RoundingMode.HALF_UP)
                    : price;

            // Broker timestamp is close but not identical (±2 minutes)
            Instant brokerTs = ts.plusSeconds(RANDOM.nextInt(120) - 60);

            trades.add(Trade.builder()
                    .symbol(symbol).quantity(brokerQty).price(brokerPrice)
                    .side(side).source(Trade.Source.BROKER)
                    .broker(broker).status(Trade.Status.PENDING)
                    .tradeRef("BRK-" + String.format("%05d", i))
                    .timestamp(brokerTs).build());
        }

        tradeRepository.saveAll(trades);
        log.info("Seeded {} trades (500 internal + 500 broker, ~15% with mismatches)", trades.size());
    }

    private BigDecimal randomDecimal(double min, double max) {
        double val = min + RANDOM.nextDouble() * (max - min);
        return BigDecimal.valueOf(val).setScale(6, RoundingMode.HALF_UP);
    }
}
