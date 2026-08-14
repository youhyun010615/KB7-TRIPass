importScripts(
  'https://www.gstatic.com/firebasejs/10.8.0/firebase-app-compat.js',
);
importScripts(
  'https://www.gstatic.com/firebasejs/10.8.0/firebase-messaging-compat.js',
);

// Firebase 프로젝트 설정 (프론트엔드와 동일하게 설정)
firebase.initializeApp({
  apiKey: 'AIzaSyAyChrDytnSp7j2eKFGitgw0uYWZm469mM',
  authDomain: 'kb7-tripass.firebaseapp.com',
  projectId: 'kb7-tripass',
  storageBucket: '://appspot.com',
  messagingSenderId: '233597599125',
  appId: '1:233597599125:web:4cb5328fb2616a1028ca9c',
  measurementId: 'G-V80T73Q6RJ',
});

const messaging = firebase.messaging();

// 백그라운드 메시지 수신 핸들러
messaging.onBackgroundMessage((payload) => {
  console.log(
    '[firebase-messaging-sw.js] Received background message ',
    payload,
  );
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/favicon.ico', // 프로젝트 아이콘 경로
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});
