package org.example.entity;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "productType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ArchivePayload.class, name = "ARCHIVE"),
        @JsonSubTypes.Type(value = TaskingPayload.class, name = "TASKING"),
        @JsonSubTypes.Type(value = MonitoringPayload.class, name = "MONITORING")
})
public interface OrderPayload {
    String getAoi();
}
