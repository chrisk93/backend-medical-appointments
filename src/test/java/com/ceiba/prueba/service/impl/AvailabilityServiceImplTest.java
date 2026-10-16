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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ceiba.prueba.domain.model.AppointmentStatus;
import com.ceiba.prueba.domain.policy.WorkingHoursPolicy;
import com.ceiba.prueba.dto.response.TimeSlotResponse;
import com.ceiba.prueba.exception.BusinessRuleException;
import com.ceiba.prueba.exception.ResourceNotFoundException;
import com.ceiba.prueba.mapper.AppointmentMapper;
import com.ceiba.prueba.repository.AppointmentRepository;
import com.ceiba.prueba.repository.DoctorRepository;

/**
 * "Now" is fixed at Monday 2026-10-19 10:15. The week of 2026-10-26 is entirely in the future.
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 10, 15);
	private static final Long DOCTOR_ID = 1L;

	private static final LocalDate TODAY_MONDAY = NOW.toLocalDate();
	private static final LocalDate NEXT_MONDAY = LocalDate.of(2026, 10, 26);
	private static final LocalDate NEXT_TUESDAY = LocalDate.of(2026, 10, 27);
	private static final LocalDate NEXT_SATURDAY = LocalDate.of(2026, 10, 31);
	private static final LocalDate NEXT_SUNDAY = LocalDate.of(2026, 11, 1);

	@Mock
	private DoctorRepository doctorRepository;

	@Mock
	private AppointmentRepository appointmentRepository;

	private AvailabilityServiceImpl service;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
		service = new AvailabilityServiceImpl(doctorRepository, appointmentRepository, new WorkingHoursPolicy(),
				new AppointmentMapper(), clock);
	}

	@Test
	void getAvailableSlots_fullFutureWeek_returns110Slots() {
		givenDoctorExists();
		givenTakenSlots();

		List<TimeSlotResponse> slots = service.getAvailableSlots(DOCTOR_ID, NEXT_MONDAY, NEXT_SUNDAY);

		// 20 slots x 5 weekdays + 10 on Saturday + 0 on Sunday
		assertThat(slots).hasSize(110);
		assertThat(slots.get(0)).isEqualTo(slot(NEXT_MONDAY.atTime(8, 0)));
		assertThat(slots.get(109)).isEqualTo(slot(NEXT_SATURDAY.atTime(12, 30)));
		assertThat(slots).allMatch(s -> s.end().equals(s.start().plusMinutes(30)));
	}

	@Test
	void getAvailableSlots_takenSlots_areExcluded() {
		givenDoctorExists();
		givenTakenSlots(NEXT_TUESDAY.atTime(10, 0), NEXT_TUESDAY.atTime(17, 30));

		List<TimeSlotResponse> slots = service.getAvailableSlots(DOCTOR_ID, NEXT_TUESDAY, NEXT_TUESDAY);

		assertThat(slots).hasSize(18)
				.doesNotContain(slot(NEXT_TUESDAY.atTime(10, 0)), slot(NEXT_TUESDAY.atTime(17, 30)))
				.contains(slot(NEXT_TUESDAY.atTime(9, 30)), slot(NEXT_TUESDAY.atTime(10, 30)));
	}

	@Test
	void getAvailableSlots_queriesOnlyScheduledAppointmentsOfWholeRange() {
		// Cancelled appointments are not queried, so they do not take their slot.
		givenDoctorExists();
		givenTakenSlots();

		service.getAvailableSlots(DOCTOR_ID, NEXT_MONDAY, NEXT_TUESDAY);

		verify(appointmentRepository).findDateTimesByDoctorIdAndStatusInRange(DOCTOR_ID,
				AppointmentStatus.SCHEDULED, NEXT_MONDAY.atStartOfDay(), LocalDate.of(2026, 10, 28).atStartOfDay());
	}

	@Test
	void getAvailableSlots_today_excludesSlotsThatAlreadyStarted() {
		givenDoctorExists();
		givenTakenSlots();

		List<TimeSlotResponse> slots = service.getAvailableSlots(DOCTOR_ID, TODAY_MONDAY, TODAY_MONDAY);

		// 08:00 to 10:00 already started at 10:15; 10:30 to 17:30 remain.
		assertThat(slots).hasSize(15);
		assertThat(slots.get(0)).isEqualTo(slot(TODAY_MONDAY.atTime(10, 30)));
	}

	@Test
	void getAvailableSlots_pastDays_returnsNoSlots() {
		givenDoctorExists();
		givenTakenSlots();

		assertThat(service.getAvailableSlots(DOCTOR_ID, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 17)))
				.isEmpty();
	}

	@Test
	void getAvailableSlots_sunday_returnsNoSlots() {
		givenDoctorExists();
		givenTakenSlots();

		assertThat(service.getAvailableSlots(DOCTOR_ID, NEXT_SUNDAY, NEXT_SUNDAY)).isEmpty();
	}

	@Test
	void getAvailableSlots_saturday_returns10Slots() {
		givenDoctorExists();
		givenTakenSlots();

		assertThat(service.getAvailableSlots(DOCTOR_ID, NEXT_SATURDAY, NEXT_SATURDAY)).hasSize(10);
	}

	@Test
	void getAvailableSlots_startDateAfterEndDate_throwsBusinessRuleWithoutQuerying() {
		givenDoctorExists();

		assertThatThrownBy(() -> service.getAvailableSlots(DOCTOR_ID, NEXT_TUESDAY, NEXT_MONDAY))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("startDate must be on or before endDate.");
		verify(appointmentRepository, never()).findDateTimesByDoctorIdAndStatusInRange(any(), any(), any(), any());
	}

	@Test
	void getAvailableSlots_doctorNotFound_throwsNotFound() {
		when(doctorRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> service.getAvailableSlots(99L, NEXT_MONDAY, NEXT_MONDAY))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Doctor not found.");
		verify(appointmentRepository, never()).findDateTimesByDoctorIdAndStatusInRange(any(), any(), any(), any());
	}

	private void givenDoctorExists() {
		when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
	}

	private void givenTakenSlots(LocalDateTime... taken) {
		when(appointmentRepository.findDateTimesByDoctorIdAndStatusInRange(any(), any(), any(), any()))
				.thenReturn(List.of(taken));
	}

	private static TimeSlotResponse slot(LocalDateTime start) {
		return new TimeSlotResponse(start, start.plusMinutes(30));
	}

}
