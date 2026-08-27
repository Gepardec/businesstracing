import type { RunModel } from './run-contract';

export interface RunSearch {
  executionId?: string;
  graphId?: string;
  status?: 'SUCCEEDED' | 'FAILED' | 'INCOMPLETE';
  completedFrom?: string;
  completedTo?: string;
  correlation?: { name: string; value: string };
  cursor?: string;
  limit?: number;
}

export interface RunSummary {
  executionId: string;
  graphId: string;
  graphVersion: number;
  decisionLabel: string;
  completedAt: string;
  status: RunModel['status'];
  finalResult: string | null;
}

export interface RunPage {
  items: RunSummary[];
  nextCursor: string | null;
}
