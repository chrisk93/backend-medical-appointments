package com.ceiba.prueba.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Input for scheduling an appointment (RF-03). {@code dateTime} is ISO 8601 ({@code yyyy-MM-ddTHH:mm:ss}).
 */
public record AppointmentRequest(

		@NotNull
		Long patientId,

		@NotNull
		Long doctorId,

		@NotNull
		LocalDateTime dateTime) {
}
