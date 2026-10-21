package com.example.weather.query.model;

import java.time.Instant;
import java.util.List;

public record StatsResponse(String stat, Instant from, Instant to, List<StatResult> results) {
}
