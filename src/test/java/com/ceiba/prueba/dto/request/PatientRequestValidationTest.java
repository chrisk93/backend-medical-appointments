package com.ceiba.prueba.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class PatientRequestValidationTest {

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
		assertThat(validator.validate(request(LocalDate.of(1990, 5, 20)))).isEmpty();
	}

	@Test
	void validate_withoutBirthDate_hasNoViolations() {
		assertThat(validator.validate(request(null))).isEmpty();
	}

	@ParameterizedTest
	@MethodSource("fullNameLengths")
	void validate_fullNameLength_respectsLimits(int length, boolean valid) {
		PatientRequest request = new PatientRequest("a".repeat(length), "1020304050", "3001234567",
				"ana.torres@mail.com", null);

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
	@MethodSource("documentNumberLengths")
	void validate_documentNumberLength_requiresAtLeast7Characters(String documentNumber, boolean valid) {
		PatientRequest request = new PatientRequest("Ana Torres", documentNumber, "3001234567",
				"ana.torres@mail.com", null);

		assertValidity(request, "documentNumber", valid);
	}

	static Stream<Arguments> documentNumberLengths() {
		return Stream.of(
				Arguments.of("123456", false),
				Arguments.of("1234567", true),
				Arguments.of("AB-1234567", true));
	}

	@ParameterizedTest
	@ValueSource(strings = { "fullName", "documentNumber", "phone", "email" })
	void validate_requiredFieldNull_isInvalid(String property) {
		PatientRequest request = withProperty(property, null);

		assertValidity(request, property, false);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void validate_documentNumberMissing_isInvalid(String documentNumber) {
		PatientRequest request = new PatientRequest("Ana Torres", documentNumber, "3001234567",
				"ana.torres@mail.com", null);

		assertValidity(request, "documentNumber", false);
	}

	@ParameterizedTest
	@ValueSource(strings = { "123456", "", "abc1234567" })
	void validate_invalidPhone_isInvalid(String phone) {
		PatientRequest request = new PatientRequest("Ana Torres", "1020304050", phone, "ana.torres@mail.com",
				null);

		assertValidity(request, "phone", false);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "not-an-email", "ana.torres@" })
	void validate_invalidEmail_isInvalid(String email) {
		PatientRequest request = new PatientRequest("Ana Torres", "1020304050", "3001234567", email, null);

		assertValidity(request, "email", false);
	}

	@Test
	void validate_birthDateToday_isValid() {
		assertValidity(request(LocalDate.now()), "birthDate", true);
	}

	@Test
	void validate_birthDateTomorrow_isInvalid() {
		assertValidity(request(LocalDate.now().plusDays(1)), "birthDate", false);
	}

	private static PatientRequest request(LocalDate birthDate) {
		return new PatientRequest("Ana Torres", "1020304050", "3001234567", "ana.torres@mail.com", birthDate);
	}

	private static PatientRequest withProperty(String property, String value) {
		return new PatientRequest(
				"fullName".equals(property) ? value : "Ana Torres",
				"documentNumber".equals(property) ? value : "1020304050",
				"phone".equals(property) ? value : "3001234567",
				"email".equals(property) ? value : "ana.torres@mail.com",
				null);
	}

	private static void assertValidity(PatientRequest request, String property, boolean valid) {
		Set<ConstraintViolation<PatientRequest>> violations = validator.validateProperty(request, property);
		if (valid) {
			assertThat(violations).isEmpty();
		}
		else {
			assertThat(violations).isNotEmpty();
		}
	}

}
