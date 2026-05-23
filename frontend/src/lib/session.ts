export const SESSION_EXPIRED_EVENT = 'resolvehub:session-expired';
export const SESSION_EXPIRED_NOTICE_KEY = 'resolvehub.sessionExpiredNotice';
export const SESSION_EXPIRED_MESSAGE = 'Your session expired. Please sign in again.';

export function markSessionExpiredNotice(): void {
  sessionStorage.setItem(SESSION_EXPIRED_NOTICE_KEY, '1');
}

export function consumeSessionExpiredNotice(): boolean {
  const marked = sessionStorage.getItem(SESSION_EXPIRED_NOTICE_KEY) === '1';
  if (marked) {
    sessionStorage.removeItem(SESSION_EXPIRED_NOTICE_KEY);
  }
  return marked;
}

export function hasSessionExpiredNotice(): boolean {
  return sessionStorage.getItem(SESSION_EXPIRED_NOTICE_KEY) === '1';
}
