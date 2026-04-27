const CACHE = 'owlf-v3';

self.addEventListener('install', e => {
  self.skipWaiting();
  e.waitUntil(
    caches.open(CACHE).then(c => {
      return c.add('./index.html');
    })
  );
});

self.addEventListener('activate', e => {
  self.skipWaiting();
  e.waitUntil(clients.claim());
});

self.addEventListener('fetch', e => {
  if (e.request.mode === 'navigate') {
    e.respondWith(
      fetch(e.request).catch(() => caches.match('./index.html'))
    );
    return;
  }
  e.respondWith(
    caches.match(e.request).then(r => r || fetch(e.request))
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
