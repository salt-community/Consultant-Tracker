package com.example.backend.consultant.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultantTimeDto(UUID itemId,
                                UUID consultantId,
                                LocalDateTime startDate,
                                LocalDateTime endDate,
                                String dayType,
                                int totalDays) {
}
