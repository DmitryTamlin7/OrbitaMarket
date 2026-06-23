package org.example.entity;

public record ArchivePayload(
        String aoi,
        String captureDate,
        String sensorType) implements OrderPayload {
    @Override
    public String getAoi() {
        return aoi;
    }
}
