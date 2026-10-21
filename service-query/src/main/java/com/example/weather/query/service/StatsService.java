package com.example.weather.query.service;

import com.example.weather.common.InvalidRequestException;
import com.example.weather.query.model.Stat;
import com.example.weather.query.model.StatResult;
import com.example.weather.query.model.StatsResponse;
import com.example.weather.query.repository.StatsRepository;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class StatsService {
	private static final Duration MIN_RANGE = Duration.ofHours(24);
	private static final Duration MAX_RANGE = Duration.ofDays(31);

	private final Clock clock;
    private final StatsRepository statsRepository;

	StatsService(Clock clock, StatsRepository statsRepository) {
		this.clock = clock;
        this.statsRepository = statsRepository;
	}

	public Request resolveRequest(String stat, Instant from, Instant to) {
		Stat parsedStat = Stat.parse(stat);

		return new Request(parsedStat, resolveRange(from, to));
	}

    public StatsResponse getStats(String stat, List<String> metrics, List<String> sensorIds, Instant from, Instant to) {
        Request request = resolveRequest(stat, from, to);
        List<StatResult> results = statsRepository.findStats(request.stat(), metrics, sensorIds, request.range().from(), request.range().to());
        return new StatsResponse(request.stat().name().toLowerCase(Locale.ROOT), request.range().from(), request.range().to(), results);
    }

	Range resolveRange(Instant from, Instant to) {
		Instant rangeEnd = to == null ? clock.instant() : to;
		Instant rangeStart = from == null ? rangeEnd.minus(MIN_RANGE) : from;

		if (!rangeEnd.isAfter(rangeStart)) {
			throw new InvalidRequestException("to must be after from");
		}

		Duration duration = Duration.between(rangeStart, rangeEnd);
		if (duration.compareTo(MIN_RANGE) < 0 || duration.compareTo(MAX_RANGE) > 0) {
			throw new InvalidRequestException("Range must be between 24 hours and 31 days");
		}
		return new Range(rangeStart, rangeEnd);
	}

	public record Range(Instant from, Instant to) {
	}

	public record Request(Stat stat, Range range) {
	}
}
