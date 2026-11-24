CREATE TABLE trips (
    id                BIGSERIAL     PRIMARY KEY,
    user_id           BIGINT        NOT NULL,
    title             VARCHAR(100)  NOT NULL,
    description       VARCHAR(1000),
    start_date        DATE          NOT NULL,
    end_date          DATE,
    total_distance_km NUMERIC(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_trips_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_trips_total_distance CHECK (total_distance_km >= 0)
);

CREATE INDEX idx_trips_user_start_date ON trips (user_id, start_date);

CREATE TABLE waypoints (
    id           BIGSERIAL         PRIMARY KEY,
    trip_id      BIGINT            NOT NULL,
    stop_order   INTEGER           NOT NULL,
    place_name   VARCHAR(200)      NOT NULL,
    latitude     DOUBLE PRECISION  NOT NULL,
    longitude    DOUBLE PRECISION  NOT NULL,
    country_code VARCHAR(2)        NOT NULL,
    country_name VARCHAR(100)      NOT NULL,
    CONSTRAINT fk_waypoints_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT uq_waypoints_trip_stop_order UNIQUE (trip_id, stop_order),
    CONSTRAINT ck_waypoints_stop_order CHECK (stop_order >= 0),
    CONSTRAINT ck_waypoints_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_waypoints_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_waypoints_country_code CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE TABLE trip_segments (
    id            BIGSERIAL     PRIMARY KEY,
    trip_id       BIGINT        NOT NULL,
    segment_order INTEGER       NOT NULL,
    transport_mode VARCHAR(10)  NOT NULL,
    distance_km   NUMERIC(10,2) NOT NULL DEFAULT 0,
    geometry_json JSONB,
    CONSTRAINT fk_trip_segments_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT uq_trip_segments_trip_segment_order UNIQUE (trip_id, segment_order),
    CONSTRAINT ck_trip_segments_segment_order CHECK (segment_order >= 0),
    CONSTRAINT ck_trip_segments_transport_mode CHECK (transport_mode IN ('CAR', 'PLANE', 'TRAIN', 'OTHER')),
    CONSTRAINT ck_trip_segments_distance CHECK (distance_km >= 0)
);
