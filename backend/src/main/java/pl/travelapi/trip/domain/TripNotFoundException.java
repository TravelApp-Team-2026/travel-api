package pl.travelapi.trip.domain;

import pl.travelapi.shared.exception.ResourceNotFoundException;

/** The trip does not exist or belongs to another user (both cases look the same to the client). */
public class TripNotFoundException extends ResourceNotFoundException {

	public TripNotFoundException() {
		super("Nie znaleziono podróży.");
	}
}
