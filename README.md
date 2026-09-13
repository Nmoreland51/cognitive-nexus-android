# Cognitive Nexus Android

Cognitive Nexus Android is a native Kotlin Android client for the Cognitive Nexus AI backend. It provides a Material 3 chat experience, backend settings stored on-device, and no fabricated AI responses: a real answer appears only after the reachable backend returns one.

## Open and build

Open this repository root in Android Studio. Let Android Studio finish Gradle sync, then select an emulator or Android device and run the `app` configuration.

To build a debug APK locally, use:

```sh
./gradlew :app:assembleDebug
```

The expected output is:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Connect a backend

Run the Cognitive Nexus backend before sending chat messages. The app uses the verified `POST /api/chat` contract; it does not call Ollama directly.

For an Android emulator, the default backend URL is `http://10.0.2.2:8000/`. `10.0.2.2` is Android Emulator's special address for the computer hosting the emulator, so it reaches a backend running on that computer. It works only on an emulator.

For a physical phone, open **Settings** and use a reachable LAN URL such as `http://192.168.1.50:8000/` during debug development, or a deployed HTTPS URL. Do not use `localhost`: on a phone it means the phone itself, not your computer. Production must use HTTPS; release builds do not permit cleartext HTTP connections.

Debug builds intentionally permit HTTP for local development. Keep your backend private and move to HTTPS before distribution.

## GitHub releases

The GitHub Actions workflow builds a debug APK on pushes to `main`. A tag push matching `v*` builds the debug APK and, only when all signing secrets are present, creates a signed GitHub Release containing an APK and AAB.

To publish version 1.0.0 after reviewing and merging the change:

```sh
git tag v1.0.0
git push origin v1.0.0
```

Set these GitHub Actions secrets before creating a signed release: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.

On an Android phone, open the project’s **Releases** page, download the release APK, and open it. Android may ask you to allow installs from the app you used to download or open it (such as GitHub, Chrome, or Files); allow that source only when you trust the release. The AI chat feature still needs a running, reachable backend.
