package com.example.weather.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReadingApiIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM readings");
    }

    private int countRows() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM readings", Integer.class);
    }

    private static final String VALID_READING = """
            {
                "sensorId": "s1",
                "timestamp": "2026-10-20T10:00:00Z",
                "metrics": {
                    "temperature": 20.5,
                    "humidity": 60.0
                }
            }
            """;

    // valid reading is stored, one row per metric
    @Test
    void storesOneRowPerMetric() throws Exception {
        String json = VALID_READING;

        mockMvc.perform(post("/api/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        assertEquals(2, countRows());
    }

    // same reading posted twice is stored once
    @Test
    void ignoresDuplicateReading() throws Exception {
        String json = VALID_READING;

        mockMvc.perform(post("/api/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        assertEquals(2, countRows());
    }

    // invalid body returns 400 and writes nothing
    @Test
    void rejectsInvalidBody() throws Exception {
        String json = """
                {
                    "sensorId": "",
                    "timestamp": "2026-10-20T10:00:00Z",
                    "metrics": {}
                }
                """;

        mockMvc.perform(post("/api/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isNotEmpty());

        assertEquals(0, countRows());
    }

   // timestamp an hour ahead returns 400 and writes nothing
    @Test
    void rejectsFutureTimestamp() throws Exception {
        String futureTimestamp = Instant.now().plus(Duration.ofHours(1)).toString();
        String json = """
                {
                    "sensorId": "s1",
                    "timestamp": "%s",
                    "metrics": {
                        "temperature": 20.5
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(json, futureTimestamp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("timestamp is too far in the future"));

        assertEquals(0, countRows());
    }

    // A body that is not JSON is rejected with 415, not a 500.
    @Test
    void wrongContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/v1/readings")
                .contentType(MediaType.TEXT_PLAIN)
                .content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));

        assertEquals(0, countRows());
    }

    // The endpoint only accepts POST; other methods return 405.
    @Test
    void getOnReadingsReturns405() throws Exception {
        mockMvc.perform(get("/api/v1/readings"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
