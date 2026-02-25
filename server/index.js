const express = require('express');
const app = express();

app.use(express.json());

// In-memory store for demonstrations (In production, use Redis/PostgreSQL)
// Key: fingerprint (IP + User-Agent), Value: parameters
const installParametersStore = new Map();

/**
 * 1. The URL users click from an Ad or Web page.
 * Example: GET /click?utm_source=fb&utm_medium=social&utm_campaign=spring_sale
 */
app.get('/click', (req, res) => {
  // Capture device fingerprint. Normalize IPv6 loopback to IPv4.
  let ip = req.ip || req.connection.remoteAddress;
  if (ip === '::1' || ip === '::ffff:127.0.0.1') ip = '127.0.0.1';
  const userAgent = req.headers['user-agent'] || '';
  const fingerprint = `${ip}`;

  // Capture campaign parameters AND dynamic Store IDs if provided
  const { utm_source, utm_medium, utm_campaign, android_id, ios_id, app_scheme } = req.query;

  // Save the parameters against the fingerprint (with a TTL in production)
  installParametersStore.set(fingerprint, {
    utm_source,
    utm_medium,
    utm_campaign,
    timestamp: Date.now()
  });

  console.log(`Saved deferred parameters for fingerprint: ${fingerprint}`);

  // Instead of a hard redirect, serve a smart HTML page that tries to open the app via custom scheme
  // and falls back to the store if the app isn't installed.
  const customSchemeUrl = app_scheme ? `${app_scheme}://home` : `deferredexample://home`;
  const playStoreUrl = android_id ? `https://play.google.com/store/apps/details?id=${android_id}` : `https://play.google.com/store/apps/details?id=com.example.app`;
  const appStoreUrl = ios_id ? `https://apps.apple.com/us/app/id${ios_id}` : `https://apps.apple.com/us/app/example/id123456789`;

  res.send(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <title>Redirecting...</title>
      <script>
        window.onload = function() {
          // 1. Try to open the mobile app via custom URL scheme
          window.location.href = "${customSchemeUrl}";

          // 2. Fallback to App Store / Play Store if app is not installed
          setTimeout(function() {
            var ua = navigator.userAgent || navigator.vendor || window.opera;
            if (/android/i.test(ua)) {
              window.location.href = "${playStoreUrl}";
            } else if (/iPad|iPhone|iPod/.test(ua) && !window.MSStream) {
              window.location.href = "${appStoreUrl}";
            } else {
              document.getElementById('msg').innerText = 'Please install the app to continue.';
            }
          }, 2500);
        }
      </script>
    </head>
    <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
      <h2 id="msg">Opening the mobile app...</h2>
    </body>
    </html>
  `);
});

/**
 * 2. The endpoint the mobile app calls upon first open.
 * Example: POST /api/install
 */
app.post('/api/install', (req, res) => {
  // Read the fingerprint from the incoming request from the mobile app
  let ip = req.ip || req.connection.remoteAddress;
  if (ip === '::1' || ip === '::ffff:127.0.0.1') ip = '127.0.0.1';
  const userAgent = req.headers['user-agent'] || '';
  // Or, if the app sends specific device info in the body
  const { device_id } = req.body;

  // NOTE: For better accuracy, use a combination of IP + Device Model 
  // passed securely from the client.
  const fingerprint = `${ip}`;

  const storedData = installParametersStore.get(fingerprint);

  if (storedData) {
    // Optional: Ensure the data is recent (e.g., < 2 hours old) to prevent misattribution
    const isRecent = (Date.now() - storedData.timestamp) < (2 * 60 * 60 * 1000);

    if (isRecent) {
      console.log(`Found deferred parameters for fingerprint: ${fingerprint}`);
      // Remove from store to prevent duplicate tracking
      installParametersStore.delete(fingerprint);

      return res.json({
        success: true,
        data: {
          utm_source: storedData.utm_source,
          utm_medium: storedData.utm_medium,
          utm_campaign: storedData.utm_campaign
        }
      });
    }
  }

  res.json({
    success: false,
    message: 'No recent referred install found.'
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Deferred Deep Link Server running on port ${PORT}`);
});
