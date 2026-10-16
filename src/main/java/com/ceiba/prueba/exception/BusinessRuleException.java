package com.ceiba.prueba.exception;

/**
 * Thrown when a request is well-formed but violates a business rule (400).
 */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}

}
