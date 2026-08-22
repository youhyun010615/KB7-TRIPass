import { readonly, reactive } from 'vue';

const SHOW_DELAY = 420;
const MIN_VISIBLE_TIME = 650;

const state = reactive({ visible: false });
const pending = new Set();
let showTimer = null;
let hideTimer = null;
let visibleAt = 0;

function clearTimer(timer) {
  if (timer) window.clearTimeout(timer);
}

export function beginLoading(key = Symbol('loading')) {
  pending.add(key);
  clearTimer(hideTimer);
  hideTimer = null;

  if (!state.visible && !showTimer) {
    showTimer = window.setTimeout(() => {
      showTimer = null;
      if (!pending.size) return;
      state.visible = true;
      visibleAt = Date.now();
    }, SHOW_DELAY);
  }
  return key;
}

export function endLoading(key) {
  pending.delete(key);
  if (pending.size) return;

  clearTimer(showTimer);
  showTimer = null;
  if (!state.visible) return;

  const remaining = Math.max(0, MIN_VISIBLE_TIME - (Date.now() - visibleAt));
  hideTimer = window.setTimeout(() => {
    hideTimer = null;
    if (!pending.size) state.visible = false;
  }, remaining);
}

export const loadingOverlayState = readonly(state);
