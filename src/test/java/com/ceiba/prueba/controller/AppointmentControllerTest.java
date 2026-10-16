package com.ceiba.prueba.controller;

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
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ceiba.prueba.domain.model.AppointmentStatus;
import com.ceiba.prueba.dto.request.AppointmentFilter;
import com.ceiba.prueba.dto.request.AppointmentRequest;
import com.ceiba.prueba.dto.response.AppointmentResponse;
import com.ceiba.prueba.exception.BusinessRuleException;
import com.ceiba.prueba.exception.ConflictException;
import com.ceiba.prueba.exception.ResourceNotFoundException;
import com.ceiba.prueba.service.AppointmentService;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

	private static final String APPOINTMENTS_URL = "/api/appointments";
	private static final LocalDateTime TUESDAY_10_00 = LocalDateTime.of(2026, 10, 20, 10, 0);
	private static final String VALID_BODY = """
			{"patientId": 3, "doctorId": 1, "dateTime": "2026-10-20T10:00:00"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AppointmentService appointmentService;

	// --- RF-03: POST /api/appointments ---

	@Test
	void schedule_validRequest_returns201WithAppointment() throws Exception {
		when(appointmentService.schedule(any())).thenReturn(
				new AppointmentResponse(10L, 3L, 1L, TUESDAY_10_00, AppointmentStatus.SCHEDULED, null));

		mockMvc.perform(post(APPOINTMENTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.patientId").value(3))
				.andExpect(jsonPath("$.doctorId").value(1))
				.andExpect(jsonPath("$.dateTime").value("2026-10-20T10:00:00"))
				.andExpect(jsonPath("$.status").value("SCHEDULED"))
				.andExpect(jsonPath("$.cancelledAt").doesNotExist());

		verify(appointmentService).schedule(new AppointmentRequest(3L, 1L, TUESDAY_10_00));
	}

	@Test
	void schedule_missingFields_returns400WithFieldErrors() throws Exception {
		mockMvc.perform(post(APPOINTMENTS_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors.length()").value(3))
				.andExpect(jsonPath("$.errors[?(@.field == 'patientId')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'doctorId')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'dateTime')]").exists());

		verifyNoInteractions(appointmentService);
	}

	@Test
	void schedule_dateTimeNotIso8601_returns400() throws Exception {
		mockMvc.perform(post(APPOINTMENTS_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"patientId": 3, "doctorId": 1, "dateTime": "20/10/2026 10:00"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		verifyNoInteractions(appointmentService);
	}

	@Test
	void schedule_businessRuleViolated_returns400() throws Exception {
		when(appointmentService.schedule(any()))
				.thenThrow(new BusinessRuleException("Appointment date and time must be in the future."));

		mockMvc.perform(post(APPOINTMENTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Appointment date and time must be in the future."));
	}

	@Test
	void schedule_patientNotFound_returns404() throws Exception {
		when(appointmentService.schedule(any())).thenThrow(new ResourceNotFoundException("Patient not found."));

		mockMvc.perform(post(APPOINTMENTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Patient not found."));
	}

	@Test
	void schedule_slotTaken_returns409() throws Exception {
		when(appointmentService.schedule(any()))
				.thenThrow(new ConflictException("The doctor already has an appointment in this time slot."));

		mockMvc.perform(post(APPOINTMENTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("The doctor already has an appointment in this time slot."));
	}

	// --- RF-05: POST /api/appointments/{id}/cancellation ---

	@Test
	void cancel_scheduledAppointment_returns200WithCancelledAppointment() throws Exception {
		LocalDateTime cancelledAt = LocalDateTime.of(2026, 10, 19, 10, 15);
		when(appointmentService.cancel(10L)).thenReturn(
				new AppointmentResponse(10L, 3L, 1L, TUESDAY_10_00, AppointmentStatus.CANCELLED, cancelledAt));

		mockMvc.perform(post(APPOINTMENTS_URL + "/10/cancellation"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.cancelledAt").value("2026-10-19T10:15:00"));
	}

	@Test
	void cancel_appointmentNotFound_returns404() throws Exception {
		when(appointmentService.cancel(99L)).thenThrow(new ResourceNotFoundException("Appointment not found."));

		mockMvc.perform(post(APPOINTMENTS_URL + "/99/cancellation"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Appointment not found."));
	}

	@Test
	void cancel_alreadyCancelled_returns409() throws Exception {
		when(appointmentService.cancel(10L))
				.thenThrow(new ConflictException("Only scheduled appointments can be cancelled."));

		mockMvc.perform(post(APPOINTMENTS_URL + "/10/cancellation"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Only scheduled appointments can be cancelled."));
	}

	@Test
	void cancel_nonNumericId_returns400() throws Exception {
		mockMvc.perform(post(APPOINTMENTS_URL + "/abc/cancellation"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(appointmentService);
	}

	// --- RF-06: GET /api/appointments ---

	@Test
	void search_allFilters_returns200AndBindsQueryParameters() throws Exception {
		when(appointmentService.search(any())).thenReturn(List.of(
				new AppointmentResponse(10L, 3L, 1L, TUESDAY_10_00, AppointmentStatus.SCHEDULED, null)));

		mockMvc.perform(get(APPOINTMENTS_URL)
				.param("doctorId", "1")
				.param("patientId", "3")
				.param("status", "SCHEDULED")
				.param("startDate", "2026-10-19")
				.param("endDate", "2026-10-25"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(10));

		verify(appointmentService).search(new AppointmentFilter(1L, 3L, AppointmentStatus.SCHEDULED,
				LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25)));
	}

	@Test
	void search_noFilters_returns200WithEmptyFilter() throws Exception {
		when(appointmentService.search(any())).thenReturn(List.of());

		mockMvc.perform(get(APPOINTMENTS_URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		verify(appointmentService).search(new AppointmentFilter(null, null, null, null, null));
	}

	@Test
	void search_unknownStatus_returns400WithoutEchoingInternals() throws Exception {
		mockMvc.perform(get(APPOINTMENTS_URL).param("status", "FOO"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[0].field").value("status"))
				.andExpect(jsonPath("$.errors[0].message").value("Invalid value."));

		verifyNoInteractions(appointmentService);
	}

	@Test
	void search_malformedStartDate_returns400() throws Exception {
		mockMvc.perform(get(APPOINTMENTS_URL).param("startDate", "19/10/2026"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("startDate"));

		verifyNoInteractions(appointmentService);
	}

	@Test
	void search_nonNumericDoctorId_returns400() throws Exception {
		mockMvc.perform(get(APPOINTMENTS_URL).param("doctorId", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("doctorId"));

		verifyNoInteractions(appointmentService);
	}

	@Test
	void search_invertedRange_returns400() throws Exception {
		when(appointmentService.search(any()))
				.thenThrow(new BusinessRuleException("startDate must be on or before endDate."));

		mockMvc.perform(get(APPOINTMENTS_URL).param("startDate", "2026-10-25").param("endDate", "2026-10-19"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("startDate must be on or before endDate."));
	}

}
