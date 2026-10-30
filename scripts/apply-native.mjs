// Overlays the custom native code (widget, pinned timer, full-screen reminders) on the generated Android project.
import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const A = path.join(root, 'android');
const app = path.join(A, 'app');
const main = path.join(app, 'src', 'main');
if (!fs.existsSync(A)) throw new Error('android/ not found — run: npx cap add android');

function copyDir(from, to) {
  fs.mkdirSync(to, { recursive: true });
  for (const e of fs.readdirSync(from, { withFileTypes: true })) {
    const s = path.join(from, e.name), d = path.join(to, e.name);
    if (e.isDirectory()) copyDir(s, d); else fs.copyFileSync(s, d);
  }
}
function edit(file, fn) {
  const before = fs.readFileSync(file, 'utf8');
  const after = fn(before);
  if (after === before) console.log('  (no change) ' + path.relative(root, file));
  fs.writeFileSync(file, after);
}

// 1. Java sources, resources, manifest
copyDir(path.join(root, 'native', 'java'), path.join(main, 'java', 'com', 'ckj5', 'owlf'));
copyDir(path.join(root, 'native', 'res'), path.join(main, 'res'));
fs.copyFileSync(path.join(root, 'native', 'AndroidManifest.xml'), path.join(main, 'AndroidManifest.xml'));

// 2. Gradle variables: Android 8+, Google sign-in support in the Firebase Auth plugin
edit(path.join(A, 'variables.gradle'), s => {
  s = s.replace(/minSdkVersion = \d+/, 'minSdkVersion = 26');
  if (!s.includes('rgcfaIncludeGoogle')) s = s.replace('ext {', 'ext {\n    rgcfaIncludeGoogle = true');
  return s;
});
edit(path.join(A, 'gradle.properties'), s => s.replace(/org\.gradle\.jvmargs=.*/, 'org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8'));

// 3. App module: version + signing with the fixed key (keeps the Google sign-in SHA-1 stable)
const pkg = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8'));
const code = parseInt(process.env.GITHUB_RUN_NUMBER || '1', 10);
edit(path.join(app, 'build.gradle'), s => {
  s = s.replace(/versionCode \d+/, `versionCode ${code}`).replace(/versionName "[^"]*"/, `versionName "${pkg.version}"`);
  if (!s.includes('signingConfigs')) {
    s = s.replace('    buildTypes {', `    signingConfigs {
        owlf {
            storeFile file('../../native/owlf.keystore')
            storePassword 'owlf-sideload'
            keyAlias 'owlf'
            keyPassword 'owlf-sideload'
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    buildTypes {
        debug {
            signingConfig signingConfigs.owlf
        }`);
    s = s.replace('        release {\n', '        release {\n            signingConfig signingConfigs.owlf\n');
  }
  return s;
});

// 4. Firebase config (needed for native Google sign-in)
const gs = path.join(root, 'native', 'google-services.json');
if (fs.existsSync(gs)) { fs.copyFileSync(gs, path.join(app, 'google-services.json')); console.log('google-services.json applied'); }
else console.log('NOTE: native/google-services.json not found — the app works, but Google sign-in will not until you add it (see SETUP.md).');
console.log('native overlay applied');
