package com.ceiba.prueba.service;

import com.ceiba.prueba.dto.request.AppointmentRequest;
import com.ceiba.prueba.dto.response.AppointmentResponse;

public interface AppointmentService {

	/**
	 * Schedules an appointment (RF-03) applying RN-01 to RN-06.
	 *
	 * @throws com.ceiba.prueba.exception.ResourceNotFoundException if the patient or doctor does not exist
	 * @throws com.ceiba.prueba.exception.BusinessRuleException if the date-time or birth date is not valid
	 * @throws com.ceiba.prueba.exception.ConflictException if the patient is blocked or the slot is taken
	 */
	AppointmentResponse schedule(AppointmentRequest request);

}
