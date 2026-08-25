import { describe, expect, it } from 'vitest';
import { graphFixture } from './graph-fixtures';
import {
  bendCount,
  evaluateLayoutQuality,
  manhattanLength,
  measureLayoutQuality,
  measureRouteDetours,
  parallelClearanceViolations
} from './route-quality';

describe('layout quality metrics', () => {
  it('measures route length and bends without sampling pixels', () => {
    const points = [{ x: 0, y: 0 }, { x: 0, y: 20 }, { x: 30, y: 20 }, { x: 30, y: 60 }];
    expect(manhattanLength(points)).toBe(90);
    expect(bendCount(points)).toBe(2);
  });

  it('reports unsafe geometry', () => {
    const graph = graphFixture('quality-fixture', ['entry', 'blocker', 'outcome'], [{ id: 'unsafe-edge', from: 'entry', to: 'outcome' }]);
    const metrics = measureLayoutQuality(graph, [
      { id: 'entry', x: 0, y: 0, width: 20, height: 20 },
      { id: 'blocker', x: 40, y: 0, width: 20, height: 20 },
      { id: 'outcome', x: 80, y: 0, width: 20, height: 20 }
    ], [{ id: 'unsafe-edge', points: [{ x: 20, y: 10 }, { x: 80, y: 10 }] }]);
    expect(metrics.unrelatedNodeIntrusions).toBe(1);
    expect(metrics.totalManhattanLength).toBe(60);
  });

  it('reports label collisions', () => {
    const graph = graphFixture('label-quality-fixture', ['entry', 'outcome'], [{ id: 'edge', from: 'entry', to: 'outcome', outcome: 'Yes' }]);
    const metrics = measureLayoutQuality(graph, [
      { id: 'entry', x: 0, y: 0, width: 100, height: 40 },
      { id: 'outcome', x: 0, y: 100, width: 100, height: 40 }
    ], [{ id: 'edge', points: [{ x: 50, y: 40 }, { x: 50, y: 100 }], labelPosition: { x: 50, y: 20 }, displayLabel: 'Yes' }]);
    expect(metrics.labelCollisions).toBe(1);
  });

  it('identifies the exact routes that violate parallel clearance', () => {
    const routes = [
      { id: 'edge-a', points: [{ x: 10, y: 0 }, { x: 10, y: 80 }] },
      { id: 'edge-b', points: [{ x: 18, y: 20 }, { x: 18, y: 100 }] },
      { id: 'edge-c', points: [{ x: 40, y: 20 }, { x: 40, y: 100 }] }
    ];
    expect(parallelClearanceViolations(routes)).toEqual(['edge-a is too close to edge-b']);
  });

  it('measures candidate-relative detours by route class', () => {
    const detours = measureRouteDetours([
      { id: 'normal', points: [{ x: 0, y: 0 }, { x: 0, y: 80 }, { x: 80, y: 80 }], shortestCandidateLength: 80 },
      { id: 'cycle', points: [{ x: 0, y: 0 }, { x: 0, y: 30 }, { x: 30, y: 30 }], shortestCandidateLength: 50, corridor: 'cycle' }
    ]);
    expect(detours[0]).toMatchObject({ routeId: 'normal', ratio: 2, avoidable: true });
    expect(detours[1]).toMatchObject({ routeId: 'cycle', ratio: 1.2, avoidable: true });
  });

  it('reports densities and named quality-gate failures', () => {
    const graph = graphFixture('density-fixture', ['entry', 'left', 'right', 'outcome'], [
      { id: 'edge-a', from: 'entry', to: 'left' },
      { id: 'edge-b', from: 'right', to: 'outcome' }
    ]);
    const routes = [
      { id: 'edge-a', points: [{ x: 0, y: 20 }, { x: 100, y: 20 }], shortestCandidateLength: 100 },
      { id: 'edge-b', points: [{ x: 50, y: 0 }, { x: 50, y: 80 }], shortestCandidateLength: 60 }
    ];
    const metrics = measureLayoutQuality(graph, [
      { id: 'entry', x: -20, y: 0, width: 20, height: 20 },
      { id: 'left', x: 100, y: 10, width: 20, height: 20 },
      { id: 'right', x: 40, y: -20, width: 20, height: 20 },
      { id: 'outcome', x: 40, y: 80, width: 20, height: 20 }
    ], routes);
    expect(metrics.crossingDensity).toBe(0.5);
    expect(metrics.maximumNormalDetourRatio).toBeCloseTo(4 / 3);
    const failures = evaluateLayoutQuality({ ...metrics, crossingDensity: 0.75 }, routes);
    expect(failures.map((failure) => failure.metric)).toContain('crossingDensity');
    expect(failures.find((failure) => failure.metric === 'avoidableDetours')?.evidence).toContain('edge-b');
  });
});
