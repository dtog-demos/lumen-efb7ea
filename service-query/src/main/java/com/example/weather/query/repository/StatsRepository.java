package com.example.weather.query.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.weather.query.model.Stat;
import com.example.weather.query.model.StatResult;

@Repository
public class StatsRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public StatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StatResult> findStats(Stat stat, List<String> metrics, List<String> sensorIds, Instant from,
            Instant to) {
        StringBuilder sql = new StringBuilder("SELECT sensor_id, metric, ")
                .append(stat.name())
                .append("(value) AS value, COUNT(*) AS count FROM readings ")
                .append("WHERE metric IN (:metrics) ")
                .append("AND recorded_at >= :from AND recorded_at < :to ");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("metrics", metrics)
                .addValue("from", OffsetDateTime.ofInstant(from, ZoneOffset.UTC))
                .addValue("to", OffsetDateTime.ofInstant(to, ZoneOffset.UTC));
        // sensor filtered only when the list is non-empty
        if (sensorIds != null && !sensorIds.isEmpty()) {
            sql.append("AND sensor_id IN (:sensorIds) ");
            params.addValue("sensorIds", sensorIds);
        }

        sql.append("GROUP BY sensor_id, metric ORDER BY sensor_id, metric");

        return jdbc.query(sql.toString(), params, this::mapStatResult);
    }

    // Builds one StatResult per row; column names must match the 
    // SQL aliases (sensor_id, metric, value, count).
    private StatResult mapStatResult(ResultSet rs, int rowNum) throws SQLException {
        String sensorId = rs.getString("sensor_id");
        String metric = rs.getString("metric");
        double value = rs.getDouble("value");
        long count = rs.getLong("count");

        return new StatResult(sensorId, metric, value, count);
    }
}
