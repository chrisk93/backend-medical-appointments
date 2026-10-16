package com.ceiba.prueba.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ceiba.prueba.domain.model.Appointment;
import com.ceiba.prueba.domain.model.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

	/** RN-02: does the doctor already have an appointment in this slot? */
	boolean existsByDoctorIdAndDateTimeAndStatus(Long doctorId, LocalDateTime dateTime, AppointmentStatus status);

	/** RN-04: does the patient already have an appointment in this slot (with any doctor)? */
	boolean existsByPatientIdAndDateTimeAndStatus(Long patientId, LocalDateTime dateTime, AppointmentStatus status);

	/** RF-04: start times of the doctor's appointments in {@code [from, to)}. */
	@Query("""
			select a.dateTime from Appointment a
			where a.doctor.id = :doctorId and a.status = :status
			  and a.dateTime >= :from and a.dateTime < :to
			""")
	List<LocalDateTime> findDateTimesByDoctorIdAndStatusInRange(@Param("doctorId") Long doctorId,
			@Param("status") AppointmentStatus status, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

}
