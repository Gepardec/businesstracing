import { describe, expect, it } from 'vitest';
import { MAX_GRAPH_FILE_BYTES, parseGraphFile, type BrowserGraphFile } from './graph-file';

function generatedDocument(nodeCount = 3): object {
  return {
    schema: 'fachtracing-developer-graph/v1',
    sourceOrigins: [{ id: 'generated', kind: 'GENERATED', identity: 'unit test', checksum: 'fixture' }],
    sourceFiles: [],
    graph: {
      id: `generated-${nodeCount}`, version: 1, label: 'generated preview graph', entryNodeId: 'node-0', completeness: 'COMPLETE',
      nodes: Array.from({ length: nodeCount }, (_, index) => ({
        id: `node-${index}`, kind: index === 0 ? 'ENTRY' : index === nodeCount - 1 ? 'OUTCOME' : 'COMPUTATION',
        label: `generated node ${index}`, attributes: {}
      })),
      edges: Array.from({ length: nodeCount - 1 }, (_, index) => ({
        id: `edge-${index}`, from: `node-${index}`, to: `node-${index + 1}`, outcome: 'next'
      })),
      coverageGaps: []
    }
  };
}

function file(name: string, content: string, size = new TextEncoder().encode(content).byteLength): BrowserGraphFile {
  const bytes = new TextEncoder().encode(content);
  return { name, size, arrayBuffer: async () => bytes.buffer };
}

describe('browser graph file adapter', () => {
  it('parses a generated developer graph JSON file', async () => {
    const graph = await parseGraphFile(file('generated.JSON', JSON.stringify(generatedDocument(4))));
    expect(graph.label).toBe('generated preview graph');
    expect(graph.nodes).toHaveLength(4);
  });

  it('rejects invalid file boundaries before rendering', async () => {
    await expect(parseGraphFile(file('graph.txt', '{}'))).rejects.toThrow(/\.json/);
    await expect(parseGraphFile(file('graph.json', '', 0))).rejects.toThrow(/empty/);
    await expect(parseGraphFile(file('graph.json', '{}', MAX_GRAPH_FILE_BYTES + 1))).rejects.toThrow(/5 MiB/);
    await expect(parseGraphFile(file('graph.json', '{'))).rejects.toThrow(/not valid JSON/);
    await expect(parseGraphFile(file('graph.json', '{}'))).rejects.toThrow(/schema/);
  });
});
