package com.example.weather.query.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.weather.common.InvalidRequestException;
import com.example.weather.query.model.Stat;
import com.example.weather.query.model.StatResult;
import com.example.weather.query.repository.StatsRepository;

@ExtendWith(MockitoExtension.class)
public class StatsServiceTest {

    @Mock
    private StatsRepository statsRepository;

    private final Clock fixedClock = Clock.fixed(
        Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(fixedClock, statsRepository);
    }

    // A missing range defaults to the last 24 hours.
    @Test
    void noRangeDefaultsToLast24Hours() {
        Instant now = fixedClock.instant();
        assertRange(null, null, now.minus(Duration.ofHours(24)), now);
    }

    // A from-only range ends at the current time.
    @Test
    void fromOnlyRangeEndsNow() {
        Instant now = fixedClock.instant();
        assertRange(now.minus(Duration.ofDays(3)), null, now.minus(Duration.ofDays(3)), now);
    }

    // A to-only range starts 24 hours before to.
    @Test
    void toOnlyRangeStarts24HoursEarlier() {
        Instant now = fixedClock.instant();
        Instant requestedTo = now.minus(Duration.ofDays(2));
        assertRange(null, requestedTo, requestedTo.minus(Duration.ofHours(24)), requestedTo);
    }

    // A to endpoint equal to or before from is rejected.
    @Test
    void toEqualToOrBeforeFromIsRejected() {
        Instant now = fixedClock.instant();
        InvalidRequestException equalRange = assertThrows(InvalidRequestException.class,
            () -> statsService.resolveRange(now, now));
        assertEquals("to must be after from", equalRange.getMessage());
        assertThrows(InvalidRequestException.class,
            () -> statsService.resolveRange(now, now.minusSeconds(1)));
    }

    // Ranges shorter than 24 hours are rejected, while exactly 24 hours is accepted.
    @Test
    void rangeMustBeAtLeast24Hours() {
        Instant now = fixedClock.instant();
        assertThrows(InvalidRequestException.class,
            () -> statsService.resolveRange(now.minus(Duration.ofHours(23).plusMinutes(59)), now));
        assertRange(now.minus(Duration.ofHours(24)), now,
            now.minus(Duration.ofHours(24)), now);
    }

    // Ranges longer than 31 days are rejected, while exactly 31 days is accepted.
    @Test
    void rangeMustBeAtMost31Days() {
        Instant now = fixedClock.instant();
        assertThrows(InvalidRequestException.class,
            () -> statsService.resolveRange(now.minus(Duration.ofDays(31).plusSeconds(1)), now));
        assertRange(now.minus(Duration.ofDays(31)), now,
            now.minus(Duration.ofDays(31)), now);
    }

    // A from endpoint without to is rejected when it is only one hour ago.
    @Test
    void fromOneHourAgoWithoutToIsRejected() {
        Instant now = fixedClock.instant();
        assertThrows(InvalidRequestException.class,
            () -> statsService.resolveRange(now.minus(Duration.ofHours(1)), null));
    }

    // An unknown stat is rejected before the repository is called.
    @Test
    void unknownStatIsRejectedWithoutCallingRepository() {
        assertThrows(InvalidRequestException.class,
            () -> statsService.getStats("unknown", null, null, null, null));
        verifyNoInteractions(statsRepository);
    }

    // getStats returns the lower-case stat, the resolved range and the repository results.
    @Test
    void getStatsReturnsLowerCaseStatAndResolvedRange() {
        Instant now = fixedClock.instant();
        Instant requestedFrom = now.minus(Duration.ofDays(2));
        List<String> metrics = List.of("temperature");
        List<StatResult> expectedResults = List.of(
            new StatResult("s1", "temperature", 21.5, 2));
        when(statsRepository.findStats(Stat.AVG, metrics, null, requestedFrom, now))
            .thenReturn(expectedResults);

        var result = statsService.getStats("AVG", metrics, null, requestedFrom, null);

        assertEquals("avg", result.stat());
        assertEquals(requestedFrom, result.from());
        assertEquals(now, result.to());
        assertEquals(expectedResults, result.results());
    }

    private void assertRange(Instant from, Instant to, Instant expectedFrom, Instant expectedTo) {
        StatsService.Range range = statsService.resolveRange(from, to);
        assertEquals(expectedFrom, range.from());
        assertEquals(expectedTo, range.to());
    }

    // Empty or blank metrics are rejected before the repository is called.
    @Test
    void emptyOrBlankMetricsAreRejectedWithoutCallingRepository() {
        for (List<String> metrics : List.of(List.<String>of(), List.of(""), List.of(" ", ""))) {
            InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> statsService.getStats("avg", metrics, null, null, null));
            assertEquals("metrics must contain at least one metric name", ex.getMessage());
        }
        verifyNoInteractions(statsRepository);
    }

    // Blank entries are dropped and the rest are trimmed before the query.
    @Test
    void blankMetricEntriesAreDroppedAndTrimmed() {
        Instant now = fixedClock.instant();
        when(statsRepository.findStats(Stat.AVG, List.of("temperature", "humidity"), null,
            now.minus(Duration.ofHours(24)), now)).thenReturn(List.of());

        var result = statsService.getStats("avg", List.of(" temperature", "", "humidity "), null, null, null);

        assertEquals(List.of(), result.results());
    }
}
