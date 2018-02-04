package io.provenance.exception;

public class SetupException extends Exception {

	private static final long serialVersionUID = 2L;

	public SetupException() {
    }

    public SetupException(String message) {
       super(message);
    }
}