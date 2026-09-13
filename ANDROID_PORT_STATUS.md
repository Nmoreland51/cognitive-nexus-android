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

GitHub Actions validation on Ubuntu with Java 17:

1. Run `34782350357` attempted `./gradlew :app:assembleDebug` and failed at Kotlin compilation. First exact error: `Unresolved reference 'weight'` in `CognitiveNexusApp.kt`.
2. Run `34782569868` attempted the same command and failed at Kotlin compilation. First exact error: `Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.`
3. Run `34782813824` successfully completed `./gradlew :app:assembleDebug` after the two targeted Compose layout fixes.

Verified APK output path: `app/build/outputs/apk/debug/app-debug.apk`.
The successful workflow uploaded it as the non-expired `cognitive-nexus-debug-apk` artifact (17,518,690 bytes).

## Release workflow

GitHub Actions workflow created: `.github/workflows/android-release.yml`.

Signed tag releases require these GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Verified on 2026-09-13: the repository currently has no GitHub Releases. A downloadable debug APK workflow artifact exists, but no GitHub Release APK has been created.

## Next steps

1. Install JDK 17 and ensure `java` is on `PATH` to repeat local validation, if desired.
2. Configure signing secrets before pushing a `v*` tag if a signed GitHub Release is desired.
3. Push a `v1.0.0` tag only when ready to create the first release.
4. Run the backend on a reachable address and configure the app with an emulator or HTTPS/LAN backend URL.
