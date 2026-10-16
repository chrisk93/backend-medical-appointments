package com.ceiba.prueba.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.ceiba.prueba.domain.model.Appointment;
import com.ceiba.prueba.domain.model.AppointmentStatus;
import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.domain.model.Patient;

/**
 * Uses the seed doctors (IDs 1 and 2) instead of inserting new ones: identity values are not rolled back and
 * {@link DoctorRepositoryTest} expects the next doctor ID to be 4.
 */
@DataJpaTest
class AppointmentRepositoryTest {

	private static final LocalDateTime MONDAY_10_00 = LocalDateTime.of(2026, 10, 19, 10, 0);

	@Autowired
	private AppointmentRepository appointmentRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Doctor doctor;
	private Doctor otherDoctor;
	private Patient patient;
	private Patient otherPatient;

	@BeforeEach
	void setUp() {
		doctor = entityManager.find(Doctor.class, 1L);
		otherDoctor = entityManager.find(Doctor.class, 2L);
		patient = entityManager.persist(
				new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null));
		otherPatient = entityManager.persist(
				new Patient("Luis Gómez", "5040302010", "3007654321", "luis.gomez@mail.com", null));
	}

	@Test
	void existsByDoctorIdAndDateTimeAndStatus_scheduledInSlot_isTrue() {
		persist(new Appointment(patient, doctor, MONDAY_10_00));

		assertThat(appointmentRepository.existsByDoctorIdAndDateTimeAndStatus(doctor.getId(), MONDAY_10_00,
				AppointmentStatus.SCHEDULED)).isTrue();
	}

	@Test
	void existsByDoctorIdAndDateTimeAndStatus_otherSlotOrOtherDoctor_isFalse() {
		persist(new Appointment(patient, doctor, MONDAY_10_00));

		assertThat(appointmentRepository.existsByDoctorIdAndDateTimeAndStatus(doctor.getId(),
				MONDAY_10_00.plusMinutes(30), AppointmentStatus.SCHEDULED)).isFalse();
		assertThat(appointmentRepository.existsByDoctorIdAndDateTimeAndStatus(otherDoctor.getId(), MONDAY_10_00,
				AppointmentStatus.SCHEDULED)).isFalse();
	}

	@Test
	void existsByDoctorIdAndDateTimeAndStatus_cancelledInSlot_isNotAConflict() {
		Appointment cancelled = new Appointment(patient, doctor, MONDAY_10_00);
		cancelled.cancel(MONDAY_10_00.minusDays(1));
		persist(cancelled);

		assertThat(appointmentRepository.existsByDoctorIdAndDateTimeAndStatus(doctor.getId(), MONDAY_10_00,
				AppointmentStatus.SCHEDULED)).isFalse();
	}

	@Test
	void existsByPatientIdAndDateTimeAndStatus_scheduledWithAnyDoctor_isTrue() {
		persist(new Appointment(patient, otherDoctor, MONDAY_10_00));

		assertThat(appointmentRepository.existsByPatientIdAndDateTimeAndStatus(patient.getId(), MONDAY_10_00,
				AppointmentStatus.SCHEDULED)).isTrue();
		assertThat(appointmentRepository.existsByPatientIdAndDateTimeAndStatus(otherPatient.getId(), MONDAY_10_00,
				AppointmentStatus.SCHEDULED)).isFalse();
	}

	@Test
	void existsByPatientIdAndDateTimeAndStatus_cancelledInSlot_isNotAConflict() {
		Appointment cancelled = new Appointment(patient, doctor, MONDAY_10_00);
		cancelled.cancel(MONDAY_10_00.minusDays(1));
		persist(cancelled);

		assertThat(appointmentRepository.existsByPatientIdAndDateTimeAndStatus(patient.getId(), MONDAY_10_00,
				AppointmentStatus.SCHEDULED)).isFalse();
	}

	@Test
	void findDateTimesByDoctorIdAndStatusInRange_returnsOnlyMatchingAppointmentsWithinRange() {
		// Given: range [Monday 00:00, Wednesday 00:00)
		LocalDateTime from = LocalDateTime.of(2026, 10, 19, 0, 0);
		LocalDateTime to = LocalDateTime.of(2026, 10, 21, 0, 0);
		persist(new Appointment(patient, doctor, LocalDateTime.of(2026, 10, 19, 8, 0)));
		persist(new Appointment(otherPatient, doctor, LocalDateTime.of(2026, 10, 20, 17, 30)));
		persist(new Appointment(patient, doctor, LocalDateTime.of(2026, 10, 21, 8, 0)));
		persist(new Appointment(patient, doctor, LocalDateTime.of(2026, 10, 18, 23, 30)));
		persist(new Appointment(patient, otherDoctor, LocalDateTime.of(2026, 10, 19, 9, 0)));
		Appointment cancelled = new Appointment(otherPatient, doctor, LocalDateTime.of(2026, 10, 19, 9, 30));
		cancelled.cancel(from);
		persist(cancelled);

		// When / Then
		assertThat(appointmentRepository.findDateTimesByDoctorIdAndStatusInRange(doctor.getId(),
				AppointmentStatus.SCHEDULED, from, to))
				.containsExactlyInAnyOrder(LocalDateTime.of(2026, 10, 19, 8, 0), LocalDateTime.of(2026, 10, 20, 17, 30));
	}

	private void persist(Appointment appointment) {
		entityManager.persist(appointment);
		entityManager.flush();
	}

}
