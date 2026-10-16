package com.ceiba.prueba.dto.response;

import java.time.LocalDateTime;

import com.ceiba.prueba.domain.model.AppointmentStatus;

public record AppointmentResponse(Long id, Long patientId, Long doctorId, LocalDateTime dateTime,
		AppointmentStatus status, LocalDateTime cancelledAt) {
}
