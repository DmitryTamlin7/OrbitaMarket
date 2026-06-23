package org.example.entity;

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
