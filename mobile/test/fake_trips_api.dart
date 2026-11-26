import 'dart:async';

import 'package:dio/dio.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/trips_api.dart';

class FakeTripsApi extends TripsApi {
  FakeTripsApi([this.trips = const []]) : super(ApiClient(Dio()));

  List<TripSummary> trips;
  int lists = 0;
  ApiException? listError;
  TripDetails? details;
  ApiException? detailsError;
  ApiException? saveError;
  ApiException? deleteError;
  SaveTrip? lastSaved;
  int? lastSavedId;
  int? lastDeletedId;
  Duration saveDelay = Duration.zero;

  @override
  Future<List<TripSummary>> listTrips() async {
    lists++;
    if (listError != null) {
      throw listError!;
    }
    return List.of(trips);
  }

  @override
  Future<TripDetails> getTrip(int id) async {
    if (detailsError != null) {
      throw detailsError!;
    }
    final trip = details;
    if (trip == null || trip.id != id) {
      throw const ApiException('Nie znaleziono podróży.');
    }
    return trip;
  }

  @override
  Future<TripDetails> createTrip(SaveTrip trip) async {
    if (saveDelay > Duration.zero) {
      await Future<void>.delayed(saveDelay);
    }
    lastSaved = trip;
    lastSavedId = null;
    if (saveError != null) {
      throw saveError!;
    }
    return _fromDraft(99, trip);
  }

  @override
  Future<TripDetails> updateTrip(int id, SaveTrip trip) async {
    if (saveDelay > Duration.zero) {
      await Future<void>.delayed(saveDelay);
    }
    lastSaved = trip;
    lastSavedId = id;
    if (saveError != null) {
      throw saveError!;
    }
    trips = [
      for (final existing in trips)
        if (existing.id == id)
          TripSummary(
            id: id,
            title: trip.title,
            startDate: trip.startDate,
            endDate: trip.endDate,
            stopCount: trip.waypoints.length,
            totalDistanceKm: existing.totalDistanceKm,
          )
        else
          existing,
    ];
    final updated = _fromDraft(id, trip);
    details = updated;
    return updated;
  }

  @override
  Future<void> deleteTrip(int id) async {
    lastDeletedId = id;
    if (deleteError != null) {
      throw deleteError!;
    }
    trips = [
      for (final existing in trips)
        if (existing.id != id) existing,
    ];
    if (details?.id == id) {
      details = null;
    }
  }

  static TripDetails _fromDraft(int id, SaveTrip trip) {
    return TripDetails(
      id: id,
      title: trip.title,
      description: trip.description,
      startDate: trip.startDate,
      endDate: trip.endDate,
      totalDistanceKm: 0,
      waypoints: trip.waypoints,
      legs: [for (final mode in trip.modes) TripLeg(mode: mode)],
    );
  }
}
