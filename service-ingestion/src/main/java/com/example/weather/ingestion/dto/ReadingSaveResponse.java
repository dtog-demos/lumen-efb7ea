package com.example.weather.ingestion.dto;

import java.util.List;

public record ReadingSaveResponse(List<String> inserted, List<String> skipped) {
    public ReadingSaveResponse {
        inserted = List.copyOf(inserted);
        skipped = List.copyOf(skipped);
    }
}
