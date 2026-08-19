export const MAX_EDGE_LABEL_CHARACTERS = 32;

export function conciseEdgeLabel(outcome: string): string {
  const branch = outcome.split(';', 1)[0].trim() || 'next';
  if (branch.length <= MAX_EDGE_LABEL_CHARACTERS) return branch;
  return `${branch.slice(0, MAX_EDGE_LABEL_CHARACTERS - 1).trimEnd()}…`;
}
