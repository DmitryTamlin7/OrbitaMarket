package org.example.entity;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Базовый интерфейс полиморфной полезной нагрузки (Payload) заказов OrbitaMarket.
 * Настраивает библиотеку Jackson на динамическое разрешение конкретных Java-структур
 * данных ДЗЗ на основе строкового поля-дискриминатора "product_type".
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "product_type",
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
