import { ContractError, type GraphModel } from './graph-contract';

export const DECISION_RECORD_SCHEMA = 'fachtracing-decision-record/v1' as const;

export interface DecisionValue {
  type: string;
  canonicalValue: string;
  displayValue: string;
}

export interface RunObservation {
  sequence: number;
  nodeId: string;
  outcome: string;
  evidence: Readonly<Record<string, DecisionValue>>;
  selectedEdgeId: string | null;
}

export interface RunModel {
  schema: typeof DECISION_RECORD_SCHEMA;
  recordId: string;
  executionId: string;
  graphId: string;
  graphVersion: number;
  startedAt: string;
  completedAt: string;
  status: 'SUCCEEDED' | 'FAILED' | 'INCOMPLETE';
  terminalStatus: string;
  completeness: string;
  finalDecision: DecisionValue | null;
  failure: { canonicalValue: string; displayValue: string } | null;
  observations: readonly RunObservation[];
  coverageGaps: readonly string[];
}

type JsonObject = Record<string, unknown>;

function object(value: unknown, path: string): JsonObject {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) throw new ContractError(`${path} must be an object`);
  return value as JsonObject;
}

function array(value: unknown, path: string): unknown[] {
  if (!Array.isArray(value)) throw new ContractError(`${path} must be an array`);
  return value;
}

function text(value: unknown, path: string): string {
  if (typeof value !== 'string' || value.trim() === '') throw new ContractError(`${path} must be a non-empty string`);
  return value;
}

function positiveInteger(value: unknown, path: string): number {
  if (!Number.isSafeInteger(value) || Number(value) < 1) throw new ContractError(`${path} must be a positive integer`);
  return Number(value);
}

function instant(value: unknown, path: string): string {
  const result = text(value, path);
  if (Number.isNaN(Date.parse(result))) throw new ContractError(`${path} must be an ISO date-time`);
  return result;
}

function value(raw: unknown, path: string): DecisionValue | null {
  if (raw === null) return null;
  const item = object(raw, path);
  return Object.freeze({
    type: text(item.type, `${path}.type`),
    canonicalValue: text(item.canonicalValue, `${path}.canonicalValue`),
    displayValue: text(item.displayValue, `${path}.displayValue`)
  });
}

export function parseDecisionRecord(input: unknown): RunModel {
  const root = object(input, 'decision record');
  if (root.schema !== DECISION_RECORD_SCHEMA) throw new ContractError('unsupported decision record schema');
  const observations = array(root.observations, 'observations').map((raw, index): RunObservation => {
    const item = object(raw, `observations[${index}]`);
    const evidenceInput = object(item.evidence, `observations[${index}].evidence`);
    const evidence: Record<string, DecisionValue> = {};
    for (const [key, rawValue] of Object.entries(evidenceInput)) {
      const parsed = value(rawValue, `observations[${index}].evidence.${key}`);
      if (parsed === null) throw new ContractError('evidence values cannot be null');
      evidence[key] = parsed;
    }
    return Object.freeze({
      sequence: positiveInteger(item.sequence, `observations[${index}].sequence`),
      nodeId: text(item.nodeId, `observations[${index}].nodeId`),
      outcome: text(item.outcome, `observations[${index}].outcome`),
      evidence: Object.freeze(evidence),
      selectedEdgeId: item.selectedEdgeId === null ? null : text(item.selectedEdgeId, `observations[${index}].selectedEdgeId`)
    });
  }).sort((left, right) => left.sequence - right.sequence);
  if (new Set(observations.map((item) => item.sequence)).size !== observations.length) throw new ContractError('observation sequences must be unique');
  const status = text(root.status, 'status');
  if (!['SUCCEEDED', 'FAILED', 'INCOMPLETE'].includes(status)) throw new ContractError('unsupported decision status');
  const failureObject = root.failure === null ? null : object(root.failure, 'failure');
  return Object.freeze({
    schema: DECISION_RECORD_SCHEMA,
    recordId: text(root.recordId, 'recordId'),
    executionId: text(root.executionId, 'executionId'),
    graphId: text(root.graphId, 'graphId'),
    graphVersion: positiveInteger(root.graphVersion, 'graphVersion'),
    startedAt: instant(root.startedAt, 'startedAt'),
    completedAt: instant(root.completedAt, 'completedAt'),
    status: status as RunModel['status'],
    terminalStatus: text(root.terminalStatus, 'terminalStatus'),
    completeness: text(root.completeness, 'completeness'),
    finalDecision: value(root.finalDecision, 'finalDecision'),
    failure: failureObject === null ? null : Object.freeze({
      canonicalValue: text(failureObject.canonicalValue, 'failure.canonicalValue'),
      displayValue: text(failureObject.displayValue, 'failure.displayValue')
    }),
    observations: Object.freeze(observations),
    coverageGaps: Object.freeze(array(root.coverageGaps, 'coverageGaps').map((item, index) => text(item, `coverageGaps[${index}]`)))
  });
}

export function parseDecisionRecordJson(json: string | Uint8Array): RunModel {
  try {
    const textValue = typeof json === 'string' ? json : new TextDecoder().decode(json);
    return parseDecisionRecord(JSON.parse(textValue));
  } catch (error) {
    if (error instanceof ContractError) throw error;
    throw new ContractError('decision record is not valid JSON');
  }
}

export function assertRunMatchesGraph(run: RunModel, graph: GraphModel): void {
  if (run.graphId !== graph.id || run.graphVersion !== graph.version) throw new ContractError('run and graph versions do not match');
  const nodeIds = new Set(graph.nodes.map((node) => node.id));
  const edgeIds = new Set(graph.edges.map((edge) => edge.id));
  for (const observation of run.observations) {
    if (!nodeIds.has(observation.nodeId)) throw new ContractError(`run references an unknown node: ${observation.nodeId}`);
    if (observation.selectedEdgeId !== null && !edgeIds.has(observation.selectedEdgeId)) {
      throw new ContractError(`run references an unknown edge: ${observation.selectedEdgeId}`);
    }
  }
}
