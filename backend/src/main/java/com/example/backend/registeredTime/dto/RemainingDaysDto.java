package com.example.backend.registeredTime.dto;

import java.time.LocalDateTime;

public record RemainingDaysDto(LocalDateTime endDate, int remainingDays) {
}
