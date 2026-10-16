package com.ceiba.prueba.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ceiba.prueba.dto.request.PatientRequest;
import com.ceiba.prueba.dto.response.PatientResponse;
import com.ceiba.prueba.exception.ConflictException;
import com.ceiba.prueba.service.PatientService;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

	private static final String PATIENTS_URL = "/api/patients";

	private static final String VALID_BODY = """
			{
			  "fullName": "Ana Torres",
			  "documentNumber": "1020304050",
			  "phone": "3001234567",
			  "email": "ana.torres@mail.com",
			  "birthDate": "1990-05-20"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PatientService patientService;

	@Test
	void create_validRequest_returns201WithPatient() throws Exception {
		// Given
		when(patientService.create(any(PatientRequest.class))).thenReturn(new PatientResponse(1L, "Ana Torres",
				"1020304050", "3001234567", "ana.torres@mail.com", LocalDate.of(1990, 5, 20)));

		// When / Then
		mockMvc.perform(post(PATIENTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.fullName").value("Ana Torres"))
				.andExpect(jsonPath("$.documentNumber").value("1020304050"))
				.andExpect(jsonPath("$.phone").value("3001234567"))
				.andExpect(jsonPath("$.email").value("ana.torres@mail.com"))
				.andExpect(jsonPath("$.birthDate").value("1990-05-20"));

		verify(patientService).create(new PatientRequest("Ana Torres", "1020304050", "3001234567",
				"ana.torres@mail.com", LocalDate.of(1990, 5, 20)));
	}

	@Test
	void create_withoutBirthDate_returns201() throws Exception {
		// Given
		when(patientService.create(any(PatientRequest.class))).thenReturn(
				new PatientResponse(1L, "Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null));

		// When / Then
		mockMvc.perform(post(PATIENTS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName": "Ana Torres", "documentNumber": "1020304050",
						 "phone": "3001234567", "email": "ana.torres@mail.com"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.birthDate").doesNotExist());
	}

	@Test
	void create_invalidFields_returns400WithFieldErrors() throws Exception {
		mockMvc.perform(post(PATIENTS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName": "Ana Torres", "documentNumber": "123456",
						 "phone": "3001234567", "email": "not-an-email", "birthDate": "2999-01-01"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors.length()").value(3))
				.andExpect(jsonPath("$.errors[?(@.field == 'documentNumber')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'birthDate')]").exists());

		verifyNoInteractions(patientService);
	}

	@Test
	void create_birthDateNotIso8601_returns400() throws Exception {
		mockMvc.perform(post(PATIENTS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName": "Ana Torres", "documentNumber": "1020304050",
						 "phone": "3001234567", "email": "ana.torres@mail.com", "birthDate": "15/10/2000"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(patientService);
	}

	@Test
	void create_malformedJson_returns400() throws Exception {
		mockMvc.perform(post(PATIENTS_URL).contentType(MediaType.APPLICATION_JSON).content("{\"fullName\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(patientService);
	}

	@Test
	void create_duplicateDocumentNumber_returns409() throws Exception {
		// Given
		when(patientService.create(any(PatientRequest.class)))
				.thenThrow(new ConflictException("A patient with this document number is already registered."));

		// When / Then
		mockMvc.perform(post(PATIENTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("A patient with this document number is already registered."));
	}

}
