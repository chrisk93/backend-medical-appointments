package com.ceiba.prueba.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceiba.prueba.domain.model.Patient;
import com.ceiba.prueba.dto.request.PatientRequest;
import com.ceiba.prueba.dto.response.PatientResponse;
import com.ceiba.prueba.exception.ConflictException;
import com.ceiba.prueba.mapper.PatientMapper;
import com.ceiba.prueba.repository.PatientRepository;
import com.ceiba.prueba.service.PatientService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

	private final PatientRepository patientRepository;

	private final PatientMapper patientMapper;

	@Override
	@Transactional
	public PatientResponse create(PatientRequest request) {
		// The document number is not echoed in the message: it is sensitive data.
		if (patientRepository.existsByDocumentNumber(request.documentNumber())) {
			throw new ConflictException("A patient with this document number is already registered.");
		}
		Patient saved = patientRepository.save(patientMapper.toEntity(request));
		return patientMapper.toResponse(saved);
	}

}
