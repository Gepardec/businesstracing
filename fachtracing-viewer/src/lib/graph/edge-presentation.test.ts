import { describe, expect, it } from 'vitest';
import { isQuietReference, shouldShowEdgeLabel } from './edge-presentation';

describe('edge presentation priority', () => {
  it('keeps a secondary business branch solid and labelled', () => {
    const state = { secondary: true, branch: true, current: false, onPath: false, inspected: false };

    expect(isQuietReference(state)).toBe(false);
    expect(shouldShowEdgeLabel('No', state)).toBe(true);
  });

  it('keeps a non-branch cross-link quiet until inspection', () => {
    const quiet = { secondary: true, branch: false, current: false, onPath: false, inspected: false };
    const inspected = { ...quiet, inspected: true };

    expect(isQuietReference(quiet)).toBe(true);
    expect(shouldShowEdgeLabel('returns', quiet)).toBe(false);
    expect(isQuietReference(inspected)).toBe(false);
    expect(shouldShowEdgeLabel('returns', inspected)).toBe(true);
  });

  it('does not create a label for an unlabelled branch', () => {
    const branch = { secondary: true, branch: true, current: false, onPath: false, inspected: false };
    expect(shouldShowEdgeLabel('', branch)).toBe(false);
  });
});
