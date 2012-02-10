package org.deuce.optimize.utils;

public class OptimizerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OptimizerException() {
		super();
	}

	public OptimizerException(String message) {
		super(message);
	}

	public OptimizerException(String format, Object... args) {
		super(String.format(format, args));
	}

	public OptimizerException(Throwable cause) {
		super(cause);
	}

	public OptimizerException(String message, Throwable cause) {
		super(message, cause);
	}

}
