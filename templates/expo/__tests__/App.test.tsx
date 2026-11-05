import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import App, { APP_NAME } from '../App';

test('renders the app name', async () => {
  let renderer: ReactTestRenderer.ReactTestRenderer | undefined;
  await ReactTestRenderer.act(() => {
    renderer = ReactTestRenderer.create(<App />);
  });
  expect(JSON.stringify(renderer!.toJSON())).toContain(APP_NAME);
  await ReactTestRenderer.act(() => renderer!.unmount());
});
