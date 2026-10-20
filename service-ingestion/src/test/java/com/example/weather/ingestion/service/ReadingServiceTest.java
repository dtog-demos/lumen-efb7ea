package com.example.weather.ingestion.service;

import com.example.weather.common.InvalidRequestException;
import com.example.weather.ingestion.dto.ReadingRequest;
import com.example.weather.ingestion.repository.ReadingRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReadingServiceTest {

    @Mock
    private ReadingRepository readingRepository;

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);

    private ReadingService readingService;

    @BeforeEach
    void setUp() {
        readingService = new ReadingService(readingRepository, fixedClock);
    }

    // 5 min 1 s ahead is over the skew limit: rejected, nothing saved
    @Test
    void rejectsTimestampMoreThanFiveMinutesAhead() {
        Instant tooFarAhead = fixedClock.instant().plus(Duration.ofMinutes(5)).plusSeconds(1);
        ReadingRequest request = new ReadingRequest("s1", tooFarAhead, Map.of("temperature", 20.0));

        assertThrows(InvalidRequestException.class, () -> readingService.save(request));

        verifyNoInteractions(readingRepository);
    }

    // exactly 5 min ahead is on the limit: accepted
    @Test
    void acceptsTimestampExactlyFiveMinutesAhead() {
        Instant exactlyFiveMinutesAhead = fixedClock.instant().plus(Duration.ofMinutes(5));
        ReadingRequest request = new ReadingRequest("s1", exactlyFiveMinutesAhead, Map.of("temperature", 20.0));

        readingService.save(request);

        verify(readingRepository).insertAll(
                "s1", exactlyFiveMinutesAhead, Map.of("temperature", 20.0));
    }

    // valid reading is passed to the repository unchanged
    @Test
    void savesValidReading() {
        Instant validTimestamp = fixedClock.instant();
        ReadingRequest request = new ReadingRequest("s1", validTimestamp, Map.of("temperature", 20.0));

        readingService.save(request);

        verify(readingRepository).insertAll(
                "s1", validTimestamp, Map.of("temperature", 20.0));
    }

    // far-future timestamp is rejected, nothing saved
    @Test
    void rejectsTimestampHoursAhead() {
        Instant hoursAhead = fixedClock.instant().plus(Duration.ofHours(1));
        ReadingRequest request = new ReadingRequest("s1", hoursAhead, Map.of("temperature", 20.0));

        assertThrows(InvalidRequestException.class, () -> readingService.save(request));

        verifyNoInteractions(readingRepository);
    }
}
