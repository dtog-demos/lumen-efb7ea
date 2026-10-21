package com.example.weather.query.controller;
import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.weather.query.model.StatsResponse;
import com.example.weather.query.service.StatsService;


@RestController
@RequestMapping("/api/v1/stats")
class StatsController {
    private final StatsService service;
    StatsController(StatsService service) { this.service = service; }

    @GetMapping
    StatsResponse getStats(
            @RequestParam(defaultValue = "avg") String stat,
            @RequestParam(required = true) List<String> metrics,
            @RequestParam(required = false) List<String> sensorIds,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return service.getStats(stat, metrics, sensorIds, from, to);
    }
}