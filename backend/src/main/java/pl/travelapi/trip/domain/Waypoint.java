package pl.travelapi.trip.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "waypoints")
public class Waypoint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	@Column(name = "stop_order", nullable = false)
	private int stopOrder;

	@Column(name = "place_name", nullable = false, length = 200)
	private String placeName;

	@Column(name = "latitude", nullable = false)
	private double latitude;

	@Column(name = "longitude", nullable = false)
	private double longitude;

	@Column(name = "country_code", nullable = false, length = 2)
	private String countryCode;

	@Column(name = "country_name", nullable = false, length = 100)
	private String countryName;

	protected Waypoint() {
	}

	Waypoint(Trip trip, int stopOrder, String placeName, double latitude, double longitude,
			String countryCode, String countryName) {
		this.trip = trip;
		this.stopOrder = stopOrder;
		this.placeName = placeName;
		this.latitude = latitude;
		this.longitude = longitude;
		this.countryCode = countryCode;
		this.countryName = countryName;
	}

	public Long getId() {
		return id;
	}

	public int getStopOrder() {
		return stopOrder;
	}

	public String getPlaceName() {
		return placeName;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public String getCountryName() {
		return countryName;
	}
}
