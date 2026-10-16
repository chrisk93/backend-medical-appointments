package com.ceiba.prueba.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ceiba.prueba.domain.model.Appointment;
import com.ceiba.prueba.domain.model.AppointmentStatus;
import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.domain.model.Patient;
import com.ceiba.prueba.domain.model.Penalty;
import com.ceiba.prueba.domain.policy.PenaltyPolicy;
import com.ceiba.prueba.domain.policy.WorkingHoursPolicy;
import com.ceiba.prueba.dto.request.AppointmentRequest;
import com.ceiba.prueba.dto.response.AppointmentResponse;
import com.ceiba.prueba.exception.BusinessRuleException;
import com.ceiba.prueba.exception.ConflictException;
import com.ceiba.prueba.exception.ResourceNotFoundException;
import com.ceiba.prueba.mapper.AppointmentMapper;
import com.ceiba.prueba.repository.AppointmentRepository;
import com.ceiba.prueba.repository.DoctorRepository;
import com.ceiba.prueba.repository.PatientRepository;
import com.ceiba.prueba.repository.PenaltyRepository;

/**
 * Repositories are mocked; policies and mapper are real, so the business rules are exercised end to end.
 * "Now" is fixed at Monday 2026-10-19 10:15.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 10, 15);
	private static final LocalDateTime TUESDAY_10_00 = LocalDateTime.of(2026, 10, 20, 10, 0);
	private static final Long PATIENT_ID = 3L;
	private static final Long DOCTOR_ID = 1L;

	@Mock
	private AppointmentRepository appointmentRepository;

	@Mock
	private PatientRepository patientRepository;

	@Mock
	private DoctorRepository doctorRepository;

	@Mock
	private PenaltyRepository penaltyRepository;

	private AppointmentServiceImpl service;

	private Patient patient;
	private Doctor doctor;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
		service = new AppointmentServiceImpl(appointmentRepository, patientRepository, doctorRepository,
				penaltyRepository, new WorkingHoursPolicy(), new PenaltyPolicy(), new AppointmentMapper(), clock);
		patient = patient(null);
		doctor = new Doctor("Dra. María González", "Cardiología", "555-1001", "maria.gonzalez@medisalud.com");
		ReflectionTestUtils.setField(doctor, "id", DOCTOR_ID);
	}

	@Nested
	class Schedule {

		@Test
		void schedule_validRequest_savesScheduledAppointment() {
			// Given
			givenPatientAndDoctorExist();
			givenPenalties(0);
			givenSlotFree(TUESDAY_10_00);
			when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
				Appointment appointment = invocation.getArgument(0);
				ReflectionTestUtils.setField(appointment, "id", 10L);
				return appointment;
			});

			// When
			AppointmentResponse response = service.schedule(request(TUESDAY_10_00));

			// Then
			ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
			verify(appointmentRepository).save(captor.capture());
			assertThat(captor.getValue().getPatient()).isSameAs(patient);
			assertThat(captor.getValue().getDoctor()).isSameAs(doctor);
			assertThat(response).isEqualTo(new AppointmentResponse(10L, PATIENT_ID, DOCTOR_ID, TUESDAY_10_00,
					AppointmentStatus.SCHEDULED, null));
		}

		@Test
		void schedule_laterSlotToday_isAllowed() {
			LocalDateTime today1030 = NOW.withMinute(30);
			givenPatientAndDoctorExist();
			givenPenalties(0);
			givenSlotFree(today1030);
			givenSaveReturnsArgument();

			assertThat(service.schedule(request(today1030)).dateTime()).isEqualTo(today1030);
		}

		@Test
		void schedule_saturdayLastSlot_isAllowed() {
			LocalDateTime saturday1230 = LocalDateTime.of(2026, 10, 24, 12, 30);
			givenPatientAndDoctorExist();
			givenPenalties(0);
			givenSlotFree(saturday1230);
			givenSaveReturnsArgument();

			assertThat(service.schedule(request(saturday1230)).status()).isEqualTo(AppointmentStatus.SCHEDULED);
		}

		@Test
		void schedule_patientNotFound_throwsNotFound() {
			when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.schedule(request(TUESDAY_10_00)))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessage("Patient not found.");
			verify(appointmentRepository, never()).save(any());
		}

		@Test
		void schedule_doctorNotFound_throwsNotFound() {
			when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
			when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.schedule(request(TUESDAY_10_00)))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessage("Doctor not found.");
			verify(appointmentRepository, never()).save(any());
		}

		@Test
		void schedule_patientNotFoundAndPastDate_reportsNotFoundFirst() {
			when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.schedule(request(NOW.minusDays(3))))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@ParameterizedTest
		@ValueSource(strings = { "2026-10-16T10:00", "2026-10-19T10:00", "2026-10-19T08:00" })
		void schedule_pastDateTime_throwsBusinessRule(String dateTime) {
			givenPatientAndDoctorExist();

			assertThatThrownBy(() -> service.schedule(request(LocalDateTime.parse(dateTime))))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessage("Appointment date and time must be in the future.");
			verify(appointmentRepository, never()).save(any());
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"2026-10-20T08:15", // not a slot start
				"2026-10-20T07:30", // before opening
				"2026-10-20T18:00", // weekday closing time
				"2026-10-24T13:00", // Saturday closing time
				"2026-10-25T10:00", // Sunday
				"2026-10-20T10:00:30" // seconds
		})
		void schedule_outsideWorkingHoursOrMisaligned_throwsBusinessRule(String dateTime) {
			givenPatientAndDoctorExist();

			assertThatThrownBy(() -> service.schedule(request(LocalDateTime.parse(dateTime))))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageStartingWith("Appointment must start at a 30-minute slot within working hours");
			verify(appointmentRepository, never()).save(any());
		}

		@Test
		void schedule_patientWithFutureBirthDate_throwsBusinessRule() {
			patient = patient(NOW.toLocalDate().plusDays(1));
			givenPatientAndDoctorExist();

			assertThatThrownBy(() -> service.schedule(request(TUESDAY_10_00)))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessage("Patient birth date cannot be in the future.");
		}

		@Test
		void schedule_patientBornToday_isAllowed() {
			patient = patient(NOW.toLocalDate());
			givenPatientAndDoctorExist();
			givenPenalties(0);
			givenSlotFree(TUESDAY_10_00);
			givenSaveReturnsArgument();

			assertThat(service.schedule(request(TUESDAY_10_00)).status()).isEqualTo(AppointmentStatus.SCHEDULED);
		}

		@Test
		void schedule_patientWithoutBirthDate_isAllowedAsAgeZero() {
			givenPatientAndDoctorExist();
			givenPenalties(0);
			givenSlotFree(TUESDAY_10_00);
			givenSaveReturnsArgument();

			assertThat(service.schedule(request(TUESDAY_10_00)).status()).isEqualTo(AppointmentStatus.SCHEDULED);
		}

		@Test
		void schedule_patientWith3PenaltiesInWindow_throwsConflict() {
			givenPatientAndDoctorExist();
			givenPenalties(3);

			assertThatThrownBy(() -> service.schedule(request(TUESDAY_10_00)))
					.isInstanceOf(ConflictException.class)
					.hasMessageContaining("3 or more late cancellations");
			verify(appointmentRepository, never()).save(any());
		}

		@Test
		void schedule_patientWith2PenaltiesInWindow_isAllowed() {
			givenPatientAndDoctorExist();
			givenPenalties(2);
			givenSlotFree(TUESDAY_10_00);
			givenSaveReturnsArgument();

			assertThat(service.schedule(request(TUESDAY_10_00)).status()).isEqualTo(AppointmentStatus.SCHEDULED);
		}

		@Test
		void schedule_countsPenaltiesFrom30DaysBeforeNow() {
			givenPatientAndDoctorExist();
			givenPenalties(0);
			givenSlotFree(TUESDAY_10_00);
			givenSaveReturnsArgument();

			service.schedule(request(TUESDAY_10_00));

			verify(penaltyRepository).countByPatientIdAndCreatedAtAfter(PATIENT_ID, NOW.minusDays(30));
		}

		@Test
		void schedule_doctorSlotTaken_throwsConflict() {
			givenPatientAndDoctorExist();
			givenPenalties(0);
			when(appointmentRepository.existsByDoctorIdAndDateTimeAndStatus(DOCTOR_ID, TUESDAY_10_00,
					AppointmentStatus.SCHEDULED)).thenReturn(true);

			assertThatThrownBy(() -> service.schedule(request(TUESDAY_10_00)))
					.isInstanceOf(ConflictException.class)
					.hasMessage("The doctor already has an appointment in this time slot.");
			verify(appointmentRepository, never()).save(any());
		}

		@Test
		void schedule_patientHasAppointmentAtSameTimeWithOtherDoctor_throwsConflict() {
			givenPatientAndDoctorExist();
			givenPenalties(0);
			when(appointmentRepository.existsByDoctorIdAndDateTimeAndStatus(DOCTOR_ID, TUESDAY_10_00,
					AppointmentStatus.SCHEDULED)).thenReturn(false);
			when(appointmentRepository.existsByPatientIdAndDateTimeAndStatus(PATIENT_ID, TUESDAY_10_00,
					AppointmentStatus.SCHEDULED)).thenReturn(true);

			assertThatThrownBy(() -> service.schedule(request(TUESDAY_10_00)))
					.isInstanceOf(ConflictException.class)
					.hasMessage("The patient already has an appointment at this time.");
			verify(appointmentRepository, never()).save(any());
		}

		@Test
		void schedule_slotWithOnlyCancelledAppointment_isAllowed() {
			// Conflicts are checked only against SCHEDULED appointments, so a cancelled one frees the slot.
			givenPatientAndDoctorExist();
			givenPenalties(0);
			givenSlotFree(TUESDAY_10_00);
			givenSaveReturnsArgument();

			service.schedule(request(TUESDAY_10_00));

			verify(appointmentRepository).existsByDoctorIdAndDateTimeAndStatus(DOCTOR_ID, TUESDAY_10_00,
					AppointmentStatus.SCHEDULED);
			verify(appointmentRepository).existsByPatientIdAndDateTimeAndStatus(PATIENT_ID, TUESDAY_10_00,
					AppointmentStatus.SCHEDULED);
		}

	}

	@Nested
	class Cancel {

		private static final Long APPOINTMENT_ID = 10L;

		@Test
		void cancel_3HoursBefore_cancelsWithoutPenalty() {
			Appointment appointment = givenScheduledAppointment(NOW.plusHours(3));

			AppointmentResponse response = service.cancel(APPOINTMENT_ID);

			assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
			assertThat(response.cancelledAt()).isEqualTo(NOW);
			assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
			verify(penaltyRepository, never()).save(any());
		}

		@Test
		void cancel_exactly2HoursBefore_cancelsWithoutPenalty() {
			givenScheduledAppointment(NOW.plusHours(2));

			service.cancel(APPOINTMENT_ID);

			verify(penaltyRepository, never()).save(any());
		}

		@Test
		void cancel_1HourBefore_registersPenaltyForPatient() {
			Appointment appointment = givenScheduledAppointment(NOW.plusHours(1));

			service.cancel(APPOINTMENT_ID);

			ArgumentCaptor<Penalty> captor = ArgumentCaptor.forClass(Penalty.class);
			verify(penaltyRepository).save(captor.capture());
			assertThat(captor.getValue().getPatient()).isSameAs(patient);
			assertThat(captor.getValue().getAppointment()).isSameAs(appointment);
			assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
		}

		@Test
		void cancel_appointmentAlreadyPassed_registersPenalty() {
			Appointment appointment = givenScheduledAppointment(NOW.minusHours(2));

			AppointmentResponse response = service.cancel(APPOINTMENT_ID);

			assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
			verify(penaltyRepository).save(any(Penalty.class));
			assertThat(appointment.getCancelledAt()).isEqualTo(NOW);
		}

		@Test
		void cancel_alreadyCancelled_throwsConflictWithoutPenalty() {
			Appointment appointment = givenScheduledAppointment(NOW.plusHours(1));
			LocalDateTime firstCancellation = NOW.minusDays(1);
			appointment.cancel(firstCancellation);

			assertThatThrownBy(() -> service.cancel(APPOINTMENT_ID))
					.isInstanceOf(ConflictException.class)
					.hasMessage("Only scheduled appointments can be cancelled.");
			assertThat(appointment.getCancelledAt()).isEqualTo(firstCancellation);
			verify(penaltyRepository, never()).save(any());
		}

		@Test
		void cancel_appointmentNotFound_throwsNotFound() {
			when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.cancel(APPOINTMENT_ID))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessage("Appointment not found.");
			verify(penaltyRepository, never()).save(any());
		}

		private Appointment givenScheduledAppointment(LocalDateTime dateTime) {
			Appointment appointment = new Appointment(patient, doctor, dateTime);
			ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
			when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
			return appointment;
		}

	}

	private static AppointmentRequest request(LocalDateTime dateTime) {
		return new AppointmentRequest(PATIENT_ID, DOCTOR_ID, dateTime);
	}

	private static Patient patient(LocalDate birthDate) {
		Patient result = new Patient("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", birthDate);
		ReflectionTestUtils.setField(result, "id", PATIENT_ID);
		return result;
	}

	private void givenPatientAndDoctorExist() {
		when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
		when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
	}

	private void givenPenalties(long count) {
		when(penaltyRepository.countByPatientIdAndCreatedAtAfter(any(), any())).thenReturn(count);
	}

	private void givenSlotFree(LocalDateTime dateTime) {
		when(appointmentRepository.existsByDoctorIdAndDateTimeAndStatus(DOCTOR_ID, dateTime,
				AppointmentStatus.SCHEDULED)).thenReturn(false);
		when(appointmentRepository.existsByPatientIdAndDateTimeAndStatus(PATIENT_ID, dateTime,
				AppointmentStatus.SCHEDULED)).thenReturn(false);
	}

	private void givenSaveReturnsArgument() {
		when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

}
