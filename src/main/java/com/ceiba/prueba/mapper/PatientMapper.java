package com.ceiba.prueba.mapper;

import org.springframework.stereotype.Component;

import com.ceiba.prueba.domain.model.Patient;
import com.ceiba.prueba.dto.request.PatientRequest;
import com.ceiba.prueba.dto.response.PatientResponse;

@Component
public class PatientMapper {

	public Patient toEntity(PatientRequest request) {
		return new Patient(request.fullName(), request.documentNumber(), request.phone(), request.email(),
				request.birthDate());
	}

	public PatientResponse toResponse(Patient patient) {
		return new PatientResponse(patient.getId(), patient.getFullName(), patient.getDocumentNumber(),
				patient.getPhone(), patient.getEmail(), patient.getBirthDate());
	}

}
