package com.ceiba.prueba;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * End-to-end flows through every layer against the real H2 database (no mocks).
 * <p>
 * "Now" is fixed at Monday 2026-10-19 10:15. Each test registers its own patient; doctors come from data.sql.
 * Not transactional on purpose: every request commits, as in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AppointmentFlowIntegrationTest.FixedClockConfig.class)
class AppointmentFlowIntegrationTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 10, 15);
	private static final String TUESDAY = "2026-10-20";

	@Autowired
	private MockMvc mockMvc;

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock fixedClock() {
			ZoneId zone = ZoneId.systemDefault();
			return Clock.fixed(NOW.atZone(zone).toInstant(), zone);
		}

	}

	@Test
	void scheduleCancelAndList_slotAvailabilityFollowsTheAppointment() throws Exception {
		long patientId = registerPatient("1020304050");

		// The slot is free before scheduling
		availability(1, TUESDAY)
				.andExpect(jsonPath("$.length()").value(20))
				.andExpect(jsonPath("$[*].start", hasItem("2026-10-20T10:00:00")));

		// Schedule (RF-03)
		long appointmentId = id(schedule(patientId, 1, "2026-10-20T10:00:00")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("SCHEDULED")));

		// The slot is no longer available (RF-04)
		availability(1, TUESDAY)
				.andExpect(jsonPath("$.length()").value(19))
				.andExpect(jsonPath("$[*].start", not(hasItem("2026-10-20T10:00:00"))));

		// Same slot again: doctor conflict (RN-02)
		long otherPatientId = registerPatient("5040302010");
		schedule(otherPatientId, 1, "2026-10-20T10:00:00")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("The doctor already has an appointment in this time slot."));

		// Same time with another doctor: patient conflict (RN-04)
		schedule(patientId, 2, "2026-10-20T10:00:00")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("The patient already has an appointment at this time."));

		// Cancel (RF-05): more than 2 hours ahead, so no penalty
		mockMvc.perform(post("/api/appointments/" + appointmentId + "/cancellation"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.cancelledAt").value("2026-10-19T10:15:00"));

		// Cancelling twice is a conflict
		mockMvc.perform(post("/api/appointments/" + appointmentId + "/cancellation"))
				.andExpect(status().isConflict());

		// The slot is available again
		availability(1, TUESDAY)
				.andExpect(jsonPath("$.length()").value(20))
				.andExpect(jsonPath("$[*].start", hasItem("2026-10-20T10:00:00")));

		// List (RF-06): the cancellation was persisted
		mockMvc.perform(get("/api/appointments")
				.param("patientId", String.valueOf(patientId))
				.param("status", "CANCELLED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(appointmentId));

		// The freed slot can be booked again
		schedule(otherPatientId, 1, "2026-10-20T10:00:00").andExpect(status().isCreated());
	}

	@Test
	void threeLateCancellations_blockNewAppointments() throws Exception {
		long patientId = registerPatient("7777777777");

		// Three appointments less than 2 hours ahead, each cancelled late (RN-05)
		for (String time : new String[] { "11:00", "11:30", "12:00" }) {
			long appointmentId = id(schedule(patientId, 3, "2026-10-19T" + time + ":00")
					.andExpect(status().isCreated()));
			mockMvc.perform(post("/api/appointments/" + appointmentId + "/cancellation"))
					.andExpect(status().isOk());
		}

		schedule(patientId, 3, "2026-10-21T09:00:00")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value(
						"The patient has 3 or more late cancellations in the last 30 days and cannot schedule appointments."));
	}

	private long registerPatient(String documentNumber) throws Exception {
		return id(mockMvc.perform(post("/api/patients")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName": "Paciente %s", "documentNumber": "%s",
						 "phone": "3001234567", "email": "p%s@mail.com"}
						""".formatted(documentNumber, documentNumber, documentNumber)))
				.andExpect(status().isCreated()));
	}

	private ResultActions schedule(long patientId, long doctorId, String dateTime) throws Exception {
		return mockMvc.perform(post("/api/appointments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"patientId": %d, "doctorId": %d, "dateTime": "%s"}
						""".formatted(patientId, doctorId, dateTime)));
	}

	private ResultActions availability(long doctorId, String date) throws Exception {
		return mockMvc.perform(get("/api/doctors/" + doctorId + "/availability")
				.param("startDate", date)
				.param("endDate", date))
				.andExpect(status().isOk());
	}

	private static long id(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		assertThat(id).isNotNull();
		return id.longValue();
	}

}
