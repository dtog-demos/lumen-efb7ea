package com.example.weather.ingestion.controller;

import com.example.weather.ingestion.dto.ReadingRequest;
import com.example.weather.ingestion.dto.ReadingSaveResponse;
import com.example.weather.ingestion.service.ReadingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/readings")
class ReadingController {
    private final ReadingService service;
    ReadingController(ReadingService service) { this.service = service; }

    @PostMapping
    ResponseEntity<ReadingSaveResponse> create(@Valid @RequestBody ReadingRequest request) {
        ReadingSaveResponse response = service.save(request);
        HttpStatus status = response.inserted().isEmpty() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}