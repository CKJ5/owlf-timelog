const CACHE = 'owlf-v4'; // ← bump this when you deploy

self.addEventListener('install', e => {
  self.skipWaiting();
  e.waitUntil(
    caches.open(CACHE).then(c => c.add('./index.html'))
  );
});

self.addEventListener('activate', e => {
  self.skipWaiting();
  // Delete ALL old caches on activate
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
    ).then(() => clients.claim())
  );
});

self.addEventListener('fetch', e => {
  if (e.request.mode === 'navigate') {
    e.respondWith(
      fetch(e.request)
        .then(response => {
          // ← KEY FIX: cache the fresh response every time network succeeds
          const clone = response.clone();
          caches.open(CACHE).then(c => c.put(e.request, clone));
          return response;
        })
        .catch(() => caches.match('./index.html'))
    );
    return;
  }
  e.respondWith(
    caches.match(e.request).then(r => r || fetch(e.request))
  );
});

// Rest of your message/notificationclick handlers stay exactly the same...
self.addEventListener('message', e => {
  // ... your existing code unchanged
});

self.addEventListener('notificationclick', e => {
  // ... your existing code unchanged
});
