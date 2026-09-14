# Android Port Status

## Native backend v2 — current work (2026-09-13)

This section supersedes the historical preview results below. Work is on review branch
`feat/native-backend-v2`, not merged to main. No tag or signed release has been created by this work.

### New files in this change

- `backend/__init__.py`
- `backend/__main__.py`
- `backend/models.py`
- `backend/store.py`
- `backend/server.py`
- `backend/engine.py`
- `backend/requirements.txt`
- `backend/test_api.py`
- `backend/live_check.py`
- `backend-reference/MOBILE_API_V2.md`
- `app/src/main/java/com/nmoreland/cognitivenexus/data/NexusRepository.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/data/TokenVault.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/ui/NexusViewModel.kt`
- `app/src/main/java/com/nmoreland/cognitivenexus/ui/FeatureScreens.kt`
- `app/src/test/java/com/nmoreland/cognitivenexus/data/api/ContractTest.kt`

### Existing files modified

`.github/workflows/android-release.yml`, `.gitignore`, `README.md`, `ANDROID_PORT_STATUS.md`,
`backend-reference/API_CONTRACT_DISCOVERY.md`, `app/build.gradle.kts`,
`app/src/debug/res/values/strings.xml`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/themes.xml`,
`app/src/main/java/com/nmoreland/cognitivenexus/data/BackendSettingsRepository.kt`,
`app/src/main/java/com/nmoreland/cognitivenexus/data/api/CognitiveNexusApi.kt`,
`app/src/main/java/com/nmoreland/cognitivenexus/data/api/NetworkClient.kt`,
`app/src/main/java/com/nmoreland/cognitivenexus/ui/CognitiveNexusApp.kt`.

Removed obsolete v1 `data/ChatRepository.kt`, `ui/ChatViewModel.kt`, and `ui/SettingsViewModel.kt`;
their functionality is replaced by the v2 repository/ViewModel. They remain recoverable in Git history.
No original Python-engine source files were edited. Live checks initialized normal engine runtime folders/logs.

### Validation actually performed

- `python -m unittest backend.test_api -v`: **10 tests passed**. An intervening restricted-shell run
  failed with `sqlite3.OperationalError: unable to open database file` because Windows protected temporary
  directories were inaccessible. Rerunning with approved access passed all 10 tests.
- `python -m backend.live_check --engine-root <original-engine-root> --chat --model llama3.2:3b`:
  overview, memory, notes, gallery, tools, profile, workflows all passed against the real original engine.
  Real chat passed using **Ollama / llama3.2:3b**, reply: `The number two plus two equals four.`
  Engine-reported chat duration approximately 12.16 seconds. This is not a mocked response.
- `.\gradlew.bat tasks`: failed locally with exact first error:
  `'java' is not recognized as an internal or external command, operable program or batch file.`
- `.\gradlew.bat :app:assembleDebug`: failed locally with the same missing-Java error.
- `git diff --check`: passed.
- GitHub Actions run **34797116866**, commit `2b07a4b500ca7af9cdfd9cd64f09f30b1cee3b89`:
  `./gradlew tasks`, `./gradlew :app:testDebugUnitTest`, and `./gradlew :app:assembleDebug` all **passed**.
  Verified output: `app/build/outputs/apk/debug/app-debug.apk`; uploaded copy named
  `cognitive-nexus-native-backend-v2.apk`. This run validates that commit, not later edits.
- Expanded live checks also passed the actual authenticated HTTP job routes, chat session history,
  gallery PNG download, and real file ingestion/hash-vector knowledge retrieval in an isolated store.
  Live inference validation verifies provider execution, **not answer accuracy**: a second arithmetic
  request returned the incorrect answer `The answer is two.` The adapter does not correct or fabricate
  model outputs. Evaluate the chosen LLM separately for answer quality.

### Supported vs still unverified

All main native sections now call implemented v2 operations. Jobs, authenticated media, persisted sessions,
explicit memory changes with forget confirmation, file selection, shared persona, and model controls are wired.
The app checks API version before connecting; old demo endpoints are never used. Fallback text is rejected.

Real live checks do **not yet verify** web/Reality-First/Bloodhound research,
image generation, or a ComfyUI workflow end-to-end. ComfyUI was unavailable during detection. These need
their real dependencies/models and functional smoke tests before claiming complete parity.
Advanced desktop router tuning, raw logs/evaluation commands, arbitrary tools, token-by-token streaming,
custom workflow uploading, and full mobile device UI testing remain outstanding. Native image export
is implemented via Android's document picker; physical-device execution remains untested.
Do not describe this as every desktop function fully ported or production-ready.

Workflow: `.github/workflows/android-release.yml` now tests Python and Kotlin and builds a distinctly named
`cognitive-nexus-native-backend-v2-apk` artifact. Debug label `Cognitive Nexus Mobile`, package
`com.nmoreland.cognitivenexus.mobile`, version code 2, visible source revision. Only the tag-release job
has write permissions. Required signing secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Missing secrets skip a signed release, not the debug build.

Next: validate CI, review/merge the branch, start the adapter and pair the phone, then complete the remaining
real-provider and physical-device checks. Tag `v1.0.0` only after main is green and signing is configured.

## Historical original starter / dashboard preview (not current v2 validation)

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
