package pl.travelapi.trip.api;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.travelapi.routing.domain.GeoPoint;

public record GeometryPointResponse(

		@Schema(description = "Szerokość geograficzna w stopniach (WGS 84).", example = "50.06")
		double latitude,

		@Schema(description = "Długość geograficzna w stopniach (WGS 84), zwykle od -180 do 180. Na odcinku "
				+ "samolotowym przez południk 180° wartości są ciągłe, więc mogą wyjść poza ten zakres "
				+ "(np. 181.5 oznacza -178.5).", example = "19.94")
		double longitude) {

	public static GeometryPointResponse from(GeoPoint point) {
		return new GeometryPointResponse(point.latitude(), point.longitude());
	}
}
