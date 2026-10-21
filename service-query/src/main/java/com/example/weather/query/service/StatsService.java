package com.example.weather.query.service;

import com.example.weather.common.InvalidRequestException;
import com.example.weather.query.model.Stat;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class StatsService {
	private static final Duration MIN_RANGE = Duration.ofHours(24);
	private static final Duration MAX_RANGE = Duration.ofDays(31);

	private final Clock clock;

	StatsService(Clock clock) {
		this.clock = clock;
	}

	public Request resolveRequest(String stat, Instant from, Instant to) {
		Stat parsedStat = Stat.parse(stat);

		return new Request(parsedStat, resolveRange(from, to));
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
