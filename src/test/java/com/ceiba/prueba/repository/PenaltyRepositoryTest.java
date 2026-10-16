package com.ceiba.prueba.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.ceiba.prueba.domain.model.Appointment;
import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.domain.model.Patient;
import com.ceiba.prueba.domain.model.Penalty;

@DataJpaTest
class PenaltyRepositoryTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 10, 0);
	private static final LocalDateTime WINDOW_START = NOW.minusDays(30);

	@Autowired
	private PenaltyRepository penaltyRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Doctor doctor;
	private Patient patient;
	private Patient otherPatient;

	@BeforeEach
	void setUp() {
		// Seed doctor: inserting doctors would shift the ID expected by DoctorRepositoryTest.
		doctor = entityManager.find(Doctor.class, 1L);
		patient = entityManager.persist(
				new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null));
		otherPatient = entityManager.persist(
				new Patient("Luis Gómez", "5040302010", "3007654321", "luis.gomez@mail.com", null));
	}

	@Test
	void countByPatientIdAndCreatedAtAfter_penaltiesInsideAndOutsideWindow_countsOnlyInside() {
		// Given
		penalize(patient, NOW.minusDays(29));
		penalize(patient, NOW.minusHours(1));
		penalize(patient, NOW.minusDays(31));
		penalize(otherPatient, NOW.minusDays(1));

		// When / Then
		assertThat(penaltyRepository.countByPatientIdAndCreatedAtAfter(patient.getId(), WINDOW_START)).isEqualTo(2);
	}

	@Test
	void countByPatientIdAndCreatedAtAfter_penaltyExactlyAtWindowStart_isNotCounted() {
		penalize(patient, WINDOW_START);

		assertThat(penaltyRepository.countByPatientIdAndCreatedAtAfter(patient.getId(), WINDOW_START)).isZero();
	}

	@Test
	void countByPatientIdAndCreatedAtAfter_noPenalties_isZero() {
		assertThat(penaltyRepository.countByPatientIdAndCreatedAtAfter(patient.getId(), WINDOW_START)).isZero();
	}

	private void penalize(Patient penalized, LocalDateTime createdAt) {
		// Each penalty needs its own appointment (one penalty per appointment).
		Appointment appointment = new Appointment(penalized, doctor, createdAt.plusHours(1));
		appointment.cancel(createdAt);
		entityManager.persist(appointment);
		entityManager.persist(new Penalty(penalized, appointment, createdAt));
		entityManager.flush();
	}

}
