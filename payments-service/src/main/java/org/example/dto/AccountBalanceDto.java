package org.example.dto;

public record AccountBalanceDto(
        String userId,
        Long balance,
        String currency
) { }
