import { describe, expect, it } from 'vitest';
import { roundedOrthogonalPath, routeLabelPosition, routePointAtFraction } from './edge-route';

describe('edge route projection', () => {
  it('keeps orthogonal sections and rounds only their corners', () => {
    const points = [{ x: 10, y: 10 }, { x: 10, y: 60 }, { x: 80, y: 60 }, { x: 80, y: 100 }];
    expect(roundedOrthogonalPath(points, 8)).toBe('M 10 10 L 10 52 Q 10 60 18 60 L 72 60 Q 80 60 80 68 L 80 100');
  });

  it('places labels by route length instead of endpoint distance', () => {
    const points = [{ x: 0, y: 0 }, { x: 0, y: 100 }, { x: 20, y: 100 }];
    expect(routePointAtFraction(points, 0.5)).toEqual({ x: 0, y: 60 });
    expect(routeLabelPosition(points, 0.5, -10)).toEqual({ x: 12, y: 60 });
  });
});
