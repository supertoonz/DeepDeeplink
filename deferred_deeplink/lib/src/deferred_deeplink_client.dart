import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'dart:io';

/// Options retrieved from the deferred install endpoint
class DeferredInstallParameters {
  final String? utmSource;
  final String? utmMedium;
  final String? utmCampaign;

  DeferredInstallParameters({
    this.utmSource,
    this.utmMedium,
    this.utmCampaign,
  });
}

/// A service to handle custom deferred deep linking parameters
/// fetched from a backend because the user clicked a link before installing.
class DeferredDeeplinkClient {
  final String apiEndpoint;

  /// Create a client pointing to your backend endpoint
  /// (e.g., https://yourdomain.com/api/install)
  DeferredDeeplinkClient({required this.apiEndpoint});

  /// Check if this is the first open, call the backend to see if there are
  /// deferred campaign parameters, and return them.
  ///
  /// The consuming app is responsible for logging these to Analytics (e.g. Firebase).
  /// Returns null if not the first open, or if no params were found/errors occurred.
  Future<DeferredInstallParameters?> checkDeferredDeeplinkOnFirstOpen() async {
    try {
      final prefs = await SharedPreferences.getInstance();

      // We only want to check our backend once ever (the first time the app launches).
      final hasCheckedDeferred =
          prefs.getBool('has_checked_deferred_install') ?? false;
      if (hasCheckedDeferred) return null;

      // Build a device fingerprint to send to the server.
      final deviceFingerprint = await _getDeviceFingerprint();

      // Retrieve deferred parameters from our custom backend
      final response = await http.post(
        Uri.parse(apiEndpoint),
        headers: {
          'Content-Type': 'application/json',
          'User-Agent': _getUserAgentString(),
        },
        body: jsonEncode({
          'device_id': deviceFingerprint,
        }),
      );

      // Mark that we have checked regardless of success
      // to avoid persistent polling if backend fails
      await prefs.setBool('has_checked_deferred_install', true);

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          final campaignData = data['data'];

          return DeferredInstallParameters(
            utmSource: campaignData['utm_source'] as String?,
            utmMedium: campaignData['utm_medium'] as String?,
            utmCampaign: campaignData['utm_campaign'] as String?,
          );
        }
      }
      return null;
    } catch (e) {
      debugPrint('[DeferredDeeplinkClient] Error: $e');
      return null;
    }
  }

  /// Helper to generate a unique string based on basic device properties
  Future<String> _getDeviceFingerprint() async {
    final deviceInfo = DeviceInfoPlugin();
    String fingerprint = 'unknown';

    try {
      if (Platform.isAndroid) {
        final androidInfo = await deviceInfo.androidInfo;
        fingerprint =
            '${androidInfo.brand}_${androidInfo.model}_${androidInfo.version.sdkInt}';
      } else if (Platform.isIOS) {
        final iosInfo = await deviceInfo.iosInfo;
        fingerprint =
            '${iosInfo.name}_${iosInfo.systemName}_${iosInfo.systemVersion}';
      }
    } catch (e) {
      debugPrint('[DeferredDeeplinkClient] _getDeviceFingerprint error: $e');
    }

    return fingerprint;
  }

  String _getUserAgentString() {
    return 'DeferredDeeplinkClient/1.0 (${Platform.operatingSystem})';
  }
}
