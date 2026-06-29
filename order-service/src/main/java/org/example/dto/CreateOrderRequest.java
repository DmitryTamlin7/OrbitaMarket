package org.example.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.example.entity.*;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateOrderRequest(
        ProductType productType,
        Integer price,
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "product_type",
                visible = true
        )
        @JsonSubTypes({
                @JsonSubTypes.Type(value = ArchivePayload.class, name = "ARCHIVE"),
                @JsonSubTypes.Type(value = TaskingPayload.class, name = "TASKING"),
                @JsonSubTypes.Type(value = MonitoringPayload.class, name = "MONITORING")
        })
        OrderPayload payload)
{}
