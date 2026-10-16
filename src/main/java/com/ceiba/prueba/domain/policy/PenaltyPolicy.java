package com.ceiba.prueba.domain.policy;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

/**
 * Late-cancellation penalties (RN-05).
 * <ul>
 * <li>Cancelling less than 2 hours before the appointment registers a penalty (exactly 2 hours does not).</li>
 * <li>A patient with 3 or more penalties in the last 30 days cannot schedule new appointments.</li>
 * </ul>
 */
@Component
public class PenaltyPolicy {

	public static final Duration LATE_CANCELLATION_THRESHOLD = Duration.ofHours(2);

	public static final Duration PENALTY_WINDOW = Duration.ofDays(30);

	public static final long MAX_PENALTIES = 3;

	/** True if cancelling at {@code now} is late; an appointment whose time already passed is always late. */
	public boolean isLateCancellation(LocalDateTime appointmentDateTime, LocalDateTime now) {
		return Duration.between(now, appointmentDateTime).compareTo(LATE_CANCELLATION_THRESHOLD) < 0;
	}

	/** Start of the window in which penalties are counted. */
	public LocalDateTime penaltyWindowStart(LocalDateTime now) {
		return now.minus(PENALTY_WINDOW);
	}

	/** True if the number of penalties within the window blocks new appointments. */
	public boolean isBlocked(long penaltyCount) {
		return penaltyCount >= MAX_PENALTIES;
	}

}
