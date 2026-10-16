package com.ceiba.prueba.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.ceiba.prueba.exception.ConflictException;

class AppointmentTest {

	private static final LocalDateTime APPOINTMENT_TIME = LocalDateTime.of(2026, 10, 19, 10, 0);
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 18, 9, 15);

	private final Patient patient = new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com",
			null);
	private final Doctor doctor = new Doctor("Dra. María González", "Cardiología", "555-1001",
			"maria.gonzalez@medisalud.com");

	@Test
	void new_appointment_isScheduledAndNotCancelled() {
		Appointment appointment = new Appointment(patient, doctor, APPOINTMENT_TIME);

		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
		assertThat(appointment.getCancelledAt()).isNull();
		assertThat(appointment.getPatient()).isSameAs(patient);
		assertThat(appointment.getDoctor()).isSameAs(doctor);
		assertThat(appointment.getDateTime()).isEqualTo(APPOINTMENT_TIME);
	}

	@Test
	void cancel_scheduledAppointment_setsCancelledStatusAndCancellationTime() {
		Appointment appointment = new Appointment(patient, doctor, APPOINTMENT_TIME);

		appointment.cancel(NOW);

		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
		assertThat(appointment.getCancelledAt()).isEqualTo(NOW);
	}

	@Test
	void cancel_alreadyCancelledAppointment_throwsConflictAndKeepsOriginalCancellation() {
		Appointment appointment = new Appointment(patient, doctor, APPOINTMENT_TIME);
		appointment.cancel(NOW);

		assertThatThrownBy(() -> appointment.cancel(NOW.plusHours(1)))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only scheduled appointments can be cancelled.");
		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
		assertThat(appointment.getCancelledAt()).isEqualTo(NOW);
	}

}
