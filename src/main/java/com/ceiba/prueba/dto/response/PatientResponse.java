package com.ceiba.prueba.dto.response;

import java.time.LocalDate;

public record PatientResponse(Long id, String fullName, String documentNumber, String phone, String email,
		LocalDate birthDate) {
}
