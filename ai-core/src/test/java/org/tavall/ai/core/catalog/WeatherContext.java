package org.tavall.ai.core.catalog;

import java.util.List;
import java.util.Map;

public record WeatherContext(LocationInfo location, List<String> tags, Map<String, Integer> thresholdByDay) {
}