package org.example.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;


/**
 * Спецификация параметров для архивных данных ДЗЗ.
 * Мапится из формата snake_case внешнего API и игнорирует избыточные метаданные.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchivePayload(
        String aoi,
        String captureDate,
        String sensorType) implements OrderPayload {
    @Override
    public String getAoi() {
        return aoi;
    }
}
