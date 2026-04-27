const CACHE = 'owlf-v2';

self.addEventListener('install', e => {
  self.skipWaiting();
  e.waitUntil(
    caches.open(CACHE).then(c => c.addAll([
      './',
      './index.html'
    ]).catch(() => {}))
  );
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys => Promise.all(
      keys.filter(k => k !== CACHE).map(k => caches.delete(k))
    )).then(() => clients.claim())
  );
});

self.addEventListener('fetch', e => {
  e.respondWith(
    fetch(e.request).then(res => {
      const clone = res.clone();
      caches.open(CACHE).then(c => c.put(e.request, clone));
      return res;
    }).catch(() => caches.match(e.request))
  );
});

self.addEventListener('message', e => {
  if (!e.data || e.data.type !== 'NOTIFY') return;
  const { title, body, tag } = e.data;
  e.waitUntil(self.registration.showNotification(title, {
    body, tag,
    icon: 'https://design.owlfstudio.com/wp-content/uploads/2023/12/OWLF_Studio_Favicon.svg',
    renotify: true,
    requireInteraction: false
  }));
});
