package com.ceiba.prueba.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PenaltyPolicyTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 8, 0);

	private final PenaltyPolicy policy = new PenaltyPolicy();

	@Test
	void isLateCancellation_exactly2HoursBefore_isNotLate() {
		assertThat(policy.isLateCancellation(NOW.plusHours(2), NOW)).isFalse();
	}

	@Test
	void isLateCancellation_1Hour59MinutesBefore_isLate() {
		assertThat(policy.isLateCancellation(NOW.plusHours(1).plusMinutes(59), NOW)).isTrue();
	}

	@Test
	void isLateCancellation_oneNanosecondUnder2Hours_isLate() {
		assertThat(policy.isLateCancellation(NOW.plusHours(2).minusNanos(1), NOW)).isTrue();
	}

	@Test
	void isLateCancellation_2Hours1MinuteBefore_isNotLate() {
		assertThat(policy.isLateCancellation(NOW.plusHours(2).plusMinutes(1), NOW)).isFalse();
	}

	@Test
	void isLateCancellation_daysBefore_isNotLate() {
		assertThat(policy.isLateCancellation(NOW.plusDays(3), NOW)).isFalse();
	}

	@Test
	void isLateCancellation_atAppointmentTime_isLate() {
		assertThat(policy.isLateCancellation(NOW, NOW)).isTrue();
	}

	@Test
	void isLateCancellation_appointmentAlreadyPassed_isLate() {
		assertThat(policy.isLateCancellation(NOW.minusDays(1), NOW)).isTrue();
	}

	@Test
	void penaltyWindowStart_is30DaysBeforeNow() {
		assertThat(policy.penaltyWindowStart(NOW)).isEqualTo(LocalDateTime.of(2026, 9, 19, 8, 0));
	}

	@ParameterizedTest
	@CsvSource({ "0, false", "1, false", "2, false", "3, true", "4, true" })
	void isBlocked_penaltyCount_blocksFrom3(long penaltyCount, boolean blocked) {
		assertThat(policy.isBlocked(penaltyCount)).isEqualTo(blocked);
	}

}
