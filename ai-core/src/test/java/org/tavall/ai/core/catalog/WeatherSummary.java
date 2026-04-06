package org.tavall.ai.core.catalog;

public record WeatherSummary(String summary, int daysRequested, String countryCode, String notes) {
}