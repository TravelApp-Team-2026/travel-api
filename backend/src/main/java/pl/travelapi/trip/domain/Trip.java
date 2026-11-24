package pl.travelapi.trip.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import pl.travelapi.user.domain.User;

@Entity
@Table(name = "trips")
public class Trip {

	private static final BigDecimal NO_DISTANCE = BigDecimal.ZERO.setScale(2);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "total_distance_km", nullable = false, precision = 10, scale = 2)
	private BigDecimal totalDistanceKm = NO_DISTANCE;

	@OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("stopOrder")
	private List<Waypoint> waypoints = new ArrayList<>();

	@OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("segmentOrder")
	private List<TripSegment> segments = new ArrayList<>();

	protected Trip() {
	}

	private Trip(User user, String title, String description, LocalDate startDate, LocalDate endDate) {
		this.user = user;
		this.title = title;
		this.description = description;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public static Trip create(User user, String title, String description, LocalDate startDate, LocalDate endDate) {
		return new Trip(user, title, description, startDate, endDate);
	}

	public void addWaypoint(int stopOrder, String placeName, double latitude, double longitude,
			String countryCode, String countryName) {
		waypoints.add(new Waypoint(this, stopOrder, placeName, latitude, longitude, countryCode, countryName));
	}

	/** The segment with order {@code i} connects the waypoint with order {@code i} to the one with order {@code i + 1}. */
	public void addSegment(int segmentOrder, TransportMode transportMode, BigDecimal distanceKm, String geometryJson) {
		segments.add(new TripSegment(this, segmentOrder, transportMode, distanceKm, geometryJson));
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public BigDecimal getTotalDistanceKm() {
		return totalDistanceKm;
	}

	public List<Waypoint> getWaypoints() {
		return Collections.unmodifiableList(waypoints);
	}

	public List<TripSegment> getSegments() {
		return Collections.unmodifiableList(segments);
	}
}
