package com.ceiba.prueba.repository.specification;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.ceiba.prueba.domain.model.Appointment;
import com.ceiba.prueba.domain.model.AppointmentStatus;

/**
 * Optional filters of RF-06. Each filter is unrestricted when its value is {@code null}, so they can always be
 * combined with {@link Specification#allOf}. Values are bound as query parameters (no SQL concatenation).
 */
public final class AppointmentSpecifications {

	private static final String DOCTOR = "doctor";
	private static final String PATIENT = "patient";
	private static final String ID = "id";
	private static final String STATUS = "status";
	private static final String DATE_TIME = "dateTime";

	private AppointmentSpecifications() {
	}

	/** All RF-06 filters combined; {@code null} values are ignored. */
	public static Specification<Appointment> matching(Long doctorId, Long patientId, AppointmentStatus status,
			LocalDate startDate, LocalDate endDate) {
		return Specification.allOf(hasDoctorId(doctorId), hasPatientId(patientId), hasStatus(status),
				onOrAfter(startDate), onOrBefore(endDate));
	}

	public static Specification<Appointment> hasDoctorId(Long doctorId) {
		if (doctorId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get(DOCTOR).get(ID), doctorId);
	}

	public static Specification<Appointment> hasPatientId(Long patientId) {
		if (patientId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get(PATIENT).get(ID), patientId);
	}

	public static Specification<Appointment> hasStatus(AppointmentStatus status) {
		if (status == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get(STATUS), status);
	}

	/** Appointments on or after {@code startDate} (inclusive, from 00:00). */
	public static Specification<Appointment> onOrAfter(LocalDate startDate) {
		if (startDate == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(DATE_TIME), startDate.atStartOfDay());
	}

	/** Appointments on or before {@code endDate} (inclusive: the whole day is included). */
	public static Specification<Appointment> onOrBefore(LocalDate endDate) {
		if (endDate == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.lessThan(root.get(DATE_TIME), endDate.plusDays(1).atStartOfDay());
	}

}
