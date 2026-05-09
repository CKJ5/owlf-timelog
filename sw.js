const CACHE = 'owlf-v3';

self.addEventListener('install', e => {
  self.skipWaiting();
  e.waitUntil(caches.open(CACHE).then(c => c.add('./index.html')));
});

self.addEventListener('activate', e => {
  self.skipWaiting();
  e.waitUntil(clients.claim());
});

self.addEventListener('fetch', e => {
  if (e.request.mode === 'navigate') {
    e.respondWith(fetch(e.request).catch(() => caches.match('./index.html')));
    return;
  }
  e.respondWith(caches.match(e.request).then(r => r || fetch(e.request)));
});

// FIX 1+2+3: message handler — NOTIFY, CLEAR, NOTIFY_ACTIVE
self.addEventListener('message', e => {
  if (!e.data) return;

  // Show a single notification
  if (e.data.type === 'CLEAR') {
    if(Notification.permission!=='granted')return;
    const { title, body, tag } = e.data;
    e.waitUntil(self.registration.showNotification(title, { {
      body, tag,
      icon: './icon-192.png',
      badge: './badge-72.png',
      renotify: true,
      requireInteraction: false,
      silent: true
    }));
  }

  // Clear notification by tag when session stops
  if (e.data.type === 'CLEAR') {
    e.waitUntil(
      self.registration.getNotifications({ tag: e.data.tag })
        .then(ns => ns.forEach(n => n.close()))
    );
  }

  // Show active sessions as one combined notification
  if (e.data.type === 'NOTIFY_ACTIVE') {
    const { items } = e.data; // array of label strings
    if (!items || !items.length) {
      // No active sessions — clear the active notification
      e.waitUntil(
        self.registration.getNotifications({ tag: 'owlf-active' })
          .then(ns => ns.forEach(n => n.close()))
      );
      return;
    }
    e.waitUntil(self.registration.showNotification('▶ OWLF Active', {
      body: items.join(' · '),
      tag: 'owlf-active',
      icon: './icon-192.png',
      badge: './badge-72.png',
      renotify: false,
      requireInteraction: false,
      silent: true
    }));
  }
});

// FIX 3: Tap notification → open/focus app
// Also removes the "Tap to copy URL" default behavior
self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(cs => {
      for (const c of cs) {
        if (c.url && 'focus' in c) return c.focus();
      }
      if (clients.openWindow) return clients.openWindow('./');
    })
  );
});
