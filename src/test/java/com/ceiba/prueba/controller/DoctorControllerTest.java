package com.ceiba.prueba.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ceiba.prueba.dto.request.DoctorRequest;
import com.ceiba.prueba.dto.response.DoctorResponse;
import com.ceiba.prueba.dto.response.TimeSlotResponse;
import com.ceiba.prueba.exception.BusinessRuleException;
import com.ceiba.prueba.exception.ResourceNotFoundException;
import com.ceiba.prueba.service.AvailabilityService;
import com.ceiba.prueba.service.DoctorService;

@WebMvcTest(DoctorController.class)
class DoctorControllerTest {

	private static final String DOCTORS_URL = "/api/doctors";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DoctorService doctorService;

	@MockitoBean
	private AvailabilityService availabilityService;

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

	// --- RF-04: GET /api/doctors/{doctorId}/availability ---

	@Test
	void getAvailability_validRange_returns200WithSlots() throws Exception {
		// Given
		LocalDate monday = LocalDate.of(2026, 10, 26);
		when(availabilityService.getAvailableSlots(1L, monday, monday)).thenReturn(List.of(
				new TimeSlotResponse(monday.atTime(8, 0), monday.atTime(8, 30)),
				new TimeSlotResponse(monday.atTime(8, 30), monday.atTime(9, 0))));

		// When / Then
		mockMvc.perform(get(DOCTORS_URL + "/1/availability")
				.param("startDate", "2026-10-26")
				.param("endDate", "2026-10-26"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].start").value("2026-10-26T08:00:00"))
				.andExpect(jsonPath("$[0].end").value("2026-10-26T08:30:00"))
				.andExpect(jsonPath("$[1].start").value("2026-10-26T08:30:00"));
	}

	@Test
	void getAvailability_noFreeSlots_returns200WithEmptyList() throws Exception {
		when(availabilityService.getAvailableSlots(any(), any(), any())).thenReturn(List.of());

		mockMvc.perform(get(DOCTORS_URL + "/1/availability")
				.param("startDate", "2026-11-01")
				.param("endDate", "2026-11-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void getAvailability_missingStartDate_returns400() throws Exception {
		mockMvc.perform(get(DOCTORS_URL + "/1/availability").param("endDate", "2026-10-26"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value(containsString("startDate")));

		verifyNoInteractions(availabilityService);
	}

	@Test
	void getAvailability_missingEndDate_returns400() throws Exception {
		mockMvc.perform(get(DOCTORS_URL + "/1/availability").param("startDate", "2026-10-26"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(containsString("endDate")));

		verifyNoInteractions(availabilityService);
	}

	@Test
	void getAvailability_dateNotIso8601_returns400() throws Exception {
		mockMvc.perform(get(DOCTORS_URL + "/1/availability")
				.param("startDate", "26/10/2026")
				.param("endDate", "2026-10-26"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Invalid value for parameter 'startDate'."))
				.andExpect(content().string(not(containsString("26/10/2026"))));

		verifyNoInteractions(availabilityService);
	}

	@Test
	void getAvailability_nonNumericDoctorId_returns400() throws Exception {
		mockMvc.perform(get(DOCTORS_URL + "/abc/availability")
				.param("startDate", "2026-10-26")
				.param("endDate", "2026-10-26"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(availabilityService);
	}

	@Test
	void getAvailability_invertedRange_returns400() throws Exception {
		when(availabilityService.getAvailableSlots(any(), any(), any()))
				.thenThrow(new BusinessRuleException("startDate must be on or before endDate."));

		mockMvc.perform(get(DOCTORS_URL + "/1/availability")
				.param("startDate", "2026-10-27")
				.param("endDate", "2026-10-26"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("startDate must be on or before endDate."));
	}

	@Test
	void getAvailability_doctorNotFound_returns404() throws Exception {
		when(availabilityService.getAvailableSlots(any(), any(), any()))
				.thenThrow(new ResourceNotFoundException("Doctor not found."));

		mockMvc.perform(get(DOCTORS_URL + "/999/availability")
				.param("startDate", "2026-10-26")
				.param("endDate", "2026-10-26"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Doctor not found."));
	}

}
