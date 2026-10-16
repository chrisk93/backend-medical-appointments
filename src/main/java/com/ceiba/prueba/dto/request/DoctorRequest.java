package com.ceiba.prueba.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input for registering a doctor (RF-01).
 */
public record DoctorRequest(

		@NotBlank
		@Size(min = 3, max = 100)
		String fullName,

		@NotBlank
		@Size(max = 255)
		String specialty,

		@Size(max = 255)
		@Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
		String phone,

		@Size(max = 255)
		@Email
		String email) {
}
