package pl.travelapi.trip.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "trip_segments")
public class TripSegment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	@Column(name = "segment_order", nullable = false)
	private int segmentOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "transport_mode", nullable = false, length = 10)
	private TransportMode transportMode;

	@Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
	private BigDecimal distanceKm;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "geometry_json")
	private String geometryJson;

	protected TripSegment() {
	}

	TripSegment(Trip trip, int segmentOrder, TransportMode transportMode, BigDecimal distanceKm,
			String geometryJson) {
		this.trip = trip;
		this.segmentOrder = segmentOrder;
		this.transportMode = transportMode;
		this.distanceKm = distanceKm;
		this.geometryJson = geometryJson;
	}

	public Long getId() {
		return id;
	}

	public int getSegmentOrder() {
		return segmentOrder;
	}

	public TransportMode getTransportMode() {
		return transportMode;
	}

	public BigDecimal getDistanceKm() {
		return distanceKm;
	}

	public String getGeometryJson() {
		return geometryJson;
	}
}
