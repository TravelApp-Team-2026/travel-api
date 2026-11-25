import 'package:dio/dio.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/trips_api.dart';

class FakeTripsApi extends TripsApi {
  FakeTripsApi([this.trips = const []]) : super(ApiClient(Dio()));

  List<TripSummary> trips;
  int lists = 0;
  ApiException? listError;

  @override
  Future<List<TripSummary>> listTrips() async {
    lists++;
    if (listError != null) {
      throw listError!;
    }
    return List.of(trips);
  }
}
