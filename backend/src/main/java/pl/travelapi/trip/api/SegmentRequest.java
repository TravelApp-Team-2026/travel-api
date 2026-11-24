package pl.travelapi.trip.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import pl.travelapi.trip.domain.TransportMode;

public record SegmentRequest(

		@Schema(description = "Środek transportu na odcinku: CAR, PLANE, TRAIN lub OTHER.", example = "TRAIN")
		@NotNull(message = "Wybierz środek transportu dla odcinka.")
		TransportMode transportMode) {
}
