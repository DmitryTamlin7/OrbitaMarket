package org.example.entity;

public record MonitoringPayload(
        String aoi,
        CadenceType cadenceType,
        Integer durationDays) implements  OrderPayload {
    @Override
    public String getAoi() {
        return aoi;
    }
}
