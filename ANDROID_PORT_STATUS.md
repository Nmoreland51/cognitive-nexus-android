# Android Port Status

## Created Android files

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts` and `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`
- `app/src/debug/AndroidManifest.xml`
- `app/src/debug/res/xml/network_security_config.xml`
- `app/src/main/java/com/nmoreland/cognitivenexus/MainActivity.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/data/BackendSettingsRepository.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/data/ChatRepository.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/data/api/CognitiveNexusApi.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/data/api/NetworkClient.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/ui/ChatViewModel.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/ui/SettingsViewModel.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/ui/CognitiveNexusApp.kt`
- `app/src/main/res/values/themes.xml`
- `.github/workflows/android-release.yml`

## Modified existing files

- `README.md`
- `.gitignore`

## Backend contract

The app uses only verified endpoints from `backend-reference/API_CONTRACT_DISCOVERY.md`: `GET /api/health` is represented in the Retrofit client and `POST /api/chat` is used for chat. The app does not call unimplemented `GET /api/models`, and it never fabricates assistant replies when the backend cannot be reached.

## Validation

Attempted commands (Windows equivalents of the requested Gradle commands):

1. `./gradlew tasks` attempted as `./gradlew.bat tasks`.
   - Initial result: failed because `gradle/wrapper/gradle-wrapper.jar` was missing.
   - Fix applied: added the official Gradle 8.7 wrapper JAR.
   - Final result: failed because Java is not installed or available on `PATH` in the validation environment. First exact error: `'java' is not recognized as an internal or external command, operable program or batch file.`
2. `./gradlew :app:assembleDebug` attempted as `./gradlew.bat :app:assembleDebug`.
   - Result: failed for the same environment prerequisite. First exact error: `'java' is not recognized as an internal or external command, operable program or batch file.`

No APK was built, so there is no verified APK output path.

## Release workflow

GitHub Actions workflow created: `.github/workflows/android-release.yml`.

Signed tag releases require these GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Verified on 2026-09-13: the repository currently has no GitHub Releases. No downloadable APK has been created by this change.

## Next steps

1. Install JDK 17 and ensure `java` is on `PATH`, then run `./gradlew tasks` and `./gradlew :app:assembleDebug`.
2. Review and commit the working-tree diff, then push the reviewed change to `main` to receive the debug APK workflow artifact.
3. Configure signing secrets before pushing a `v*` tag if a signed GitHub Release is desired.
4. Run the backend on a reachable address and configure the app with an emulator or HTTPS/LAN backend URL.
