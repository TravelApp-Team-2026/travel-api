package pl.travelapi.trip.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Each violation is attached to a field ({@code endDate}, {@code segments}), so the client can show it
 * under the right input. Null and missing values are reported by the annotations on the fields.
 */
public class ValidTripRequestValidator implements ConstraintValidator<ValidTripRequest, SaveTripRequest> {

	static final String END_DATE_MESSAGE = "Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.";
	static final String SEGMENTS_MESSAGE = "Liczba odcinków musi być o 1 mniejsza niż liczba przystanków.";

	private static final int MIN_WAYPOINTS = 2;

	@Override
	public boolean isValid(SaveTripRequest request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		boolean endDateValid = isEndDateValid(request, context);
		boolean segmentsValid = isSegmentCountValid(request, context);
		return endDateValid && segmentsValid;
	}

	private boolean isEndDateValid(SaveTripRequest request, ConstraintValidatorContext context) {
		if (request.startDate() == null || request.endDate() == null) {
			return true;
		}
		if (!request.endDate().isBefore(request.startDate())) {
			return true;
		}
		addViolation(context, "endDate", END_DATE_MESSAGE);
		return false;
	}

	private boolean isSegmentCountValid(SaveTripRequest request, ConstraintValidatorContext context) {
		if (request.waypoints() == null || request.segments() == null
				|| request.waypoints().size() < MIN_WAYPOINTS) {
			return true;
		}
		if (request.segments().size() == request.waypoints().size() - 1) {
			return true;
		}
		addViolation(context, "segments", SEGMENTS_MESSAGE);
		return false;
	}

	private static void addViolation(ConstraintValidatorContext context, String field, String message) {
		context.buildConstraintViolationWithTemplate(message)
			.addPropertyNode(field)
			.addConstraintViolation();
	}
}
