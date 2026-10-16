package com.ceiba.prueba.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class AppointmentRequestValidationTest {

	private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 10, 19, 10, 0);

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void setUp() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		factory.close();
	}

	@Test
	void validate_completeRequest_hasNoViolations() {
		assertThat(validator.validate(new AppointmentRequest(1L, 1L, DATE_TIME))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "patientId", "doctorId", "dateTime" })
	void validate_requiredFieldNull_isInvalid(String property) {
		AppointmentRequest request = new AppointmentRequest(
				"patientId".equals(property) ? null : 1L,
				"doctorId".equals(property) ? null : 1L,
				"dateTime".equals(property) ? null : DATE_TIME);

		assertThat(validator.validate(request))
				.extracting(ConstraintViolation::getPropertyPath)
				.extracting(Object::toString)
				.containsExactly(property);
	}

}
