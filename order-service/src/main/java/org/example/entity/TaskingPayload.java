package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskingPayload(
        String aoi,
        TimeWindow timeWindow,
        String sensorType) implements OrderPayload {
    @Override
    public String getAoi() {
        return aoi;
    }

    public record TimeWindow(String from, String to){}
}
