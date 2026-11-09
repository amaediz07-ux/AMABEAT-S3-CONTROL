AMABEAT S3 CONTROL — Android Studio source project

Build:
1. Install Android Studio (with Android SDK 35 and JDK 17).
2. Open this folder as a project; let Gradle sync.
3. Build > Build APK(s).
4. Install app/build/outputs/apk/debug/app-debug.apk on Android.
5. Connect phone to ESP32 Wi-Fi AMABEAT_UPLOAD (password 12345678).
6. Open app, Refresh Library, Choose Tone Folder, Upload, Auto Assign.

Requires AMABEAT S3 firmware with /api/data, /api/refresh, /api/assign,
/api/auto, /api/preview, /upload endpoints (v1.5 supplied previously).

Notes:
- This is a native Android app (not WebView).
- Folder picker uses Android Storage Access Framework.
- App connects locally to http://192.168.4.1; no cloud access.
- Android SDK/Gradle are unavailable in this environment; APK is NOT compiled/tested here.
- FFat capacity remains limited. Uploaded WAVs must fit in free space.
- Firmware's existing WAV scanner/assignment issues are not automatically fixed by an app.
- Tone preview plays through ESP32 audio output, not phone speakers.
- The current firmware does not expose volume/velocity/chain APIs, so these controls are not in this build.
