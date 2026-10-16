package com.ceiba.prueba.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ceiba.prueba.dto.request.DoctorRequest;
import com.ceiba.prueba.dto.response.DoctorResponse;
import com.ceiba.prueba.service.DoctorService;

@WebMvcTest(DoctorController.class)
class DoctorControllerTest {

	private static final String DOCTORS_URL = "/api/doctors";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DoctorService doctorService;

	@Test
	void create_validRequest_returns201WithDoctor() throws Exception {
		// Given
		DoctorRequest request = new DoctorRequest("Dr. Luis Pérez", "Neurología", "555-1004",
				"luis.perez@medisalud.com");
		when(doctorService.create(any(DoctorRequest.class))).thenReturn(
				new DoctorResponse(4L, "Dr. Luis Pérez", "Neurología", "555-1004", "luis.perez@medisalud.com"));

		// When / Then
		mockMvc.perform(post(DOCTORS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Dr. Luis Pérez",
						  "specialty": "Neurología",
						  "phone": "555-1004",
						  "email": "luis.perez@medisalud.com"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(4))
				.andExpect(jsonPath("$.fullName").value("Dr. Luis Pérez"))
				.andExpect(jsonPath("$.specialty").value("Neurología"))
				.andExpect(jsonPath("$.phone").value("555-1004"))
				.andExpect(jsonPath("$.email").value("luis.perez@medisalud.com"));

		verify(doctorService).create(request);
	}

	@Test
	void create_onlyRequiredFields_returns201() throws Exception {
		// Given
		when(doctorService.create(any(DoctorRequest.class)))
				.thenReturn(new DoctorResponse(4L, "Dr. Luis Pérez", "Neurología", null, null));

		// When / Then
		mockMvc.perform(post(DOCTORS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName": "Dr. Luis Pérez", "specialty": "Neurología"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(4));
	}

	@Test
	void create_invalidFields_returns400WithFieldErrors() throws Exception {
		mockMvc.perform(post(DOCTORS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName": "Dr", "specialty": "Neurología", "phone": "12345"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors.length()").value(2))
				.andExpect(jsonPath("$.errors[?(@.field == 'fullName')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'phone')].message").value("must contain at least 7 digits"));

		verifyNoInteractions(doctorService);
	}

	@Test
	void create_missingRequiredFields_returns400() throws Exception {
		mockMvc.perform(post(DOCTORS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field == 'fullName')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'specialty')]").exists());

		verifyNoInteractions(doctorService);
	}

	@Test
	void create_malformedJson_returns400() throws Exception {
		mockMvc.perform(post(DOCTORS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fullName\": "))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(doctorService);
	}

}
