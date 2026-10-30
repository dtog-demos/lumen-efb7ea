package com.example.weather.ingestion.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.example.weather.common.ConflictException;
import com.example.weather.ingestion.dto.ReadingSaveResponse;

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

    public ReadingSaveResponse insertAll(String sensorId, Instant ts, Map<String, Double> metrics) {
        OffsetDateTime recordedAt = OffsetDateTime.ofInstant(ts, ZoneOffset.UTC);
        List<Entry<String, Double>> entries = metrics.entrySet().stream()
            .sorted(Entry.comparingByKey())
            .toList();
        SqlParameterSource[] batch = entries.stream()
            .map(e -> new MapSqlParameterSource()
                .addValue("sensorId", sensorId).addValue("metric", e.getKey())
                .addValue("value", e.getValue()).addValue("recordedAt", recordedAt))
            .toArray(SqlParameterSource[]::new);
        int[] updateCounts = jdbc.batchUpdate(INSERT, batch);
        if (updateCounts.length != entries.size()) {
            throw new IllegalStateException("Unexpected insert result count");
        }

        List<String> inserted = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (int i = 0; i < updateCounts.length; i++) {
            if (updateCounts[i] == 1) {
                inserted.add(entries.get(i).getKey());
            } else if (updateCounts[i] == 0) {
                skipped.add(entries.get(i).getKey());
            } else {
                throw new IllegalStateException("Unexpected insert update count: " + updateCounts[i]);
            }
        }
        if (!skipped.isEmpty()) {
            rejectConflictingValues(sensorId, recordedAt, entries, skipped);
        }
        return new ReadingSaveResponse(inserted, skipped);
    }

    private void rejectConflictingValues(
            String sensorId,
            OffsetDateTime recordedAt,
            List<Entry<String, Double>> entries,
            List<String> skipped) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("sensorId", sensorId)
            .addValue("recordedAt", recordedAt);
        Map<String, Double> valuesByMetric = entries.stream()
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        List<String> values = new ArrayList<>();
        for (int i = 0; i < skipped.size(); i++) {
            String metric = skipped.get(i);
            Double value = valuesByMetric.get(metric);
            if (value == null) {
                throw new IllegalStateException("Missing metric insert result");
            }
            parameters.addValue("metric" + i, metric);
            parameters.addValue("value" + i, value);
            values.add("(:metric" + i + ", :value" + i + ")");
        }
        String sql = """
            SELECT incoming.metric
            FROM (VALUES %s) AS incoming(metric, value)
            JOIN readings existing
              ON existing.sensor_id = :sensorId
             AND existing.recorded_at = :recordedAt
             AND existing.metric = incoming.metric
            WHERE existing.value IS DISTINCT FROM incoming.value
            ORDER BY incoming.metric
            """.formatted(String.join(", ", values));
        List<String> conflicts = jdbc.query(sql, parameters,
            (resultSet, rowNumber) -> resultSet.getString("metric"));
        if (!conflicts.isEmpty()) {
            throw new ConflictException(
                "Reading values conflict with existing readings", conflicts);
        }
    }
}
