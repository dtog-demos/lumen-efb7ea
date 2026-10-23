package com.example.weather.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StatsApiIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM readings");

        Instant now = Instant.now();
        insertReading("s1", "temperature", 10, now.minus(Duration.ofHours(3)));
        insertReading("s1", "temperature", 20, now.minus(Duration.ofHours(2)));
        insertReading("s1", "temperature", 30, now.minus(Duration.ofHours(1)));
        insertReading("s2", "temperature", 40, now.minus(Duration.ofHours(1)));
        insertReading("s1", "humidity", 50, now.minus(Duration.ofHours(1)));
        insertReading("s1", "temperature", 1000, now.minus(Duration.ofDays(3)));
    }

    private void insertReading(String sensor, String metric, double value, Instant when) {
        jdbcTemplate.update(
                "INSERT INTO readings (sensor_id, metric, value, recorded_at) VALUES (?, ?, ?, ?)",
                sensor, metric, value, OffsetDateTime.ofInstant(when, ZoneOffset.UTC));
    }

    // Each statistic is correct for s1 temperature.
    @ParameterizedTest
    @CsvSource({
            "min, 10",
            "max, 30",
            "sum, 60",
            "avg, 20"
    })
    void statisticIsCorrectForS1Temperature(String stat, double expected) throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1")
            .param("metrics", "temperature").param("stat", stat))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].value").value(expected))
            .andExpect(jsonPath("$.results[0].count").value(3))
            .andExpect(jsonPath("$.stat").value(stat));
    }

    // Listing two sensors returns one row for each.
    @Test 
    void severalSensors() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1,s2")
            .param("metrics", "temperature"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].sensorId").value("s1"))
            .andExpect(jsonPath("$.results[0].value").value(20))
            .andExpect(jsonPath("$.results[0].count").value(3))
            .andExpect(jsonPath("$.results[1].sensorId").value("s2"))
            .andExpect(jsonPath("$.results[1].value").value(40))
            .andExpect(jsonPath("$.results[1].count").value(1));
    }

    // Leaving out sensorIds returns every sensor.
    @Test
    void allSensors() throws Exception {
        mockMvc.perform(get("/api/v1/stats")
            .param("metrics", "temperature"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].sensorId").value("s1"))
            .andExpect(jsonPath("$.results[0].value").value(20))
            .andExpect(jsonPath("$.results[0].count").value(3))
            .andExpect(jsonPath("$.results[1].sensorId").value("s2"))
            .andExpect(jsonPath("$.results[1].value").value(40))
            .andExpect(jsonPath("$.results[1].count").value(1));
    }

    // Listing one sensor returns only that sensor.
    // The two tests above both return s1 and s2, so they would still pass if the sensor filter
    // were ignored. Only this test fails when the filter stops removing the other sensor.
    @Test
    void oneSensorOnly() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s2")
            .param("metrics", "temperature"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.results[0].sensorId").value("s2"))
            .andExpect(jsonPath("$.results[0].value").value(40))
            .andExpect(jsonPath("$.results[0].count").value(1));
    }

    // Several metrics return one row per metric, ordered by metric name.
    @Test
    void severalMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1")
            .param("metrics","humidity","temperature"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].sensorId").value("s1"))
            .andExpect(jsonPath("$.results[0].metric").value("humidity"))
            .andExpect(jsonPath("$.results[0].value").value(50))
            .andExpect(jsonPath("$.results[0].count").value(1))
            .andExpect(jsonPath("$.results[1].sensorId").value("s1"))
            .andExpect(jsonPath("$.results[1].metric").value("temperature"))
            .andExpect(jsonPath("$.results[1].value").value(20))
            .andExpect(jsonPath("$.results[1].count").value(3));
    }

    // With no range, readings older than 24 hours are left out.
    @Test
    void defaultRangeIsLast24Hours() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1")
            .param("metrics", "temperature").param("stat", "max"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].sensorId").value("s1"))
            .andExpect(jsonPath("$.results[0].value").value(30))
            .andExpect(jsonPath("$.results[0].count").value(3));
    }

    // An explicit from includes older readings.
    @Test
    void customRangeFromAndToNow() throws Exception {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(4));
    
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1")
            .param("metrics", "temperature").param("stat", "max")
            .param("from", from.toString()).param("to", now.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].sensorId").value("s1"))
            .andExpect(jsonPath("$.results[0].value").value(1000))
            .andExpect(jsonPath("$.results[0].count").value(4));
    }

    // A metric with no readings returns 200 and an empty list.
    @Test
    void noExistingMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1")
            .param("metrics", "pressure"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.length()").value(0));
    }

    // An unknown stat returns 400 with the error message as JSON.
    @Test
    void errorFromControllerIsReturnedAsJson() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1")
            .param("metrics", "temperature").param("stat", "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("stat must be one of: min, max, sum, avg"));
    }

    // A range under 24 hours returns 400.
    @Test
    void rangeOfOneHourIsRejectedByController() throws Exception {
        Instant from = Instant.now().minus(Duration.ofHours(1));

        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1")
            .param("metrics", "temperature").param("from", from.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Range must be between 24 hours and 31 days"));
    }

    // A request without metrics returns 400.
    @Test
    void missingMetricsIsRejectedByController() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Missing parameter 'metrics'"));
    }

    // An empty metrics value returns 400, not a 500 from invalid SQL (IN ()).
    @Test
    void emptyMetricsIsRejectedWith400() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("metrics", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("metrics must contain at least one metric name"));
    }

    // Only separators and blanks in metrics is the same as no metrics.
    @Test
    void blankMetricsAreRejectedWith400() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("metrics", " , "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("metrics must contain at least one metric name"));
    }

    // Blank entries next to a real metric are ignored.
    @Test
    void blankMetricEntriesAreIgnored() throws Exception {
        mockMvc.perform(get("/api/v1/stats").param("sensorIds", "s1")
            .param("metrics", "temperature,,"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.results[0].metric").value("temperature"));
    }

    // An unknown path is a client error (404), not an unexpected server error.
    @Test
    void unknownPathReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/nope"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    // A method the endpoint does not support returns 405.
    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(post("/api/v1/stats"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405));
    }
}
