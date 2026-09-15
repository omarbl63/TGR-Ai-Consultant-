import Constants from 'expo-constants';

/**
 * API base URL. Defaults to localhost (simulator / Expo web). To run on a
 * physical device via Expo Go, set `expo.extra.apiBase` in app.json to your
 * machine's LAN IP, e.g. http://192.168.1.20:8080/api/v1
 */
export const API_BASE: string =
  (Constants.expoConfig?.extra?.apiBase as string) ?? 'http://localhost:8080/api/v1';
