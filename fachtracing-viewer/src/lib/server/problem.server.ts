import { json } from '@sveltejs/kit';

export function problem(status: number, title: string): Response {
  return json({ type: 'about:blank', title, status }, {
    status,
    headers: { 'cache-control': 'no-store', 'content-type': 'application/problem+json' }
  });
}

export function noStoreJson(data: unknown, init: ResponseInit = {}): Response {
  const headers = new Headers(init.headers);
  headers.set('cache-control', 'no-store');
  return json(data, { ...init, headers });
}
