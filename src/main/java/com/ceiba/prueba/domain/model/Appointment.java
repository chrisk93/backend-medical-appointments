package com.ceiba.prueba.domain.model;

import java.time.LocalDateTime;

import com.ceiba.prueba.exception.ConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "appointment", indexes = {
		@Index(name = "idx_appointment_doctor_date_time", columnList = "doctor_id, date_time"),
		@Index(name = "idx_appointment_patient_date_time", columnList = "patient_id, date_time") })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "doctor_id", nullable = false)
	private Doctor doctor;

	@Column(nullable = false)
	private LocalDateTime dateTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AppointmentStatus status;

	private LocalDateTime cancelledAt;

	/** Creates an appointment in {@link AppointmentStatus#SCHEDULED} status (RF-03). */
	public Appointment(Patient patient, Doctor doctor, LocalDateTime dateTime) {
		this.patient = patient;
		this.doctor = doctor;
		this.dateTime = dateTime;
		this.status = AppointmentStatus.SCHEDULED;
	}

	/**
	 * Cancels the appointment and records when it happened (RF-05).
	 *
	 * @throws ConflictException if the appointment is not {@link AppointmentStatus#SCHEDULED}
	 */
	public void cancel(LocalDateTime now) {
		if (status != AppointmentStatus.SCHEDULED) {
			throw new ConflictException("Only scheduled appointments can be cancelled.");
		}
		this.status = AppointmentStatus.CANCELLED;
		this.cancelledAt = now;
	}

}
