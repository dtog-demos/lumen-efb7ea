package com.example.weather.query.model;

public record StatResult(String sensorId, String metric, double value, long count) {}
