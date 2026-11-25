package pl.travelapi.trip.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import pl.travelapi.trip.domain.GeometryJson;
import pl.travelapi.trip.domain.TransportMode;
import pl.travelapi.trip.domain.TripSegment;

public record SegmentResponse(
		int order,
		TransportMode transportMode,

		@Schema(description = "Długość odcinka w km, 2 miejsca po przecinku. CAR: po drogach, a gdy nie da się "
				+ "wyznaczyć trasy, w linii prostej. PLANE, TRAIN, OTHER: po kole wielkim.", example = "293.41")
		BigDecimal distanceKm,

		@Schema(description = "Linia odcinka: punkty w kolejności podróży, gotowe do narysowania. CAR: uproszczona "
				+ "trasa po drogach (jej końce mogą nie dotykać dokładnie przystanków) albo 2 punkty, gdy nie "
				+ "da się wyznaczyć trasy. PLANE: łuk (do 65 punktów). TRAIN i OTHER: 2 punkty. Pusta lista "
				+ "tylko dla podróży zapisanej przed wprowadzeniem tras; ponowny zapis (PUT) ją uzupełnia.")
		List<GeometryPointResponse> geometry) {

	public static SegmentResponse from(TripSegment segment) {
		return new SegmentResponse(segment.getSegmentOrder(), segment.getTransportMode(), segment.getDistanceKm(),
				GeometryJson.read(segment.getGeometryJson()).stream().map(GeometryPointResponse::from).toList());
	}
}
