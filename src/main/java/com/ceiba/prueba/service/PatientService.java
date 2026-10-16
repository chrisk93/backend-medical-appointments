package com.ceiba.prueba.service;

import com.ceiba.prueba.dto.request.PatientRequest;
import com.ceiba.prueba.dto.response.PatientResponse;

public interface PatientService {

	/** Registers a new patient (RF-02). Fails with a conflict if the document number is already registered. */
	PatientResponse create(PatientRequest request);

}
