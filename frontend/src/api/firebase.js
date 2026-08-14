import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage } from 'firebase/messaging';

// Firebase 설정 (환경변수 관리 권장)
const firebaseConfig = {
  apiKey: 'AIzaSyAyChrDytnSp7j2eKFGitgw0uYWZm469mM',
  authDomain: 'kb7-tripass.firebaseapp.com',
  projectId: 'kb7-tripass',
  storageBucket: 'kb7-tripass.firebasestorage.app',
  messagingSenderId: '233597599125',
  appId: '1:233597599125:web:4cb5328fb2616a1028ca9c',
  measurementId: 'G-V80T73Q6RJ',
};

const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);

// FCM 토큰 발급 함수
export const requestFcmToken = async () => {
  try {
    const token = await getToken(messaging, {
      vapidKey:
        'BBUDPSNH0R3JsrZJYrvgl19NeofQWjKbHgXwQxNpebPfmhZnUP0vG0t98SGKdNatNpKfo_nK-Hwq1KQuFqbepoI', // Firebase 콘솔에서 생성한 VAPID 키
    });
    if (token) {
      return token;
    } else {
      console.log('토큰을 생성할 수 없습니다.');
      return null;
    }
  } catch (error) {
    console.error('FCM 토큰 발급 에러:', error);
    return null;
  }
};

export { messaging, onMessage };
