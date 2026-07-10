package org.example.entity;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Спецификация конфигурации для долгосрочного мониторинга территорий.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MonitoringPayload(
        String aoi,
        CadenceType cadenceType,
        Integer durationDays) implements  OrderPayload {
    @Override
    public String getAoi() {
        return aoi;
    }
}
