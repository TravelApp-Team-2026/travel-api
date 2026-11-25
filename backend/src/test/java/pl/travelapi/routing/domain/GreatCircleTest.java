package pl.travelapi.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class GreatCircleTest {

	private static final GeoPoint WARSAW = new GeoPoint(52.2297, 21.0122);
	private static final GeoPoint KRAKOW = new GeoPoint(50.0647, 19.9450);
	private static final GeoPoint LONDON = new GeoPoint(51.5074, -0.1278);
	private static final GeoPoint PARIS = new GeoPoint(48.8566, 2.3522);
	private static final GeoPoint NEW_YORK = new GeoPoint(40.7128, -74.0060);
	private static final GeoPoint TOKYO = new GeoPoint(35.6762, 139.6503);
	private static final GeoPoint LOS_ANGELES = new GeoPoint(34.0522, -118.2437);

	@Test
	void shouldCalculateKnownDistances() {
		assertThat(GreatCircle.distanceKm(WARSAW, KRAKOW)).isCloseTo(252, within(1.0));
		assertThat(GreatCircle.distanceKm(LONDON, PARIS)).isCloseTo(344, within(1.0));
		assertThat(GreatCircle.distanceKm(WARSAW, NEW_YORK)).isCloseTo(6856, within(5.0));
	}

	@Test
	void shouldReturnZeroForSamePoint() {
		assertThat(GreatCircle.distanceKm(KRAKOW, KRAKOW)).isZero();
	}

	@Test
	void shouldNotDependOnDirection() {
		assertThat(GreatCircle.distanceKm(LONDON, PARIS)).isEqualTo(GreatCircle.distanceKm(PARIS, LONDON));
	}

	@Test
	void shouldTakeShortWayAcrossAntimeridian() {
		double twoDegreesOfEquator = 2 * Math.PI * GreatCircle.EARTH_RADIUS_KM / 180;

		assertThat(GreatCircle.distanceKm(new GeoPoint(0, 179), new GeoPoint(0, -179)))
			.isCloseTo(twoDegreesOfEquator, within(0.001));
	}

	@Test
	void shouldCalculateDistanceBetweenPoles() {
		assertThat(GreatCircle.distanceKm(new GeoPoint(90, 0), new GeoPoint(-90, 0)))
			.isCloseTo(Math.PI * GreatCircle.EARTH_RADIUS_KM, within(0.001));
	}

	@Test
	void shouldStartAndEndArcAtGivenPoints() {
		List<GeoPoint> arc = GreatCircle.arc(WARSAW, NEW_YORK, 10);

		assertThat(arc).hasSize(11);
		assertThat(arc.getFirst()).isEqualTo(WARSAW);
		assertThat(arc.getLast().latitude()).isEqualTo(NEW_YORK.latitude());
		assertThat(arc.getLast().longitude()).isCloseTo(NEW_YORK.longitude(), within(1e-9));
	}

	@Test
	void shouldBendArcTowardsPoleLikeRealFlightPath() {
		List<GeoPoint> arc = GreatCircle.arc(WARSAW, NEW_YORK, 10);

		double highestLatitude = arc.stream().mapToDouble(GeoPoint::latitude).max().orElseThrow();
		assertThat(highestLatitude).isGreaterThan(WARSAW.latitude() + 5);
	}

	@Test
	void shouldPlaceArcPointsEvenlyAlongGreatCircle() {
		List<GeoPoint> arc = GreatCircle.arc(WARSAW, NEW_YORK, 10);
		double total = GreatCircle.distanceKm(WARSAW, NEW_YORK);

		for (int i = 1; i < arc.size(); i++) {
			assertThat(GreatCircle.distanceKm(arc.get(i - 1), arc.get(i))).isCloseTo(total / 10, within(0.01));
		}
	}

	@Test
	void shouldKeepLongitudesContinuousAcrossAntimeridian() {
		List<GeoPoint> arc = GreatCircle.arc(TOKYO, LOS_ANGELES, 20);

		for (int i = 1; i < arc.size(); i++) {
			assertThat(arc.get(i).longitude()).isGreaterThan(arc.get(i - 1).longitude());
			assertThat(arc.get(i).longitude() - arc.get(i - 1).longitude()).isLessThan(180);
		}
		assertThat(arc.getLast().longitude()).isCloseTo(LOS_ANGELES.longitude() + 360, within(1e-9));
	}

	@Test
	void shouldReturnBothPointsWhenArcIsNotDefined() {
		GeoPoint northPole = new GeoPoint(90, 0);
		GeoPoint southPole = new GeoPoint(-90, 0);

		assertThat(GreatCircle.arc(KRAKOW, KRAKOW, 10)).containsExactly(KRAKOW, KRAKOW);
		assertThat(GreatCircle.arc(northPole, southPole, 10)).containsExactly(northPole, southPole);
	}

	@Test
	void shouldRejectArcWithoutSegments() {
		assertThatThrownBy(() -> GreatCircle.arc(WARSAW, KRAKOW, 0))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
