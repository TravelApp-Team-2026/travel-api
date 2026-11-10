# Travel

Flutter scaffold for the mobile app.

## State management and routing

- State management: [Riverpod](https://pub.dev/packages/flutter_riverpod) (`flutter_riverpod`)
- Routing: [go_router](https://pub.dev/packages/go_router)

## Folder structure

Under `lib/`:

- `models/` — data models
- `services/` — backend and other services
- `screens/` — screens
- `widgets/` — shared widgets
- `theme/` — app theme

## Backend base URL

The Android emulator does not reach the host machine through `localhost` (`localhost` on the emulator is the emulator itself). The default base URL uses `10.0.2.2`, which the emulator maps to the host loopback.

Default: `http://10.0.2.2:8080`

Defined in `lib/services/api_config.dart`. Override the full URL at run or build time:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```
