# Deferred Deeplink Library

A standalone Flutter package that handles fetching Deferred Deep Linking campaign parameters from a custom backend prior to the first app initialization.

This package provides a platform-agnostic client `DeferredDeeplinkClient` that works on both iOS and Android. It handles creating a basic device fingerprint and calling your backend to retrieve parameters like `utm_source`, `utm_medium`, and `utm_campaign`.

## Features
- **Platform Agnostic**: Uses `device_info_plus` to generate a fingerprint across iOS and Android.
- **First-open state management**: Uses `shared_preferences` to ensure the backend is only pinged once, on the very first fresh launch of the app.
- **Decoupled from Analytics**: It retrieves the Data and hands it back to your app. You are free to log it to Google Analytics for Firebase, Mixpanel, or any internal analytics service you prefer.

## Getting Started

1. Add the dependency to your app's `pubspec.yaml`:
   ```yaml
   dependencies:
     deferred_deeplink:
       path: ../path/to/deferred_deeplink
   ```

2. Initialize the client as early as possible in your app lifecycle (e.g., Splash screen or `initState` of your main widget):
   ```dart
   import 'package:deferred_deeplink/deferred_deeplink.dart';

   final client = DeferredDeeplinkClient(
     apiEndpoint: 'https://your-domain.com/api/install', 
   );

   final params = await client.checkDeferredDeeplinkOnFirstOpen();
   
   if (params != null) {
      // Send parameters to Firebase
      // FirebaseAnalytics.instance.logEvent(name: 'campaign_details', parameters: {
      //   'source': params.utmSource ?? 'unknown',
      //   'medium': params.utmMedium ?? 'unknown',
      //   'campaign': params.utmCampaign ?? 'unknown',
      // });
   }
   ```

## Note on Fingerprinting
Due to privacy restrictions on iOS and Android, deterministic deferred deep linking without tools like Apple Search Ads or Google Play Install Referrer is probabilistic. The backend uses IP matching which may be inaccurate over NAT gateways or if the user waits too long between clicking the link and opening the app.
