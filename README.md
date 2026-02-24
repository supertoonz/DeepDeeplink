# Deferred Deep Linking Sample Architecture

This repository contains a simple prototype of how you can implement Custom Deferred Deep Linking without relying on a paid attribution provider (like AppsFlyer or Branch.io) if you strictly need basic iOS/Android attribution recorded in Google Analytics via Firebase.

## Components
1. **Server (Node.js + Express):**
   - Host a link for marketing campaigns (e.g. `https://your-domain.com/click?utm_source=...`)
   - It captures the user's IP address and User-Agent, creates a temporary fingerprint record, and redirects them to the App Store or Play Store.
   - It exposes a `/api/install` endpoint to check if an installation matches a recent fingerprint.

2. **Mobile Sample (Flutter):**
   - Inside the app's startup flow (e.g., in your `main.dart` or during the splash screen sequence), you check if it's the user's first time opening the app.
   - You call the `/api/install` endpoint.
   - If the server responds with the saved `utm_*` parameters, you log the `campaign_details` event using `firebase_analytics`.
   - Firebase will then attribute the `first_open` to that specific campaign.

## How to use
1. Start the server:
   ```bash
   cd server
   npm install
   npm start
   ```
2. Navigate to your marketing link on a device to simulate a click:
   `http://localhost:3000/click?utm_source=newsletter&utm_medium=email&utm_campaign=spring_sale`
3. The server logs the fingerprint.
4. Integrate the `DeferredDeeplinkService` in your Flutter `main.dart` and call `checkDeferredDeeplinkOnFirstOpen()`. Ensure it points to your server's IP address/hostname.

## Important Note
Device fingerprinting via IP and User-Agent is inherently imperfect. If multiple devices on the same Wi-Fi (same public IP) and same device model click links at the same time, misattribution occurs. For advanced accuracy, OS-level solutions like Google Play Install Referrer (Android natively supported by Firebase) and Universal Links (iOS natively supported by Firebase) are better for simple deep linking. This sample provides the "Deferred" gap filling.
