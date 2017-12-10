package io.provenance.exception;

public class ConfigParseException extends Exception {

	private static final long serialVersionUID = 1L;

	public ConfigParseException() {
    }

    public ConfigParseException(String message) {
       super(message);
    }
}
