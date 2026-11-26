import 'package:dio/dio.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/place_search_api.dart';

class FakePlaceSearchApi extends PlaceSearchApi {
  FakePlaceSearchApi({this.results = const []}) : super(ApiClient(Dio()));

  List<Place> results;
  final queries = <String>[];
  ApiException? error;
  Duration delay = Duration.zero;

  @override
  Future<List<Place>> search(String query) async {
    queries.add(query);
    if (delay > Duration.zero) {
      await Future<void>.delayed(delay);
    }
    if (error != null) {
      throw error!;
    }
    return List.of(results);
  }
}
