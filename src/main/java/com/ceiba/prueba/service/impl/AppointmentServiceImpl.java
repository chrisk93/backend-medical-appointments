package com.ceiba.prueba.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceiba.prueba.domain.model.Appointment;
import com.ceiba.prueba.domain.model.AppointmentStatus;
import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.domain.model.Patient;
import com.ceiba.prueba.domain.policy.PenaltyPolicy;
import com.ceiba.prueba.domain.policy.WorkingHoursPolicy;
import com.ceiba.prueba.dto.request.AppointmentRequest;
import com.ceiba.prueba.dto.response.AppointmentResponse;
import com.ceiba.prueba.exception.BusinessRuleException;
import com.ceiba.prueba.exception.ConflictException;
import com.ceiba.prueba.exception.ResourceNotFoundException;
import com.ceiba.prueba.mapper.AppointmentMapper;
import com.ceiba.prueba.repository.AppointmentRepository;
import com.ceiba.prueba.repository.DoctorRepository;
import com.ceiba.prueba.repository.PatientRepository;
import com.ceiba.prueba.repository.PenaltyRepository;
import com.ceiba.prueba.service.AppointmentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

	private final AppointmentRepository appointmentRepository;

	private final PatientRepository patientRepository;

	private final DoctorRepository doctorRepository;

	private final PenaltyRepository penaltyRepository;

	private final WorkingHoursPolicy workingHoursPolicy;

	private final PenaltyPolicy penaltyPolicy;

	private final AppointmentMapper appointmentMapper;

	private final Clock clock;

	/**
	 * Validation order (DESIGN §1.6): existence (404) → date-time and birth date (400) → penalties, doctor slot and
	 * patient slot (409).
	 */
	@Override
	@Transactional
	public AppointmentResponse schedule(AppointmentRequest request) {
		Patient patient = patientRepository.findById(request.patientId())
				.orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
		Doctor doctor = doctorRepository.findById(request.doctorId())
				.orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));

		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime dateTime = request.dateTime();
		validateDateTime(dateTime, now);
		validateBirthDate(patient, now.toLocalDate());

		validatePatientNotBlocked(patient, now);
		if (appointmentRepository.existsByDoctorIdAndDateTimeAndStatus(doctor.getId(), dateTime,
				AppointmentStatus.SCHEDULED)) {
			throw new ConflictException("The doctor already has an appointment in this time slot.");
		}
		if (appointmentRepository.existsByPatientIdAndDateTimeAndStatus(patient.getId(), dateTime,
				AppointmentStatus.SCHEDULED)) {
			throw new ConflictException("The patient already has an appointment at this time.");
		}

		Appointment saved = appointmentRepository.save(new Appointment(patient, doctor, dateTime));
		return appointmentMapper.toResponse(saved);
	}

	/** RN-06 (past date-time, slot start) and RN-01 (working hours). */
	private void validateDateTime(LocalDateTime dateTime, LocalDateTime now) {
		if (!dateTime.isAfter(now)) {
			throw new BusinessRuleException("Appointment date and time must be in the future.");
		}
		if (!workingHoursPolicy.isValidSlotStart(dateTime)) {
			throw new BusinessRuleException(
					"Appointment must start at a 30-minute slot within working hours "
							+ "(Mon-Fri 08:00-18:00, Sat 08:00-13:00).");
		}
	}

	/** RN-03: a missing birth date means age 0, which is valid; a future one is not. */
	private void validateBirthDate(Patient patient, LocalDate today) {
		if (patient.getBirthDate() != null && patient.getBirthDate().isAfter(today)) {
			throw new BusinessRuleException("Patient birth date cannot be in the future.");
		}
	}

	/** RN-05: 3 or more penalties in the last 30 days block new appointments. */
	private void validatePatientNotBlocked(Patient patient, LocalDateTime now) {
		long penalties = penaltyRepository.countByPatientIdAndCreatedAtAfter(patient.getId(),
				penaltyPolicy.penaltyWindowStart(now));
		if (penaltyPolicy.isBlocked(penalties)) {
			throw new ConflictException(
					"The patient has 3 or more late cancellations in the last 30 days and cannot schedule appointments.");
		}
	}

}
