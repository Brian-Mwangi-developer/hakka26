import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import App from '../App';
import { applyBackspace, applyInput, formatCountdown, verify } from '../otp';

const empty = () => Array(6).fill('');

test('renders six labelled digit boxes', async () => {
  let renderer: ReactTestRenderer.ReactTestRenderer | undefined;
  await ReactTestRenderer.act(() => {
    renderer = ReactTestRenderer.create(<App />);
  });
  const labels = renderer!.root
    .findAll(node => typeof node.props.testID === 'string' && node.props.testID.startsWith('otp_digit_'))
    .map(node => node.props.accessibilityLabel);
  expect([...new Set(labels)]).toEqual([1, 2, 3, 4, 5, 6].map(n => `Digit ${n} of 6`));
  await ReactTestRenderer.act(() => renderer!.unmount());
});

test('typing a digit advances focus', () => {
  expect(applyInput(empty(), 0, '4')).toEqual({ digits: ['4', '', '', '', '', ''], focus: 1 });
});

test('pasting text fills all six boxes', () => {
  expect(applyInput(empty(), 0, 'Your code is 482913').digits.join('')).toBe('482913');
});

test('backspace on an empty box clears the previous digit', () => {
  expect(applyBackspace(['2', '1', '', '', '', ''], 2)).toEqual({ digits: ['2', '', '', '', '', ''], focus: 1 });
});

test('only 123456 verifies', () => {
  expect(verify('123456')).toBe('success');
  expect(verify('654321')).toBe('error');
});

test('countdown is formatted as 0:SS', () => {
  expect(formatCountdown(7)).toBe('Resend in 0:07');
});
