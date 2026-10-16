package com.ceiba.prueba.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceiba.prueba.domain.model.AppointmentStatus;
import com.ceiba.prueba.domain.policy.WorkingHoursPolicy;
import com.ceiba.prueba.dto.response.TimeSlotResponse;
import com.ceiba.prueba.exception.BusinessRuleException;
import com.ceiba.prueba.exception.ResourceNotFoundException;
import com.ceiba.prueba.mapper.AppointmentMapper;
import com.ceiba.prueba.repository.AppointmentRepository;
import com.ceiba.prueba.repository.DoctorRepository;
import com.ceiba.prueba.service.AvailabilityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

	private final DoctorRepository doctorRepository;

	private final AppointmentRepository appointmentRepository;

	private final WorkingHoursPolicy workingHoursPolicy;

	private final AppointmentMapper appointmentMapper;

	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public List<TimeSlotResponse> getAvailableSlots(Long doctorId, LocalDate startDate, LocalDate endDate) {
		if (!doctorRepository.existsById(doctorId)) {
			throw new ResourceNotFoundException("Doctor not found.");
		}
		if (startDate.isAfter(endDate)) {
			throw new BusinessRuleException("startDate must be on or before endDate.");
		}

		// One query for the whole range: [startDate 00:00, endDate + 1 day 00:00).
		Set<LocalDateTime> taken = new HashSet<>(appointmentRepository.findDateTimesByDoctorIdAndStatusInRange(
				doctorId, AppointmentStatus.SCHEDULED, startDate.atStartOfDay(),
				endDate.plusDays(1).atStartOfDay()));
		LocalDateTime now = LocalDateTime.now(clock);

		return startDate.datesUntil(endDate.plusDays(1))
				.flatMap(date -> workingHoursPolicy.generateSlots(date).stream())
				.filter(slot -> slot.isAfter(now))
				.filter(slot -> !taken.contains(slot))
				.map(appointmentMapper::toTimeSlot)
				.toList();
	}

}
