# Cognitive Nexus Android

Cognitive Nexus Android is a native Kotlin / Jetpack Compose / Material 3 client for the **original Cognitive Nexus engine**. Chat, Reality-First Research, Web Research / Bloodhound, files and knowledge notes, memory, image generation, gallery, diagnostics, persona and model controls use the authenticated Mobile API v2 adapter included in `backend/`.

This is not a website, WebView, or embedded Streamlit. LLMs and image models run on your computer/server. The APK does not contain model weights or provider keys and does not work as an offline LLM. Missing providers show errors, not fabricated replies or placeholder images.

The new debug app is named **Cognitive Nexus Mobile**, package `com.nmoreland.cognitivenexus.mobile`, version code 3. Home shows the source revision. It installs separately from the previous `.debug` and `.dashboard` previews. Only this build supports Mobile API v2.

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

Run the new Mobile API v2 adapter before using the app. **The old demo API and Streamlit on port 8501 are not this API.** The verified routes and engine mappings are in `backend-reference/MOBILE_API_V2.md`.

Install the original engine's dependencies in a Python environment, then from this Android repository root:

```powershell
python -m pip install -r backend/requirements.txt
# Generate a token once and keep it private. Enter the same token in the phone's Settings.
$env:NEXUS_API_TOKEN = python -c "import secrets; print(secrets.token_urlsafe(32))"
python -m backend --engine-root "C:\path\to\original-cognitive-nexus"
```

The engine root must contain `modules/nexus_core.py`. No original source files need to be modified.
The adapter runs the original engine with its existing knowledge, facts, persona, and image directories.
Mobile conversations and task results are persisted separately in `backend-data/mobile.sqlite3`.
Treat those directories as private. Do not run concurrent desktop and mobile writes to the same
JSON knowledge/profile files; the mobile adapter serializes its own operations but cannot lock the
separate Streamlit process. Do not start multiple adapter workers against the same engine data.

Start Ollama and load/select an installed model. Alternatively configure cloud provider credentials
**only on the backend**. Image generation needs Automatic1111 with `--api`, local Diffusers dependencies,
or ComfyUI with a saved trusted API-format JSON workflow in `data/comfyui/workflows/`. The APK does not
install server models or tools. Provider discovery is not an inference test; the last successful chat
provider/model is available in Diagnostics.

For a physical phone, bind to your actual LAN IP explicitly:

```powershell
python -m backend --engine-root "C:\path\to\original-cognitive-nexus" --host YOUR_COMPUTER_LAN_IP
```

Use your private Wi-Fi/VPN and restrict the firewall to trusted devices. Do not expose port 8000 to the
public internet. For production, put the service behind authenticated HTTPS and restrict outbound
access from the research engine. This is a single-owner service, not a multi-tenant API.

In the phone: Settings → backend URL + access token → Save and connect → select a detected provider/model.
For one secure connection at home **and away**, follow [REMOTE_ACCESS.md](REMOTE_ACCESS.md).
The token is encrypted using Android Keystore before DataStore storage. Backups are disabled. Blank
token input keeps a saved token only when the URL is unchanged; changing server clears it.

Jobs survive brief disconnections: use **Check saved task**, which polls the existing task rather than
repeating it. On process restart the app reconnects to pending tasks. Server restart marks interrupted
tasks failed; review saved outputs before submitting again. Changing backend URL clears pending IDs.

For an Android emulator, the default backend URL is `http://10.0.2.2:8000/`. `10.0.2.2` is Android Emulator's special address for the computer hosting the emulator, so it reaches a backend running on that computer. It works only on an emulator.

For a physical phone, open **Settings** and use a reachable private LAN URL such as `http://192.168.1.50:8000/`, or a deployed HTTPS URL. Do not use `localhost`: on a phone it means the phone itself, not your computer. The signed app permits HTTP only for numeric private-LAN IPv4 addresses (`10.x.x.x`, `172.16–31.x.x`, or `192.168.x.x`) so home-network development works. Public and production backends must use HTTPS.

Debug builds intentionally permit HTTP for local development. Keep your backend private and move to HTTPS before distribution.

## GitHub releases

The GitHub Actions workflow tests the API contract and Kotlin serialization, then builds a debug APK
on `main`, the review branch `feat/native-backend-v2`, pull requests, and manual runs. Download
**cognitive-nexus-native-backend-v2-apk** from the successful Actions run; unzip it and install
`cognitive-nexus-native-backend-v2.apk`. The ZIP includes its commit identifier in `build-info.txt`.
Debug signing is for testing; different runners can use different debug keys. If updating an existing
Mobile debug install reports a signing conflict, uninstall that debug app first (its device settings
are removed; server data remains). Signed releases use your stable private keystore.

A tag push matching `v*` builds the debug APK and, only when all signing secrets are present, creates
a signed GitHub Release containing an APK and AAB. Missing secrets skip the signed release without
failing the debug build. Only the tag-release job has `contents: write`; normal builds are read-only.

To publish version 1.0.0 after reviewing and merging the change:

```sh
git tag v1.0.0
git push origin v1.0.0
```

Set these GitHub Actions secrets before creating a signed release: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.

On an Android phone, open the project’s **Releases** page, download the release APK, and open it. Android may ask you to allow installs from the app you used to download or open it (such as GitHub, Chrome, or Files); allow that source only when you trust the release. The AI chat feature still needs a running, reachable backend.

## Tests and remaining parity work

```powershell
python -m unittest backend.test_api -v
python -m backend.live_check --engine-root "C:\path\to\original-cognitive-nexus"
# Opt-in real local inference test (uses a separate test conversation):
python -m backend.live_check --engine-root "C:\path\to\original-cognitive-nexus" --chat --model llama3.2:3b
# Test file ingestion and retrieval without touching your personal knowledge store:
python -m backend.live_check --engine-root "C:\path\to\original-cognitive-nexus" --knowledge
```

```sh
./gradlew tasks
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

All main sections now have native controls backed by implemented adapter operations. This is **not a
claim of complete one-for-one desktop parity**: advanced router tuning, raw logs/evaluation runners,
arbitrary tool execution, streamed token rendering, custom workflow uploading, and some desktop-only
controls are not exposed. Existing image provider availability and research dependencies still govern
what can run. See `ANDROID_PORT_STATUS.md` for what was actually tested, build results, and limitations.

Security design references: [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
and [FastAPI bearer authentication](https://fastapi.tiangolo.com/tutorial/security/).
