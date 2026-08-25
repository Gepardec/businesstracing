import { describe, expect, it } from 'vitest';
import { cycleFixture, graphFixture, multipleEntryFixture } from './graph-fixtures';
import { createGraphPresentation, explainGraph } from './graph-presentation';

describe('readable graph presentation', () => {
  it('groups only straight unlabelled action sequences and parallel connections', () => {
    const graph = graphFixture('readable-map', [
      { id: 'entry', kind: 'ENTRY', label: 'request' },
      { id: 'prepare', kind: 'COMPUTATION', label: 'prepare data' },
      { id: 'calculate', kind: 'COMPUTATION', label: 'calculate score' },
      { id: 'store', kind: 'COMPUTATION', label: 'store score' },
      { id: 'rule', kind: 'PREDICATE', label: 'score is sufficient' },
      { id: 'outcome', kind: 'OUTCOME', label: 'approved' }
    ], [
      { id: 'edge-entry', from: 'entry', to: 'prepare', outcome: '' },
      { id: 'edge-prepare', from: 'prepare', to: 'calculate', outcome: '' },
      { id: 'edge-calculate', from: 'calculate', to: 'store', outcome: '' },
      { id: 'edge-rule', from: 'store', to: 'rule', outcome: '' },
      { id: 'edge-yes', from: 'rule', to: 'outcome', outcome: 'yes' },
      { id: 'edge-no', from: 'rule', to: 'outcome', outcome: 'no' }
    ]);

    const presentation = createGraphPresentation(graph);
    const sequence = [...presentation.nodes.values()].find((node) => node.memberNodeIds.length === 3)!;
    const combined = presentation.graph.edges.find((edge) => edge.from === 'rule')!;

    expect(presentation.reduced).toBe(true);
    expect(presentation.graph.nodes).toHaveLength(4);
    expect(presentation.graph.edges).toHaveLength(3);
    expect(sequence.memberNodeIds).toEqual(['prepare', 'calculate', 'store']);
    expect(presentation.presentationNodeIdByOriginalNodeId.get('calculate')).toBe(sequence.id);
    expect(presentation.presentationEdgeIdByOriginalEdgeId.get('edge-prepare')).toBeNull();
    expect(combined.outcome).toBe('Any result');
    expect(presentation.edges.get(combined.id)?.memberEdgeIds).toEqual(['edge-yes', 'edge-no']);
  });

  it('does not hide a labelled action transition, branch, or entry', () => {
    const graph = graphFixture('unsafe-sequence', [
      { id: 'entry', kind: 'COMPUTATION' },
      { id: 'first', kind: 'COMPUTATION' },
      { id: 'second', kind: 'COMPUTATION' },
      { id: 'branch', kind: 'COMPUTATION' },
      { id: 'left', kind: 'COMPUTATION' },
      { id: 'right', kind: 'COMPUTATION' },
      { id: 'outcome', kind: 'OUTCOME' }
    ], [
      { from: 'entry', to: 'first', outcome: '' },
      { from: 'first', to: 'second', outcome: 'confirmed' },
      { from: 'second', to: 'branch', outcome: '' },
      { from: 'branch', to: 'left', outcome: 'left' },
      { from: 'branch', to: 'right', outcome: 'right' },
      { from: 'left', to: 'outcome', outcome: '' },
      { from: 'right', to: 'outcome', outcome: '' }
    ], ['entry']);

    const presentation = createGraphPresentation(graph);
    expect(presentation.nodes.get('entry')?.memberNodeIds).toEqual(['entry']);
    expect(presentation.nodes.get('first')?.memberNodeIds).toEqual(['first']);
    expect(presentation.nodes.get('branch')?.memberNodeIds).toEqual(['branch']);
  });

  it('groups rules only when yes and no lead to the same next rule', () => {
    const graph = graphFixture('rule-sequence', [
      { id: 'entry', kind: 'ENTRY' },
      { id: 'first', kind: 'PREDICATE' },
      { id: 'second', kind: 'PREDICATE' },
      { id: 'third', kind: 'PREDICATE' },
      { id: 'outcome', kind: 'OUTCOME' }
    ], [
      { from: 'entry', to: 'first', outcome: '' },
      { id: 'first-yes', from: 'first', to: 'second', outcome: 'yes' },
      { id: 'first-no', from: 'first', to: 'second', outcome: 'no' },
      { id: 'second-yes', from: 'second', to: 'third', outcome: 'yes' },
      { id: 'second-no', from: 'second', to: 'third', outcome: 'no' },
      { from: 'third', to: 'outcome', outcome: '' }
    ]);

    const presentation = createGraphPresentation(graph);
    const sequence = [...presentation.nodes.values()].find((node) => node.memberNodeIds.length > 1)!;
    expect(sequence.memberNodeIds).toEqual(['first', 'second', 'third']);
    expect(presentation.graph.nodes.find((node) => node.id === sequence.id)?.kind).toBe('PREDICATE');
    expect(presentation.presentationEdgeIdByOriginalEdgeId.get('first-yes')).toBeNull();
    expect(presentation.presentationEdgeIdByOriginalEdgeId.get('second-no')).toBeNull();
  });

  it('groups a guard sequence that has one shared exit', () => {
    const graph = graphFixture('guard-sequence', [
      { id: 'entry', kind: 'ENTRY' },
      { id: 'email', kind: 'PREDICATE', label: 'email exists' },
      { id: 'username', kind: 'PREDICATE', label: 'username exists' },
      { id: 'enabled', kind: 'PREDICATE', label: 'account is enabled' },
      { id: 'matched', kind: 'OUTCOME', label: 'filter matched' },
      { id: 'none', kind: 'OUTCOME', label: 'no filter matched' }
    ], [
      { from: 'entry', to: 'email', outcome: '' },
      { id: 'email-match', from: 'email', to: 'matched', outcome: 'yes' },
      { id: 'email-next', from: 'email', to: 'username', outcome: 'no' },
      { id: 'username-match', from: 'username', to: 'matched', outcome: 'yes' },
      { id: 'username-next', from: 'username', to: 'enabled', outcome: 'no' },
      { id: 'enabled-match', from: 'enabled', to: 'matched', outcome: 'yes' },
      { id: 'enabled-next', from: 'enabled', to: 'none', outcome: 'no' }
    ]);

    const presentation = createGraphPresentation(graph);
    const sequence = [...presentation.nodes.values()].find((node) => node.memberNodeIds.length === 3)!;
    const exit = presentation.graph.edges.find((edge) => edge.to === 'matched')!;

    expect(sequence.memberNodeIds).toEqual(['email', 'username', 'enabled']);
    expect(presentation.presentationEdgeIdByOriginalEdgeId.get('username-next')).toBeNull();
    expect(exit.from).toBe(sequence.id);
    expect(presentation.edges.get(exit.id)?.memberEdgeIds).toEqual(['email-match', 'username-match', 'enabled-match']);
  });

  it('keeps a material binary decision separate', () => {
    const graph = graphFixture('binary-decision', [
      { id: 'entry', kind: 'ENTRY' },
      { id: 'choice', kind: 'PREDICATE' },
      { id: 'left', kind: 'PREDICATE' },
      { id: 'right', kind: 'PREDICATE' },
      { id: 'approved', kind: 'OUTCOME' },
      { id: 'rejected', kind: 'OUTCOME' }
    ], [
      { from: 'entry', to: 'choice', outcome: '' },
      { from: 'choice', to: 'left', outcome: 'yes' },
      { from: 'choice', to: 'right', outcome: 'no' },
      { from: 'left', to: 'approved', outcome: 'yes' },
      { from: 'left', to: 'rejected', outcome: 'no' },
      { from: 'right', to: 'approved', outcome: 'yes' },
      { from: 'right', to: 'rejected', outcome: 'no' }
    ]);

    const presentation = createGraphPresentation(graph);
    expect(presentation.nodes.get('choice')?.memberNodeIds).toEqual(['choice']);
    expect(presentation.nodes.get('left')?.memberNodeIds).toEqual(['left']);
    expect(presentation.nodes.get('right')?.memberNodeIds).toEqual(['right']);
  });

  it('restores exact source identity in full detail', () => {
    const graph = cycleFixture();
    const presentation = createGraphPresentation(graph, 'full');
    expect(presentation.graph).toBe(graph);
    expect([...presentation.presentationNodeIdByOriginalNodeId]).toEqual(graph.nodes.map((node) => [node.id, node.id]));
    expect([...presentation.presentationEdgeIdByOriginalEdgeId]).toEqual(graph.edges.map((edge) => [edge.id, edge.id]));
  });

  it('explains starts, first alternatives, results, and cycles in at most three sentences', () => {
    const cycle = explainGraph(cycleFixture());
    expect(cycle.sentences).toHaveLength(3);
    expect(cycle.sentences.every((sentence) => (sentence.match(/[.!?](?:\s|$)/g) ?? []).length === 1)).toBe(true);
    expect(cycle.sentences.join(' ')).toContain('“entry”');
    expect(cycle.sentences.join(' ')).toContain('“outcome”');
    expect(cycle.sentences.join(' ')).toContain('return to an earlier check');

    const multipleEntries = explainGraph(multipleEntryFixture());
    expect(multipleEntries.sentences[0]).toContain('2 entry points');
    expect(multipleEntries.sentences[0]).toContain('“entry a”');
    expect(multipleEntries.sentences).toHaveLength(3);
  });
});
