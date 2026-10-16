package com.ceiba.prueba.domain.policy;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Working hours and 30-minute time slots (RN-01, RN-06).
 * <ul>
 * <li>Monday to Friday: 08:00 - 18:00 (20 slots)</li>
 * <li>Saturday: 08:00 - 13:00 (10 slots)</li>
 * <li>Sunday: closed</li>
 * </ul>
 * Stateless: it does not know the current time, so past slots are the caller's concern.
 */
@Component
public class WorkingHoursPolicy {

	public static final Duration SLOT_DURATION = Duration.ofMinutes(30);

	private static final LocalTime OPENING_TIME = LocalTime.of(8, 0);

	private static final LocalTime WEEKDAY_CLOSING_TIME = LocalTime.of(18, 0);

	private static final LocalTime SATURDAY_CLOSING_TIME = LocalTime.of(13, 0);

	/** Returns the start of every slot of the given day, in chronological order; empty if closed. */
	public List<LocalDateTime> generateSlots(LocalDate date) {
		List<LocalDateTime> slots = new ArrayList<>();
		closingTime(date.getDayOfWeek()).ifPresent(closing -> {
			LocalTime start = OPENING_TIME;
			while (!start.plus(SLOT_DURATION).isAfter(closing)) {
				slots.add(date.atTime(start));
				start = start.plus(SLOT_DURATION);
			}
		});
		return slots;
	}

	/** True if the date-time is exactly the start of a slot within working hours. */
	public boolean isValidSlotStart(LocalDateTime dateTime) {
		LocalTime time = dateTime.toLocalTime();
		boolean alignedToSlot = time.getMinute() % SLOT_DURATION.toMinutes() == 0
				&& time.getSecond() == 0
				&& time.getNano() == 0;
		return alignedToSlot && closingTime(dateTime.getDayOfWeek())
				.map(closing -> !time.isBefore(OPENING_TIME) && !time.plus(SLOT_DURATION).isAfter(closing))
				.orElse(false);
	}

	private static Optional<LocalTime> closingTime(DayOfWeek day) {
		return switch (day) {
			case SATURDAY -> Optional.of(SATURDAY_CLOSING_TIME);
			case SUNDAY -> Optional.empty();
			default -> Optional.of(WEEKDAY_CLOSING_TIME);
		};
	}

}
