export const OTP_LENGTH = 6;
export const RESEND_SECONDS = 30;
export const VALID_CODE = '123456';

export type Status = 'idle' | 'success' | 'error';

export function applyInput(digits: string[], index: number, text: string): { digits: string[]; focus: number } {
  const numbers = text.replace(/\D/g, '');
  if (!numbers) {
    return { digits, focus: index };
  }
  if (numbers.length >= OTP_LENGTH || numbers.length !== text.length) {
    const pasted = numbers.slice(0, OTP_LENGTH).split('');
    const next = Array.from({ length: OTP_LENGTH }, (_, i) => pasted[i] ?? '');
    return { digits: next, focus: Math.min(pasted.length, OTP_LENGTH - 1) };
  }
  const next = [...digits];
  numbers
    .slice(0, OTP_LENGTH - index)
    .split('')
    .forEach((digit, offset) => {
      next[index + offset] = digit;
    });
  return { digits: next, focus: Math.min(index + numbers.length, OTP_LENGTH - 1) };
}

export function applyBackspace(digits: string[], index: number): { digits: string[]; focus: number } {
  const next = [...digits];
  if (next[index]) {
    next[index] = '';
    return { digits: next, focus: index };
  }
  if (index === 0) {
    return { digits: next, focus: 0 };
  }
  next[index - 1] = '';
  return { digits: next, focus: index - 1 };
}

export function verify(code: string): Status {
  return code === VALID_CODE ? 'success' : 'error';
}

export function formatCountdown(seconds: number): string {
  return `Resend in 0:${String(seconds).padStart(2, '0')}`;
}
