package com.ceiba.prueba.exception;

/**
 * Thrown when a requested resource does not exist (404).
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

}
