package com.ceiba.prueba.exception;

/**
 * Thrown when an operation conflicts with the current state of the data (409).
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
