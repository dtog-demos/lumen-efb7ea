package com.example.weather.ingestion.dto;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReadingRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String sensorId,
    @NotNull Instant timestamp,
    @NotEmpty @Size(max = 50)
    Map<@Pattern(regexp = "^[a-z][a-z0-9_]{0,63}$") String, @NotNull Double> metrics) {}

