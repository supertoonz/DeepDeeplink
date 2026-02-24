import 'package:flutter/material.dart';
import 'package:deferred_deeplink/deferred_deeplink.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Deferred Deeplink Example',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String _status = 'Initializing...';
  String _campaignData = '';

  @override
  void initState() {
    super.initState();
    _checkDeferredInstall();
  }

  Future<void> _checkDeferredInstall() async {
    setState(() => _status = 'Checking for deferred deeplink...');

    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear(); // FORCE CHECK FOR TESTING

      // 1. Initialize the library client
      final client = DeferredDeeplinkClient(
        apiEndpoint:
            'https://upset-vans-shave.loca.lt/api/install', // Public URL
      );

      // 2. Fetch parameters
      final params = await client.checkDeferredDeeplinkOnFirstOpen();

      if (params != null) {
        setState(() {
          _status = 'Found deferred install!';
          _campaignData =
              '''
Source: ${params.utmSource}
Medium: ${params.utmMedium}
Campaign: ${params.utmCampaign}
          ''';
        });

        // 3. Log to your Analytics provider here
        // FirebaseAnalytics.instance.logEvent(name: 'campaign_details', parameters: {...});
        debugPrint('Logged to Analytics Provider: $_campaignData');
      } else {
        setState(
          () => _status = 'No deferred install found, or already checked.',
        );
      }
    } catch (e) {
      setState(() => _status = 'Error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Deferred Install Library Demo')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text(
                'Deferred Deeplink Status:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              Text(_status, textAlign: TextAlign.center),
              const SizedBox(height: 24),
              if (_campaignData.isNotEmpty) ...[
                const Text(
                  'Data Retrieved:',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                Text(_campaignData),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
