package com.ceiba.prueba.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.dto.request.DoctorRequest;
import com.ceiba.prueba.dto.response.DoctorResponse;
import com.ceiba.prueba.mapper.DoctorMapper;
import com.ceiba.prueba.repository.DoctorRepository;
import com.ceiba.prueba.service.DoctorService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

	private final DoctorRepository doctorRepository;

	private final DoctorMapper doctorMapper;

	@Override
	@Transactional
	public DoctorResponse create(DoctorRequest request) {
		Doctor saved = doctorRepository.save(doctorMapper.toEntity(request));
		return doctorMapper.toResponse(saved);
	}

}
