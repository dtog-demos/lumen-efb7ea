package com.example.weather.ingestion.controller;

import com.example.weather.ingestion.dto.ReadingRequest;
import com.example.weather.ingestion.service.ReadingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/readings")
class ReadingController {
    private final ReadingService service;
    ReadingController(ReadingService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void create(@Valid @RequestBody ReadingRequest request) { service.save(request); }
}