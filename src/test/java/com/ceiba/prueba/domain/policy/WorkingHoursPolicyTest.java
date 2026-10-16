package com.ceiba.prueba.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WorkingHoursPolicyTest {

	// Week of reference: Monday 2026-10-19 to Sunday 2026-10-25.
	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
	private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 23);
	private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 24);
	private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 25);

	private final WorkingHoursPolicy policy = new WorkingHoursPolicy();

	@ParameterizedTest
	@ValueSource(strings = { "2026-10-19", "2026-10-20", "2026-10-21", "2026-10-22", "2026-10-23" })
	void generateSlots_weekday_returns20SlotsFrom0800To1730(String date) {
		List<LocalDateTime> slots = policy.generateSlots(LocalDate.parse(date));

		assertThat(slots).hasSize(20);
		assertThat(slots.get(0).toLocalTime()).hasToString("08:00");
		assertThat(slots.get(1).toLocalTime()).hasToString("08:30");
		assertThat(slots.get(19).toLocalTime()).hasToString("17:30");
	}

	@Test
	void generateSlots_weekday_slotsAreConsecutiveAndOnTheSameDay() {
		List<LocalDateTime> slots = policy.generateSlots(MONDAY);

		for (int i = 0; i < slots.size(); i++) {
			assertThat(slots.get(i)).isEqualTo(MONDAY.atTime(8, 0).plusMinutes(30L * i));
		}
	}

	@Test
	void generateSlots_saturday_returns10SlotsFrom0800To1230() {
		List<LocalDateTime> slots = policy.generateSlots(SATURDAY);

		assertThat(slots).hasSize(10);
		assertThat(slots.get(0)).isEqualTo(SATURDAY.atTime(8, 0));
		assertThat(slots.get(9)).isEqualTo(SATURDAY.atTime(12, 30));
	}

	@Test
	void generateSlots_sunday_returnsNoSlots() {
		assertThat(policy.generateSlots(SUNDAY)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "08:00", "08:30", "12:00", "17:00", "17:30" })
	void isValidSlotStart_weekdayWithinHours_isTrue(String time) {
		assertThat(policy.isValidSlotStart(FRIDAY.atTime(LocalTime.parse(time)))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "07:30", "07:59", "18:00", "18:30", "08:15", "08:01", "17:45", "00:00" })
	void isValidSlotStart_weekdayOutsideHoursOrMisaligned_isFalse(String time) {
		assertThat(policy.isValidSlotStart(MONDAY.atTime(LocalTime.parse(time)))).isFalse();
	}

	@Test
	void isValidSlotStart_withSecondsOrNanos_isFalse() {
		assertThat(policy.isValidSlotStart(MONDAY.atTime(8, 0, 30))).isFalse();
		assertThat(policy.isValidSlotStart(MONDAY.atTime(8, 0, 0, 1))).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "08:00", "12:00", "12:30" })
	void isValidSlotStart_saturdayWithinHours_isTrue(String time) {
		assertThat(policy.isValidSlotStart(SATURDAY.atTime(LocalTime.parse(time)))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "07:30", "13:00", "13:30", "17:30" })
	void isValidSlotStart_saturdayOutsideHours_isFalse(String time) {
		assertThat(policy.isValidSlotStart(SATURDAY.atTime(LocalTime.parse(time)))).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "08:00", "10:30", "12:00" })
	void isValidSlotStart_sunday_isFalse(String time) {
		assertThat(policy.isValidSlotStart(SUNDAY.atTime(LocalTime.parse(time)))).isFalse();
	}

	@Test
	void isValidSlotStart_everyGeneratedSlot_isValid() {
		for (LocalDate date = MONDAY; !date.isAfter(SUNDAY); date = date.plusDays(1)) {
			assertThat(policy.generateSlots(date)).allMatch(policy::isValidSlotStart);
		}
	}

}
