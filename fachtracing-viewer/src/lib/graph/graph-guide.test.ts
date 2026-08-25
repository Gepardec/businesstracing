import { describe, expect, it } from 'vitest';
import { graphFixture } from './graph-fixtures';
import { graphGuideContext } from './graph-guide';
import { createGraphPresentation } from './graph-presentation';

describe('graph explanation guide', () => {
  it('uses target labels for unlabeled alternatives without inventing outcomes', () => {
    const graph = graphFixture('guide', [
      { id: 'entry', kind: 'COMPUTATION', label: 'choose route' },
      { id: 'left', label: 'use local route' },
      { id: 'right', label: 'use express route' }
    ], [
      { from: 'entry', to: 'left', outcome: '' },
      { from: 'entry', to: 'right', outcome: '' }
    ]);
    const context = graphGuideContext(createGraphPresentation(graph), 'entry')!;
    expect(context.outgoing.map((item) => item.outcome)).toEqual([null, null]);
    expect(context.outgoing.map((item) => item.nodeLabel)).toEqual(['use local route', 'use express route']);
  });

  it('exposes every member of a readable sequence in order', () => {
    const graph = graphFixture('sequence-guide', [
      { id: 'entry', kind: 'ENTRY' },
      { id: 'first', kind: 'COMPUTATION', label: 'prepare request' },
      { id: 'second', kind: 'COMPUTATION', label: 'calculate result' },
      { id: 'third', kind: 'COMPUTATION', label: 'store result' },
      { id: 'outcome', kind: 'OUTCOME' }
    ], [
      { from: 'entry', to: 'first', outcome: '' },
      { from: 'first', to: 'second', outcome: '' },
      { from: 'second', to: 'third', outcome: '' },
      { from: 'third', to: 'outcome', outcome: '' }
    ]);
    const presentation = createGraphPresentation(graph);
    const sequence = [...presentation.nodes.values()].find((item) => item.memberNodeIds.length === 3)!;
    expect(graphGuideContext(presentation, sequence.id)?.memberLabels).toEqual([
      'prepare request', 'calculate result', 'store result'
    ]);
  });
});
