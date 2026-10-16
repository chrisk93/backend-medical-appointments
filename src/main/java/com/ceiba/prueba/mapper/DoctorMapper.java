package com.ceiba.prueba.mapper;

import org.springframework.stereotype.Component;

import com.ceiba.prueba.domain.model.Doctor;
import com.ceiba.prueba.dto.request.DoctorRequest;
import com.ceiba.prueba.dto.response.DoctorResponse;

@Component
public class DoctorMapper {

	public Doctor toEntity(DoctorRequest request) {
		return new Doctor(request.fullName(), request.specialty(), request.phone(), request.email());
	}

	public DoctorResponse toResponse(Doctor doctor) {
		return new DoctorResponse(doctor.getId(), doctor.getFullName(), doctor.getSpecialty(), doctor.getPhone(),
				doctor.getEmail());
	}

}
