package com.ceiba.prueba.dto.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.ceiba.prueba.domain.model.AppointmentStatus;

/**
 * Optional query parameters for listing appointments (RF-06). Dates are ISO 8601 ({@code yyyy-MM-dd}) and
 * inclusive.
 */
public record AppointmentFilter(

		Long doctorId,

		Long patientId,

		AppointmentStatus status,

		@DateTimeFormat(iso = ISO.DATE)
		LocalDate startDate,

		@DateTimeFormat(iso = ISO.DATE)
		LocalDate endDate) {
}
