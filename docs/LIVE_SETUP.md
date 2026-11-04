# Cognitive Nexus: GitHub Live Mode

The Android APK opens to the **Native** Cognitive Nexus screen by default, which talks to your Nexus backend over Wi-Fi. Tap **Live / Files** in the top bar to open the hosted interface and GitHub file bank at:

https://nmoreland51.github.io/cognitive-nexus-android/

Tap **Native** (or press Back) to return to the AI.

## Enable publishing

1. Merge the `feature/github-live-interface` pull request into `main` after Android build checks pass.
2. In the **cognitive-nexus-android** GitHub repository, go to **Settings → Pages**, select **GitHub Actions** as the build and deployment source.
3. From **Actions**, run **Publish Cognitive Nexus Live** (or push a change to `docs/live/` or `live-bank/`).
4. Confirm the Pages URL opens from your phone.
5. Download and install the APK produced by a successful **Android APK** workflow on `main` after merge. The previous APK does not know about Live Mode.

## Updating the app's screens

Edit `docs/live/index.html` on `main`. GitHub Actions publishes your changes to GitHub Pages. The open app checks `version.json` every 30 seconds and reloads when its SHA changes. Actual publication and caching can introduce a delay. A file's contents do not change until deployment finishes.

## Using GitHub as the file bank

Place approved, public files in `live-bank/`. The Pages workflow copies them to the published site and generates a `catalog.json` file. Hidden files, symlinks, unsupported extensions and files above 2 MB are skipped. The app lists and previews files from the published catalog. This is a published subset, not unrestricted access to the whole repository. No GitHub personal-access token or custom API server is required.

Supported types: Markdown, plain text, JSON, YAML, TOML, XML, CSV, HTML, CSS, JavaScript, Python, Kotlin, Java, PNG, JPG, WebP, GIF, SVG and PDF. Most text types show as escaped source code. HTML is isolated in a sandboxed frame. PDF display inside Android WebView is device-dependent; use the external file link if embedded preview doesn't work.

**Security:** GitHub Pages publishes this site and the included bank files publicly. Never store secret API keys or private data in the published folder. GitHub is hosting the static interface, not executing Ollama, Python, or model inference. The existing native Mobile API v2 screens remain accessible through the **Native** button but still require their original backend.

## Updating native Android code

Editing `.kt` or Gradle files still requires a new APK build and install. GitHub Pages updates cannot silently replace installed native code. Review GitHub Actions checks before distributing a new APK.
