import { describe, expect, it } from 'vitest';
import { accessibleEdgeLabel, conciseEdgeLabel, displayedEdgeLabel, MAX_EDGE_LABEL_CHARACTERS } from './edge-label';

describe('canvas edge labels', () => {
  it('keeps a short branch token and removes a compound explanation', () => {
    expect(conciseEdgeLabel('false; returns the fallback business result')).toBe('No');
    expect(conciseEdgeLabel('next')).toBe('next');
  });

  it('bounds a long single branch token', () => {
    const label = conciseEdgeLabel('a branch token that is much too long for the graph canvas');
    expect(label.length).toBeLessThanOrEqual(MAX_EDGE_LABEL_CHARACTERS);
    expect(label.endsWith('…')).toBe(true);
  });

  it('uses business-facing Boolean labels without changing raw evidence', () => {
    expect(displayedEdgeLabel('true; returns accepted', 2, 0)).toBe('Yes');
    expect(displayedEdgeLabel('false', 2, 1)).toBe('No');
    expect(accessibleEdgeLabel('false', 'No')).toBe('No (false)');
  });

  it('does not invent labels for unlabeled continuations', () => {
    expect(displayedEdgeLabel('next', 1, 0)).toBeNull();
    expect(displayedEdgeLabel('', 1, 0)).toBeNull();
    expect(displayedEdgeLabel('', 2, 1)).toBeNull();
  });
});
