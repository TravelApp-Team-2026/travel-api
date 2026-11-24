package pl.travelapi.shared.exception;

/** An external service is unavailable or too busy. Mapped to HTTP 503. */
public class ServiceUnavailableException extends RuntimeException {

	public ServiceUnavailableException(String message) {
		super(message);
	}
}
