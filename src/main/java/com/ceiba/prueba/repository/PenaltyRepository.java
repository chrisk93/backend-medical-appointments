package com.ceiba.prueba.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceiba.prueba.domain.model.Penalty;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

	/** RN-05: penalties of the patient registered strictly after {@code windowStart}. */
	long countByPatientIdAndCreatedAtAfter(Long patientId, LocalDateTime windowStart);

}
