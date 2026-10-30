package com.example.weather.common;

import java.util.List;

public class ConflictException extends RuntimeException {
    private final List<String> details;

    public ConflictException(String message, List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }

    public List<String> details() {
        return details;
    }
}
