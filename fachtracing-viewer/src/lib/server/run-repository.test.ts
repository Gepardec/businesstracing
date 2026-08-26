import { describe, expect, it } from 'vitest';
import { parseRunSearch } from './run-repository.server';

describe('run search contract', () => {
  it('accepts generic exact correlations and bounded filters', () => {
    expect(parseRunSearch({ correlation: { name: 'routeId', value: 'route-17' }, limit: 25 })).toEqual({
      correlation: { name: 'routeId', value: 'route-17' }, limit: 25
    });
  });

  it('rejects unknown fields and partial correlations', () => {
    expect(() => parseRunSearch({ customerId: '17' })).toThrow(/unknown/);
    expect(() => parseRunSearch({ correlation: { name: 'routeId' } })).toThrow(/invalid/);
  });

  it('rejects oversized and invalid values', () => {
    expect(() => parseRunSearch({ limit: 51 })).toThrow(/limit/);
    expect(() => parseRunSearch({ executionId: 'x'.repeat(201) })).toThrow(/executionId/);
  });
});
