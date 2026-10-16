package com.ceiba.prueba.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.ceiba.prueba.domain.model.Appointment;
import com.ceiba.prueba.domain.policy.WorkingHoursPolicy;
import com.ceiba.prueba.dto.response.AppointmentResponse;
import com.ceiba.prueba.dto.response.TimeSlotResponse;

/**
 * There is no {@code toEntity}: an appointment is built by the service from the loaded patient and doctor.
 */
@Component
public class AppointmentMapper {

	public AppointmentResponse toResponse(Appointment appointment) {
		return new AppointmentResponse(appointment.getId(), appointment.getPatient().getId(),
				appointment.getDoctor().getId(), appointment.getDateTime(), appointment.getStatus(),
				appointment.getCancelledAt());
	}

	public TimeSlotResponse toTimeSlot(LocalDateTime start) {
		return new TimeSlotResponse(start, start.plus(WorkingHoursPolicy.SLOT_DURATION));
	}

}
