package com.ceiba.prueba.dto.request;

/**
 * Validation patterns shared by request DTOs.
 */
public final class ValidationPatterns {

	/** At least 7 digits; only digits, spaces, '+', '-', '(' and ')' are allowed (e.g. "555-1001"). */
	public static final String PHONE = "^(?=(?:\\D*\\d){7,})[\\d\\s+()-]+$";

	public static final String PHONE_MESSAGE = "must contain at least 7 digits";

	private ValidationPatterns() {
	}

}
