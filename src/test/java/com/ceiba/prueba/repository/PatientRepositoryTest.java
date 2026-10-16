package com.ceiba.prueba.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.ceiba.prueba.domain.model.Patient;

@DataJpaTest
class PatientRepositoryTest {

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void save_patient_persistsAllFieldsIncludingOptionalBirthDate() {
		// Given
		Patient patient = new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com",
				LocalDate.of(1990, 5, 20));

		// When
		Long id = patientRepository.save(patient).getId();
		entityManager.flush();
		entityManager.clear();

		// Then
		assertThat(patientRepository.findById(id)).get().satisfies(found -> {
			assertThat(found.getFullName()).isEqualTo("Ana Torres");
			assertThat(found.getDocumentNumber()).isEqualTo("1020304050");
			assertThat(found.getPhone()).isEqualTo("3001234567");
			assertThat(found.getEmail()).isEqualTo("ana.torres@mail.com");
			assertThat(found.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 20));
		});
	}

	@Test
	void save_patientWithoutBirthDate_isAllowed() {
		Patient saved = patientRepository.saveAndFlush(
				new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getBirthDate()).isNull();
	}

	@Test
	void existsByDocumentNumber_existingAndMissingDocument_returnsExpected() {
		// Given
		entityManager.persistAndFlush(
				new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null));

		// When / Then
		assertThat(patientRepository.existsByDocumentNumber("1020304050")).isTrue();
		assertThat(patientRepository.existsByDocumentNumber("9999999")).isFalse();
	}

	@Test
	void save_duplicateDocumentNumber_throwsDataIntegrityViolation() {
		// Given
		patientRepository.saveAndFlush(
				new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null));
		Patient duplicate = new Patient("Otra Persona", "1020304050", "3007654321", "otra@mail.com", null);

		// When / Then
		assertThatThrownBy(() -> patientRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
