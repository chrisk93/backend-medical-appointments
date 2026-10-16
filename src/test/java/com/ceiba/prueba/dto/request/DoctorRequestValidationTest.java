package com.ceiba.prueba.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class DoctorRequestValidationTest {

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
	void validate_seedDoctor_hasNoViolations() {
		DoctorRequest request = new DoctorRequest("Dra. María González", "Cardiología", "555-1001",
				"maria.gonzalez@medisalud.com");

		assertThat(validator.validate(request)).isEmpty();
	}

	@ParameterizedTest
	@MethodSource("fullNameLengths")
	void validate_fullNameLength_respectsLimits(int length, boolean valid) {
		DoctorRequest request = new DoctorRequest("a".repeat(length), "Pediatría", null, null);

		assertValidity(request, "fullName", valid);
	}

	static Stream<Arguments> fullNameLengths() {
		return Stream.of(
				Arguments.of(2, false),
				Arguments.of(3, true),
				Arguments.of(100, true),
				Arguments.of(101, false));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void validate_fullNameMissing_isInvalid(String fullName) {
		DoctorRequest request = new DoctorRequest(fullName, "Pediatría", null, null);

		assertValidity(request, "fullName", false);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void validate_specialtyMissing_isInvalid(String specialty) {
		DoctorRequest request = new DoctorRequest("Dr. Carlos Ruiz", specialty, null, null);

		assertValidity(request, "specialty", false);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "555-1001", "1234567", "+57 (300) 123-4567", "300 123 4567" })
	void validate_validOrMissingPhone_isValid(String phone) {
		DoctorRequest request = new DoctorRequest("Dr. Carlos Ruiz", "Pediatría", phone, null);

		assertValidity(request, "phone", true);
	}

	@ParameterizedTest
	@ValueSource(strings = { "123456", "555-100", "", "abc1234567", "555.1001.22" })
	void validate_invalidPhone_isInvalid(String phone) {
		DoctorRequest request = new DoctorRequest("Dr. Carlos Ruiz", "Pediatría", phone, null);

		assertValidity(request, "phone", false);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = "carlos.ruiz@medisalud.com")
	void validate_validOrMissingEmail_isValid(String email) {
		DoctorRequest request = new DoctorRequest("Dr. Carlos Ruiz", "Pediatría", null, email);

		assertValidity(request, "email", true);
	}

	@ParameterizedTest
	@ValueSource(strings = { "not-an-email", "carlos.ruiz@", "@medisalud.com" })
	void validate_invalidEmail_isInvalid(String email) {
		DoctorRequest request = new DoctorRequest("Dr. Carlos Ruiz", "Pediatría", null, email);

		assertValidity(request, "email", false);
	}

	private static void assertValidity(DoctorRequest request, String property, boolean valid) {
		Set<ConstraintViolation<DoctorRequest>> violations = validator.validateProperty(request, property);
		if (valid) {
			assertThat(violations).isEmpty();
		}
		else {
			assertThat(violations).isNotEmpty();
		}
	}

}
