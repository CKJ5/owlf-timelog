// Builds ./www for the Android app: the web app + a local copy of the Firebase SDK (works offline).
import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const www = path.join(root, 'www');
fs.rmSync(www, { recursive: true, force: true });
fs.mkdirSync(path.join(www, 'vendor'), { recursive: true });

let html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const appVersion = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8')).version;
html = html.replaceAll('__APP_VERSION__', appVersion);   // shown in the app's Android settings block
let replaced = 0;
for (const n of ['app', 'auth', 'firestore']) {
  const cdn = `https://www.gstatic.com/firebasejs/10.12.0/firebase-${n}-compat.js`;
  const src = path.join(root, 'node_modules', 'firebase', `firebase-${n}-compat.js`);
  if (!fs.existsSync(src)) throw new Error('Missing ' + src + ' — run npm ci first');
  fs.copyFileSync(src, path.join(www, 'vendor', `firebase-${n}-compat.js`));
  if (html.includes(cdn)) { html = html.replace(cdn, `./vendor/firebase-${n}-compat.js`); replaced++; }
}
if (replaced !== 3) throw new Error('Expected to localise 3 Firebase scripts, got ' + replaced + ' — did the CDN URLs in index.html change?');

// The Android bridge only provides the native messaging layer; Capacitor.registerPlugin comes from @capacitor/core.
// The page has no bundler, so ship core's standalone build and load it first (the web version is untouched).
const coreSrc = path.join(root, 'node_modules', '@capacitor', 'core', 'dist', 'capacitor.js');
if (!fs.existsSync(coreSrc)) throw new Error('Missing ' + coreSrc + ' — run npm ci first');
fs.copyFileSync(coreSrc, path.join(www, 'vendor', 'capacitor.js'));
const TITLE = '<title>OWLF Time Log</title>';
if (!html.includes(TITLE)) throw new Error('Could not find ' + TITLE + ' in index.html to attach capacitor.js');
html = html.replace(TITLE, TITLE + '\n<script src="./vendor/capacitor.js"></script>');
fs.writeFileSync(path.join(www, 'index.html'), html);

for (const f of ['icon-192.png', 'icon-512.png', 'favicon.png', 'badge-72.png']) {
  if (fs.existsSync(path.join(root, f))) fs.copyFileSync(path.join(root, f), path.join(www, f));
}
console.log('www ready (' + fs.readdirSync(www).join(', ') + ')');
