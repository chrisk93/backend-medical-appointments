package com.ceiba.prueba.dto.response;

import java.time.LocalDateTime;

/** An available 30-minute slot (RF-04). */
public record TimeSlotResponse(LocalDateTime start, LocalDateTime end) {
}
