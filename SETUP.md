# OWLF Android app — setup (about 15 minutes, no PC tools needed)

The GitHub Action in `.github/workflows/build-apk.yml` builds the APK in the cloud.

## 1. Build the APK
1. Copy everything in this folder into the **root of your repo** (overwrite `index.html`), commit, push to `main`.
2. GitHub → **Actions** → *Build Android APK* → wait ~10 min (first run is slower).
   If it's red, open the run, copy the last error lines and send them to me.
3. GitHub → **Releases** → *OWLF APK (latest build)* → tap **owlf.apk** on your phone → install
   (Android will ask once to allow installs from your browser).

Every later push to `main` builds a new APK and replaces that file. Installing over the old app keeps your data
(same signing key).

## 2. Turn on the Android-only features
Open the app → **Settings → Android app**. Tap *Allow* on each ⚠️ line:
- **Notifications**
- **Pop up over other apps** — this is what lets the reminder take over the whole screen, even when the phone is unlocked
- **Full-screen notifications** (Android 14+) — lock-screen version of the same thing
- **Unrestricted battery** — so alarms are not delayed

Then tap **🔔 Test reminder**, press Home, and wait 10 seconds.
Add the widget: Settings → *＋ Add widget*, or long-press the home screen → Widgets → OWLF.

## 3. Google sign-in (needed for cloud sync and Google Calendar)
Browser pop-ups don't work inside an app, so the app uses Android's own Google sign-in. One-time setup:
1. Firebase console → your project → ⚙ **Project settings** → *Your apps* → **Add app → Android**
   - Package name: `com.ckj5.owlf`
   - SHA-1: `D0:92:B7:B8:32:39:41:71:A7:60:2B:87:8C:65:7E:B1:90:2E:59:DD`
2. Download **google-services.json** and save it in the repo as `native/google-services.json`. Push.
3. Wait for the new build and reinstall. Settings → Sign in with Google.

Use the same Google account as on the web version — your data is already in the cloud and will appear.

## 4. Google Calendar sync (optional)
Google Cloud console (same project as Firebase) → **APIs & Services → Library → Google Calendar API → Enable**.
If Google says the app is unverified, add your account under *OAuth consent screen → Test users*.

## Notes
- `native/owlf.keystore` is the signing key (password `owlf-sideload`). It is committed on purpose so the app's
  SHA-1 never changes. That's fine for a personal app; don't reuse it for anything public.
- The web version in this repo keeps working as before (GitHub Pages etc.).
- To change what the widget buttons do, edit `native/java/WidgetProvider.java` and the `applyNativeAction` function in `index.html`.
