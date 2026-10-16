package com.ceiba.prueba.service;

import java.util.List;

import com.ceiba.prueba.dto.request.AppointmentFilter;
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

	/**
	 * Cancels a scheduled appointment (RF-05) and registers a penalty if it is a late cancellation (RN-05).
	 *
	 * @throws com.ceiba.prueba.exception.ResourceNotFoundException if the appointment does not exist
	 * @throws com.ceiba.prueba.exception.ConflictException if the appointment is not scheduled
	 */
	AppointmentResponse cancel(Long id);

	/**
	 * Lists appointments matching the optional filters (RF-06), ordered by date-time ascending.
	 *
	 * @throws com.ceiba.prueba.exception.BusinessRuleException if {@code startDate} is after {@code endDate}
	 */
	List<AppointmentResponse> search(AppointmentFilter filter);

}
