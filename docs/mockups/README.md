# UI mockups

Mobile mockups (360 × 800) of the main screens, the color palette and the component sheet. Open any file in the browser or in the GitHub file view.

## Design system

| File | Content |
|---|---|
| `paleta_kolorow.svg` | Color palette: brand, surfaces, text, states, one color and line style per transport mode, map |
| `komponenty.svg` | Typography scale, button states, text fields, card, empty state |

## Screens

| Screen | Files |
|---|---|
| Login | `login.svg`, `login_error.svg` |
| Registration | `register.svg` |
| Trips list | `trips_list.svg`, `trips_list_refreshing.svg`, `trips_empty.svg` |
| Network error | `error_network.svg` |
| Trip editor | `trip_editor.svg`, `sheet_transport.svg` |
| Place search | `place_search_loading.svg`, `place_search_results.svg`, `place_search_empty.svg` |
| Trip details | `trip_details.svg`, `trip_details_leg_selected.svg`, `dialog_delete_trip.svg` |
| Global map | `map_global.svg`, `map_global_route_selected.svg` |
| Statistics | `statistics.svg`, `statistics_countries.svg`, `statistics_empty.svg` |
| Profile | `profile.svg`, `dialog_logout.svg` |
| Travelers | `travelers_list.svg`, `travelers_empty.svg` |
| Public profile | `public_profile.svg`, `public_trip_details.svg` |

## Navigation

The bottom navigation has four tabs: Trips, Map, Statistics and Profile. The travelers list is opened from the header icon of the Trips screen.

```mermaid
flowchart TD
  login["Login<br/>login.svg · login_error.svg"]
  register["Registration<br/>register.svg"]
  login -->|Register link| register
  register -->|Login link| login
  login -->|signed in| trips
  register -->|account created| trips

  subgraph tabs["Bottom navigation"]
    trips["Trips<br/>trips_list.svg · trips_list_refreshing.svg · trips_empty.svg"]
    map["Map<br/>map_global.svg"]
    stats["Statistics<br/>statistics.svg · statistics_empty.svg"]
    profile["Profile<br/>profile.svg"]
  end

  trips -.->|no connection| neterr["Network error<br/>error_network.svg"]
  trips -->|add trip| editor["Trip editor<br/>trip_editor.svg"]
  trips -->|trip card| details["Trip details<br/>trip_details.svg · trip_details_leg_selected.svg"]
  details -->|edit| editor
  editor -->|add stop| search["Place search<br/>place_search_loading.svg · place_search_results.svg · place_search_empty.svg"]
  search -->|place chosen| editor
  editor -->|transport of a leg| sheet["Transport sheet<br/>sheet_transport.svg"]
  sheet -->|mode chosen| editor
  editor -->|save| trips
  details -->|delete| deleteDialog["Delete dialog<br/>dialog_delete_trip.svg"]
  deleteDialog -->|confirm or cancel| trips

  map -->|route tapped| route["Map with selected route<br/>map_global_route_selected.svg"]
  stats -->|countries| countries["Visited countries<br/>statistics_countries.svg"]
  countries -->|show on map| map

  trips -->|travelers icon| travelers["Travelers<br/>travelers_list.svg · travelers_empty.svg"]
  travelers -->|person| pub["Public profile<br/>public_profile.svg"]
  pub -->|trip| pubtrip["Public trip details<br/>public_trip_details.svg"]

  profile -->|log out| logout["Logout dialog<br/>dialog_logout.svg"]
  logout -->|confirm| login
  logout -->|cancel| profile
