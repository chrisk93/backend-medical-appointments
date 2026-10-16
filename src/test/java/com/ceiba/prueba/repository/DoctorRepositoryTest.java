package com.ceiba.prueba.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.ceiba.prueba.domain.model.Doctor;

@DataJpaTest
class DoctorRepositoryTest {

	@Autowired
	private DoctorRepository doctorRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void findAll_afterStartup_containsSeedDoctorsWithIds1To3() {
		// When
		List<Doctor> doctors = doctorRepository.findAll();

		// Then
		assertThat(doctors)
				.extracting(Doctor::getId, Doctor::getFullName, Doctor::getSpecialty, Doctor::getPhone, Doctor::getEmail)
				.containsExactlyInAnyOrder(
						tuple(1L, "Dra. María González", "Cardiología", "555-1001",
								"maria.gonzalez@medisalud.com"),
						tuple(2L, "Dr. Carlos Ruiz", "Pediatría", "555-1002",
								"carlos.ruiz@medisalud.com"),
						tuple(3L, "Dra. Ana López", "Dermatología", "555-1003",
								"ana.lopez@medisalud.com"));
	}

	// Only test in this class that inserts: identity values are not rolled back, so a second insert would shift the ID.
	@Test
	void save_newDoctor_getsId4AndCanBeFound() {
		// Given
		Doctor doctor = new Doctor("Dr. Luis Pérez", "Neurología", null, null);

		// When
		Doctor saved = doctorRepository.save(doctor);
		entityManager.flush();
		entityManager.clear();

		// Then
		assertThat(saved.getId()).isEqualTo(4L);
		assertThat(doctorRepository.findById(4L))
				.get()
				.satisfies(found -> {
					assertThat(found.getFullName()).isEqualTo("Dr. Luis Pérez");
					assertThat(found.getSpecialty()).isEqualTo("Neurología");
					assertThat(found.getPhone()).isNull();
					assertThat(found.getEmail()).isNull();
				});
	}

}
