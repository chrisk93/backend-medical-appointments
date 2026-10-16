package com.ceiba.prueba.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input for registering a patient (RF-02). {@code birthDate} is optional, ISO 8601 ({@code yyyy-MM-dd}).
 */
public record PatientRequest(

		@NotBlank
		@Size(min = 3, max = 100)
		String fullName,

		@NotBlank
		@Size(min = 7, max = 255)
		String documentNumber,

		@NotBlank
		@Size(max = 255)
		@Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
		String phone,

		@NotBlank
		@Size(max = 255)
		@Email
		String email,

		@PastOrPresent
		LocalDate birthDate) {
}
