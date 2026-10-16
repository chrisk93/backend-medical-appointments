package com.ceiba.prueba.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.dto.request.DoctorRequest;
import com.ceiba.prueba.dto.response.DoctorResponse;

class DoctorMapperTest {

	private final DoctorMapper mapper = new DoctorMapper();

	@Test
	void toEntity_request_copiesAllFieldsWithoutId() {
		DoctorRequest request = new DoctorRequest("Dr. Carlos Ruiz", "Pediatría", "555-1002",
				"carlos.ruiz@medisalud.com");

		Doctor doctor = mapper.toEntity(request);

		assertThat(doctor.getId()).isNull();
		assertThat(doctor.getFullName()).isEqualTo("Dr. Carlos Ruiz");
		assertThat(doctor.getSpecialty()).isEqualTo("Pediatría");
		assertThat(doctor.getPhone()).isEqualTo("555-1002");
		assertThat(doctor.getEmail()).isEqualTo("carlos.ruiz@medisalud.com");
	}

	@Test
	void toResponse_entity_copiesAllFieldsIncludingId() {
		Doctor doctor = new Doctor("Dr. Carlos Ruiz", "Pediatría", null, null);
		ReflectionTestUtils.setField(doctor, "id", 2L);

		DoctorResponse response = mapper.toResponse(doctor);

		assertThat(response).isEqualTo(new DoctorResponse(2L, "Dr. Carlos Ruiz", "Pediatría", null, null));
	}

}
