package com.ceiba.prueba.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ceiba.prueba.domain.model.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

	boolean existsByDocumentNumber(String documentNumber);

}
