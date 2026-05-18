package com.tradereconciliation.service;

import com.tradereconciliation.dto.response.BreakResponse;
import com.tradereconciliation.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.tradereconciliation.model.ReconciliationBreak;
import com.tradereconciliation.repository.BreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BreakService {

    private final BreakRepository breakRepository;

    public List<BreakResponse> getAllBreaks() {
        return breakRepository.findAll().stream()
                .map(BreakResponse::from).collect(Collectors.toList());
    }

    public List<BreakResponse> getUnresolvedBreaks() {
        return breakRepository.findAllUnresolved().stream()
                .map(BreakResponse::from).collect(Collectors.toList());
    }

    public BreakResponse getById(Long id) {
        return breakRepository.findById(id)
                .map(BreakResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Break not found: " + id));
    }

    @Transactional
    public BreakResponse resolveBreak(Long id) {
        ReconciliationBreak b = breakRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Break not found: " + id));
        b.setResolvedAt(Instant.now());
        return BreakResponse.from(breakRepository.save(b));
    }

    public long countUnresolved() {
        return breakRepository.countByResolvedAtIsNull();
    }
}
