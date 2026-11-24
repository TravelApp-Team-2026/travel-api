package pl.travelapi.shared.exception;

/** A single request field is invalid (rule checked outside bean validation). Mapped to HTTP 400 with {@code fieldErrors}. */
public class InvalidRequestException extends RuntimeException {

	private final String field;

	public InvalidRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
