import { describe, expect, it } from 'vitest';
import { conciseEdgeLabel, MAX_EDGE_LABEL_CHARACTERS } from './edge-label';

describe('canvas edge labels', () => {
  it('keeps a short branch token and removes a compound explanation', () => {
    expect(conciseEdgeLabel('false; returns the fallback business result')).toBe('false');
    expect(conciseEdgeLabel('next')).toBe('next');
  });

  it('bounds a long single branch token', () => {
    const label = conciseEdgeLabel('a branch token that is much too long for the graph canvas');
    expect(label.length).toBeLessThanOrEqual(MAX_EDGE_LABEL_CHARACTERS);
    expect(label.endsWith('…')).toBe(true);
  });
});
