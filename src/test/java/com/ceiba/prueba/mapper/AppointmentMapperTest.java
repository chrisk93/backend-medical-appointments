package com.ceiba.prueba.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ceiba.prueba.domain.model.Appointment;
import com.ceiba.prueba.domain.model.AppointmentStatus;
import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.domain.model.Patient;
import com.ceiba.prueba.dto.response.AppointmentResponse;
import com.ceiba.prueba.dto.response.TimeSlotResponse;

class AppointmentMapperTest {

	private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 10, 19, 10, 0);

	private final AppointmentMapper mapper = new AppointmentMapper();

	@Test
	void toResponse_scheduledAppointment_copiesIdsDateTimeAndStatus() {
		Appointment appointment = appointment();

		AppointmentResponse response = mapper.toResponse(appointment);

		assertThat(response).isEqualTo(
				new AppointmentResponse(7L, 3L, 1L, DATE_TIME, AppointmentStatus.SCHEDULED, null));
	}

	@Test
	void toResponse_cancelledAppointment_includesCancellationTime() {
		Appointment appointment = appointment();
		LocalDateTime cancelledAt = DATE_TIME.minusHours(5);
		appointment.cancel(cancelledAt);

		AppointmentResponse response = mapper.toResponse(appointment);

		assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
		assertThat(response.cancelledAt()).isEqualTo(cancelledAt);
	}

	@Test
	void toTimeSlot_start_endsThirtyMinutesLater() {
		assertThat(mapper.toTimeSlot(LocalDateTime.of(2026, 10, 19, 17, 30)))
				.isEqualTo(new TimeSlotResponse(LocalDateTime.of(2026, 10, 19, 17, 30),
						LocalDateTime.of(2026, 10, 19, 18, 0)));
	}

	private static Appointment appointment() {
		Patient patient = new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", null);
		ReflectionTestUtils.setField(patient, "id", 3L);
		Doctor doctor = new Doctor("Dra. María González", "Cardiología", null, null);
		ReflectionTestUtils.setField(doctor, "id", 1L);
		Appointment appointment = new Appointment(patient, doctor, DATE_TIME);
		ReflectionTestUtils.setField(appointment, "id", 7L);
		return appointment;
	}

}
