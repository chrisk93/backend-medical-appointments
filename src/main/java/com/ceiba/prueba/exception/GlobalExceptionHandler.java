package com.ceiba.prueba.exception;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Translates exceptions into RFC 9457 {@link ProblemDetail} responses.
 * <p>
 * Extends {@link ResponseEntityExceptionHandler} so Spring MVC errors (malformed JSON, type mismatch,
 * missing parameter, unsupported method...) keep their proper 4xx status instead of falling into the 500 handler.
 * Rejected values are never echoed back, to avoid exposing sensitive data.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String ERRORS_PROPERTY = "errors";

	private static final String INVALID_VALUE_MESSAGE = "Invalid value.";

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ProblemDetail handleConflict(ConflictException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameters.");
		problem.setProperty(ERRORS_PROPERTY, ex.getConstraintViolations().stream()
				.map(violation -> fieldError(violation.getPropertyPath().toString(), violation.getMessage()))
				.toList());
		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		// The driver message may contain the offending values, so it is not logged nor returned.
		log.warn("Data integrity violation: {}", ex.getClass().getSimpleName());
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The operation conflicts with existing data.");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred.");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ex.getBody();
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
				// A binding failure (e.g. "FOO" for an enum) carries internal type names and the rejected value.
				.map(error -> fieldError(error.getField(),
						error.isBindingFailure() ? INVALID_VALUE_MESSAGE : error.getDefaultMessage()))
				.toList();
		problem.setProperty(ERRORS_PROPERTY, errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	/** Path variables and query parameters that cannot be converted; the rejected value is not echoed back. */
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String detail = ex.getPropertyName() != null
				? "Invalid value for parameter '" + ex.getPropertyName() + "'."
				: INVALID_VALUE_MESSAGE;
		return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, detail), headers, status,
				request);
	}

	private static Map<String, String> fieldError(String field, String message) {
		return Map.of("field", field, "message", Objects.requireNonNullElse(message, INVALID_VALUE_MESSAGE));
	}

}
