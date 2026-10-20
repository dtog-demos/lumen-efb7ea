package com.example.weather.ingestion.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class ReadingRepository {
    private static final String INSERT = """
        INSERT INTO readings (sensor_id, metric, value, recorded_at)
        VALUES (:sensorId, :metric, :value, :recordedAt)
        ON CONFLICT (sensor_id, metric, recorded_at) DO NOTHING""";
    private final NamedParameterJdbcTemplate jdbc;

    public ReadingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertAll(String sensorId, Instant ts, Map<String, Double> metrics) {
        OffsetDateTime recordedAt = OffsetDateTime.ofInstant(ts, ZoneOffset.UTC);
        SqlParameterSource[] batch = metrics.entrySet().stream()
            .map(e -> new MapSqlParameterSource()
                .addValue("sensorId", sensorId).addValue("metric", e.getKey())
                .addValue("value", e.getValue()).addValue("recordedAt", recordedAt))
            .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(INSERT, batch);
    }
}
