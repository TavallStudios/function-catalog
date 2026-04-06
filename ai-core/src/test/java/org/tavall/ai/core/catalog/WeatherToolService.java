package org.tavall.ai.core.catalog;

import org.tavall.ai.core.annotation.AIParam;
import org.tavall.ai.core.annotation.AIFunction;

import java.util.List;

public final class WeatherToolService {
    @AIFunction(description = "Build a weather summary")
    public WeatherSummary summarize(
            @AIParam(name = "city") String city,
            @AIParam(name = "unit") WeatherUnit unit,
            @AIParam(name = "days") List<Integer> days,
            @AIParam(name = "context") WeatherContext context
    ) {
        int requestedDays = days == null ? 0 : days.size();
        String countryCode = context.location() == null ? "NA" : context.location().countryCode();
        String notes = context.tags() == null ? "" : String.join("|", context.tags());
        return new WeatherSummary(city + ":" + unit.name(), requestedDays, countryCode, notes);
    }
}