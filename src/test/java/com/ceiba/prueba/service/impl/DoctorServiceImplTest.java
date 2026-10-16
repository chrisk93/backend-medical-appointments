package com.ceiba.prueba.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.dto.request.DoctorRequest;
import com.ceiba.prueba.dto.response.DoctorResponse;
import com.ceiba.prueba.mapper.DoctorMapper;
import com.ceiba.prueba.repository.DoctorRepository;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

	@Mock
	private DoctorRepository doctorRepository;

	private DoctorServiceImpl doctorService;

	@BeforeEach
	void setUp() {
		doctorService = new DoctorServiceImpl(doctorRepository, new DoctorMapper());
	}

	@Test
	void create_validRequest_persistsDoctorAndReturnsResponseWithId() {
		// Given
		DoctorRequest request = new DoctorRequest("Dr. Luis Pérez", "Neurología", "555-1004",
				"luis.perez@medisalud.com");
		when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> {
			Doctor doctor = invocation.getArgument(0);
			ReflectionTestUtils.setField(doctor, "id", 4L);
			return doctor;
		});

		// When
		DoctorResponse response = doctorService.create(request);

		// Then
		ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
		verify(doctorRepository).save(captor.capture());
		Doctor persisted = captor.getValue();
		assertThat(persisted.getFullName()).isEqualTo("Dr. Luis Pérez");
		assertThat(persisted.getSpecialty()).isEqualTo("Neurología");
		assertThat(persisted.getPhone()).isEqualTo("555-1004");
		assertThat(persisted.getEmail()).isEqualTo("luis.perez@medisalud.com");

		assertThat(response).isEqualTo(
				new DoctorResponse(4L, "Dr. Luis Pérez", "Neurología", "555-1004", "luis.perez@medisalud.com"));
	}

}
