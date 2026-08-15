import { fileURLToPath, URL } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueDevTools from 'vite-plugin-vue-devtools';
import tailwindcss from '@tailwindcss/vite';

// .env 파일을 수동으로 읽고 파싱하는 함수
function loadEnvManual(filePath) {
  const env = {};
  if (fs.existsSync(filePath)) {
    const content = fs.readFileSync(filePath, 'utf8');
    content.split(/\r?\n/).forEach((line) => {
      const match = line.match(/^\s*([^=]+)\s*=\s*(.*)\s*$/);
      if (match) {
        env[match[1].trim()] = match[2].trim();
      }
    });
  }
  return env;
}

const env = loadEnvManual(path.resolve(__dirname, '.env'));

// https://vite.dev/config/
export default defineConfig({
  define: {
    'import.meta.env.VITE_FIREBASE_API_KEY': JSON.stringify(env.VITE_FIREBASE_API_KEY),
    'import.meta.env.VITE_FIREBASE_AUTH_DOMAIN': JSON.stringify(env.VITE_FIREBASE_AUTH_DOMAIN),
    'import.meta.env.VITE_FIREBASE_PROJECT_ID': JSON.stringify(env.VITE_FIREBASE_PROJECT_ID),
    'import.meta.env.VITE_FIREBASE_STORAGE_BUCKET': JSON.stringify(env.VITE_FIREBASE_STORAGE_BUCKET),
    'import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID': JSON.stringify(env.VITE_FIREBASE_MESSAGING_SENDER_ID),
    'import.meta.env.VITE_FIREBASE_APP_ID': JSON.stringify(env.VITE_FIREBASE_APP_ID),
    'import.meta.env.VITE_FIREBASE_MEASUREMENT_ID': JSON.stringify(env.VITE_FIREBASE_MEASUREMENT_ID),
    'import.meta.env.VITE_FIREBASE_VAPID_KEY': JSON.stringify(env.VITE_FIREBASE_VAPID_KEY),
  },
  plugins: [
    vue(),
    vueDevTools(),
    tailwindcss(),
    {
      name: 'replace-sw-env',
      writeBundle() {
        const swPath = path.resolve(__dirname, 'dist/firebase-messaging-sw.js');
        if (fs.existsSync(swPath)) {
          let content = fs.readFileSync(swPath, 'utf8');
          content = content
            .replace(/__VITE_FIREBASE_API_KEY__/g, env.VITE_FIREBASE_API_KEY || '')
            .replace(/__VITE_FIREBASE_AUTH_DOMAIN__/g, env.VITE_FIREBASE_AUTH_DOMAIN || '')
            .replace(/__VITE_FIREBASE_PROJECT_ID__/g, env.VITE_FIREBASE_PROJECT_ID || '')
            .replace(/__VITE_FIREBASE_STORAGE_BUCKET__/g, env.VITE_FIREBASE_STORAGE_BUCKET || '')
            .replace(/__VITE_FIREBASE_MESSAGING_SENDER_ID__/g, env.VITE_FIREBASE_MESSAGING_SENDER_ID || '')
            .replace(/__VITE_FIREBASE_APP_ID__/g, env.VITE_FIREBASE_APP_ID || '')
            .replace(/__VITE_FIREBASE_MEASUREMENT_ID__/g, env.VITE_FIREBASE_MEASUREMENT_ID || '');
          fs.writeFileSync(swPath, content);
        }
      },
    },
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
});
