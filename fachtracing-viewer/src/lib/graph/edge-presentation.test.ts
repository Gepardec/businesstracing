import { describe, expect, it } from 'vitest';
import { isQuietReference, shouldShowEdgeLabel } from './edge-presentation';

describe('edge presentation priority', () => {
  it('keeps a secondary business branch solid and labelled', () => {
    const state = { secondary: true, feedback: false, branch: true, current: false, onPath: false, inspected: false };

    expect(isQuietReference(state)).toBe(false);
    expect(shouldShowEdgeLabel('No', state)).toBe(true);
  });

  it('keeps a forward cross-link solid because it is part of the business flow', () => {
    const forward = { secondary: true, feedback: false, branch: false, current: false, onPath: false, inspected: false };

    expect(isQuietReference(forward)).toBe(false);
    expect(shouldShowEdgeLabel('returns', forward)).toBe(true);
  });

  it('keeps only a feedback route quiet until inspection', () => {
    const quiet = { secondary: true, feedback: true, branch: false, current: false, onPath: false, inspected: false };
    const inspected = { ...quiet, inspected: true };

    expect(isQuietReference(quiet)).toBe(true);
    expect(shouldShowEdgeLabel('returns', quiet)).toBe(false);
    expect(isQuietReference(inspected)).toBe(false);
    expect(shouldShowEdgeLabel('returns', inspected)).toBe(true);
  });

  it('does not create a label for an unlabelled branch', () => {
    const branch = { secondary: true, feedback: false, branch: true, current: false, onPath: false, inspected: false };
    expect(shouldShowEdgeLabel('', branch)).toBe(false);
  });
});
