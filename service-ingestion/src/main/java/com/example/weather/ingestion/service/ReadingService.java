package com.example.weather.ingestion.service;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.weather.ingestion.dto.ReadingRequest;
import com.example.weather.ingestion.repository.ReadingRepository;
import com.example.weather.common.InvalidRequestException;

@Service
public
class ReadingService {
    private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);
    private final ReadingRepository repository;
    private final Clock clock;

    ReadingService(ReadingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public
    void save(ReadingRequest r) {
        if (r.timestamp().isAfter(clock.instant().plus(MAX_FUTURE_SKEW))) {
            throw new InvalidRequestException("timestamp is too far in the future");
        }
        repository.insertAll(r.sensorId(), r.timestamp(), r.metrics());
    }
}
