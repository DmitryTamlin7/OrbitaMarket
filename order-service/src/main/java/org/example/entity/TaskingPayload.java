package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;


/**
 * Конфигурация оперативного целевого заказа на съемку
 */
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

    /**
     * Временное окно в рамках которого спутник должен совершить пролет.
     */
    public record TimeWindow(String from, String to){}
}
