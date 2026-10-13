const VERSION='haftehnegar-pwa-v1-20261013';
const APP_FILES=['./','./index.html','./manifest.webmanifest','./icon-192.png','./icon-512.png'];
self.addEventListener('install',event=>{
 event.waitUntil(caches.open(VERSION).then(cache=>cache.addAll(APP_FILES)).then(()=>self.skipWaiting()));
});
self.addEventListener('activate',event=>{
 event.waitUntil(Promise.all([caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('haftehnegar-pwa-')&&key!==VERSION).map(key=>caches.delete(key)))),self.clients.claim()]));
});
self.addEventListener('fetch',event=>{
 const req=event.request;
 if(req.method!=='GET')return;
 const url=new URL(req.url);
 if(url.origin!==self.location.origin)return; // External calendar occasions are optional online-only.
 if(req.mode==='navigate'){
   event.respondWith(fetch(req).then(resp=>{if(resp.ok){const copy=resp.clone();event.waitUntil(caches.open(VERSION).then(c=>c.put('./index.html',copy)))}return resp}).catch(()=>caches.match('./index.html')));
   return;
 }
 event.respondWith(caches.match(req).then(cached=>cached||fetch(req).then(resp=>{if(resp.ok&&['script','style','image','manifest'].includes(req.destination)){const copy=resp.clone();event.waitUntil(caches.open(VERSION).then(c=>c.put(req,copy)))}return resp})));
});
