package com.ceiba.prueba.service;

import java.time.LocalDate;
import java.util.List;

import com.ceiba.prueba.dto.response.TimeSlotResponse;

public interface AvailabilityService {

	/**
	 * Free 30-minute slots of a doctor between two dates, both inclusive (RF-04). Slots taken by scheduled
	 * appointments and slots that already started are excluded.
	 *
	 * @throws com.ceiba.prueba.exception.ResourceNotFoundException if the doctor does not exist
	 * @throws com.ceiba.prueba.exception.BusinessRuleException if {@code startDate} is after {@code endDate}
	 */
	List<TimeSlotResponse> getAvailableSlots(Long doctorId, LocalDate startDate, LocalDate endDate);

}
