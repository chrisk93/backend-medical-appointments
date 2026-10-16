package com.ceiba.prueba.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ceiba.prueba.domain.model.Patient;
import com.ceiba.prueba.dto.request.PatientRequest;
import com.ceiba.prueba.dto.response.PatientResponse;
import com.ceiba.prueba.exception.ConflictException;
import com.ceiba.prueba.mapper.PatientMapper;
import com.ceiba.prueba.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

	private static final PatientRequest REQUEST = new PatientRequest("Ana Torres", "1020304050", "3001234567",
			"ana.torres@mail.com", LocalDate.of(1990, 5, 20));

	@Mock
	private PatientRepository patientRepository;

	private PatientServiceImpl patientService;

	@BeforeEach
	void setUp() {
		patientService = new PatientServiceImpl(patientRepository, new PatientMapper());
	}

	@Test
	void create_newDocumentNumber_persistsPatientAndReturnsResponseWithId() {
		// Given
		when(patientRepository.existsByDocumentNumber("1020304050")).thenReturn(false);
		when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
			Patient patient = invocation.getArgument(0);
			ReflectionTestUtils.setField(patient, "id", 1L);
			return patient;
		});

		// When
		PatientResponse response = patientService.create(REQUEST);

		// Then
		ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
		verify(patientRepository).save(captor.capture());
		assertThat(captor.getValue().getDocumentNumber()).isEqualTo("1020304050");
		assertThat(response).isEqualTo(new PatientResponse(1L, "Ana Torres", "1020304050", "3001234567",
				"ana.torres@mail.com", LocalDate.of(1990, 5, 20)));
	}

	@Test
	void create_duplicateDocumentNumber_throwsConflictWithoutSaving() {
		// Given
		when(patientRepository.existsByDocumentNumber("1020304050")).thenReturn(true);

		// When / Then
		assertThatThrownBy(() -> patientService.create(REQUEST))
				.isInstanceOf(ConflictException.class)
				.hasMessageNotContaining("1020304050");
		verify(patientRepository, never()).save(any());
	}

}
