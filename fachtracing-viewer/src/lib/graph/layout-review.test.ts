import { describe, expect, it } from 'vitest';
import { parseBusinessGraph } from '$contracts/graph-contract';
import type { GraphReviewResult } from './layout-review';
import { formatGraphReviewTable, graphReviewExitCode, responsivenessFailure, reviewGraph } from './layout-review';
import { chainFixture } from './graph-fixtures';

function result(failures: GraphReviewResult['failures']): GraphReviewResult {
  return {
    source: 'generated.json',
    schema: 'fachtracing-developer-graph/v1',
    graphId: 'generated',
    label: 'generated graph',
    nodeCount: 2,
    edgeCount: 1,
    durationMs: 12,
    width: 232,
    height: 280,
    placementProfileId: 'generated',
    metrics: {
      nodeOverlaps: 0,
      unrelatedNodeIntrusions: 0,
      labelCollisions: 0,
      avoidableCrossings: 0,
      unavoidableCrossings: 0,
      totalManhattanLength: 96,
      totalBends: 0,
      backtrackingDistance: 0,
      longEdgeCorridorViolations: 0,
      branchRegionViolations: 0,
      crossingDensity: 0,
      parallelCorridorDensity: 0,
      maximumNormalDetourRatio: 1,
      maximumLongDetourRatio: 1,
      avoidableDetours: 0
    },
    failures
  };
}

describe('graph review', () => {
  it('reviews a generated graph with the production layout engine', async () => {
    const review = await reviewGraph(chainFixture(3), 'generated.json');
    expect(review.nodeCount).toBe(3);
    expect(review.edgeCount).toBe(2);
    expect(review.width).toBeGreaterThan(0);
    expect(review.metrics.crossingDensity).toBe(0);
  });

  it('reviews the business V1 contract through the same engine', async () => {
    const review = await reviewGraph(parseBusinessGraph({
      schema: 'fachtracing-business-graph/v1',
      version: 1,
      graphId: 'business-generated',
      decision: 'generated decision',
      completeness: 'COMPLETE',
      entryNodeIds: ['entry'],
      nodes: [
        { id: 'entry', kind: 'ACTION', label: 'start' },
        { id: 'outcome', kind: 'RESULT', label: 'stop' }
      ],
      edges: [{ id: 'edge', from: 'entry', to: 'outcome', outcome: 'next' }]
    }), 'business.json');
    expect(review.schema).toBe('fachtracing-business-graph/v1');
    expect(review.failures).toEqual([]);
  });

  it('returns a failing status and names failed metrics', () => {
    const failed = result([{
      metric: 'crossingDensity',
      actual: 0.75,
      maximum: 0.5,
      evidence: [],
      message: 'crossingDensity is 0.750; maximum is 0.500'
    }]);
    expect(graphReviewExitCode([result([])])).toBe(0);
    expect(graphReviewExitCode([failed])).toBe(1);
    expect(formatGraphReviewTable([failed])).toContain('crossingDensity');
  });

  it('enforces the local four-second POC gate only for the evidence-size range', () => {
    expect(responsivenessFailure(4_001, 45)?.metric).toBe('durationMs');
    expect(responsivenessFailure(4_000, 45)).toBeNull();
    expect(responsivenessFailure(8_000, 250)).toBeNull();
  });
});
