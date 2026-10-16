package com.ceiba.prueba.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ceiba.prueba.dto.request.PatientRequest;
import com.ceiba.prueba.dto.response.PatientResponse;
import com.ceiba.prueba.service.PatientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

	private final PatientService patientService;

	/** RF-02: registers a patient. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PatientResponse create(@Valid @RequestBody PatientRequest request) {
		return patientService.create(request);
	}

}
