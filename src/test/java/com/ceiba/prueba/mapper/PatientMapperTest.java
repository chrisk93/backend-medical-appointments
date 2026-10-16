package com.ceiba.prueba.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ceiba.prueba.domain.model.Patient;
import com.ceiba.prueba.dto.request.PatientRequest;
import com.ceiba.prueba.dto.response.PatientResponse;

class PatientMapperTest {

	private final PatientMapper mapper = new PatientMapper();

	@Test
	void toEntity_request_copiesAllFieldsWithoutId() {
		PatientRequest request = new PatientRequest("Ana Torres", "1020304050", "3001234567",
				"ana.torres@mail.com", LocalDate.of(1990, 5, 20));

		Patient patient = mapper.toEntity(request);

		assertThat(patient.getId()).isNull();
		assertThat(patient.getFullName()).isEqualTo("Ana Torres");
		assertThat(patient.getDocumentNumber()).isEqualTo("1020304050");
		assertThat(patient.getPhone()).isEqualTo("3001234567");
		assertThat(patient.getEmail()).isEqualTo("ana.torres@mail.com");
		assertThat(patient.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 20));
	}

	@Test
	void toResponse_entity_copiesAllFieldsIncludingId() {
		Patient patient = new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null);
		ReflectionTestUtils.setField(patient, "id", 1L);

		PatientResponse response = mapper.toResponse(patient);

		assertThat(response).isEqualTo(
				new PatientResponse(1L, "Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null));
	}

}
