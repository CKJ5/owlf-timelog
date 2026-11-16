# OWLF Time Log — notes for Claude

- The whole web app is the single file `index.html` (vanilla JS, no bundler). Keep it that way.
- `__APP_VERSION__` and `__BUILD_ID__` in index.html are replaced by `scripts/build-www.mjs` for the Android build — never replace them by hand.
- Pushing to `main` builds the APK (`.github/workflows/build-apk.yml`) and refreshes the `apk-latest` release; the app checks that release to detect updates.
- When you change behaviour, bump `version` in package.json.
- Planner data (S.plan blocks / S.projects) syncs through Firestore with per-item `updated` merge: call `touchP(item)` after changing any block/project.
- Paused work sessions live in `S.active['wp_<planId>']`; running ones in `S.active['w_<type>']`.
