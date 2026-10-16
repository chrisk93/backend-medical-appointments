package com.ceiba.prueba.service;

import com.ceiba.prueba.dto.request.DoctorRequest;
import com.ceiba.prueba.dto.response.DoctorResponse;

public interface DoctorService {

	/** Registers a new doctor (RF-01). */
	DoctorResponse create(DoctorRequest request);

}
