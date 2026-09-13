# Cognitive Nexus Android

Cognitive Nexus Android is a native Kotlin Android app using Jetpack Compose + Material 3 for chat interactions with the Cognitive Nexus backend.

## App overview

- Native Android Studio Gradle project at repository root
- Kotlin + Jetpack Compose + Material 3 UI
- Chat screen with message bubbles, input, send button, loading and error state
- Settings screen with configurable backend URL persisted via DataStore
- Retrofit + OkHttp networking repository layer wired to backend API contract

## Open in Android Studio

1. Clone this repository.
2. Open Android Studio.
3. Select **Open** and choose the repository root directory (`cognitive-nexus-android`).
4. Let Gradle sync complete.

## Build debug APK

```bash
./gradlew assembleDebug
```

If successful, debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Run on emulator

1. Create/start an Android emulator in Android Studio Device Manager.
2. Run the `app` configuration.
3. Keep backend URL as default unless needed:
   - `http://10.0.2.2:8000/`

## Configure physical Android device

1. Enable Developer Options and USB debugging on device.
2. Connect device via USB (or set up wireless debugging).
3. Verify device appears in Android Studio.
4. Use a backend URL reachable from the phone (prefer HTTPS for release installs), not `10.0.2.2`.

## Configure backend URL

1. Open the app.
2. Tap **Settings**.
3. Set backend URL and tap **Save**.
4. Use **Check health** to validate `/api/health`.

## Download APK from GitHub Releases

- Signed release APKs/AABs are attached to GitHub Releases for `v*` tags **only when signing secrets are configured**.
- Debug APK is always uploaded as a GitHub Actions artifact for workflow runs.

## Publish a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Tag pushes trigger release workflow logic.

## Install APK from GitHub/Chrome/Files

1. Download APK to device.
2. Open APK from browser download list or Files app.
3. If prompted, allow installation from that source.
4. Confirm install.

## Backend dependency limitation

This app depends on the Cognitive Nexus backend API contract in `backend-reference/API_CONTRACT_DISCOVERY.md`.

- Verified endpoints used directly: `GET /api/health`, `POST /api/chat`
- `/api/models` is marked as missing/incomplete in the contract; app surfaces setup guidance instead of faking unsupported behavior.
- App is prepared for a future mobile adapter layer to the original Python/Streamlit backend.

## Networking notes (important)

- `10.0.2.2` works only from Android emulator to reach host machine localhost.
- Physical phones must use a backend reachable from the device; release installs should use HTTPS.
- Android localhost (`127.0.0.1` on device) is **not** your computer's localhost.
- Never embed API keys or secrets in the APK.
