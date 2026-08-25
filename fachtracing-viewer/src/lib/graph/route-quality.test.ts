import { describe, expect, it } from 'vitest';
import { graphFixture } from './graph-fixtures';
import { bendCount, manhattanLength, measureLayoutQuality, parallelClearanceViolations } from './route-quality';

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
});
