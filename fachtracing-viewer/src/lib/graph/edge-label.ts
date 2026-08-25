export const MAX_EDGE_LABEL_CHARACTERS = 32;

export function conciseEdgeLabel(outcome: string): string {
  const branch = outcome.split(';', 1)[0].trim();
  const normalized = branch.toLowerCase();
  const display = normalized === 'true' || normalized === 'yes' ? 'Yes' : normalized === 'false' || normalized === 'no' ? 'No' : branch || 'next';
  if (display.length <= MAX_EDGE_LABEL_CHARACTERS) return display;
  return `${display.slice(0, MAX_EDGE_LABEL_CHARACTERS - 1).trimEnd()}…`;
}

export function displayedEdgeLabel(outcome: string, sourceOutDegree: number, branchIndex: number): string | null {
  const branch = outcome.split(';', 1)[0].trim();
  const normalized = branch.toLowerCase();
  if (normalized === 'true' || normalized === 'yes') return 'Yes';
  if (normalized === 'false' || normalized === 'no') return 'No';
  if ((normalized === '' || normalized === 'next') && sourceOutDegree === 1) return null;
  if (normalized === '') return `Branch ${branchIndex + 1}`;
  return conciseEdgeLabel(branch);
}

export function accessibleEdgeLabel(outcome: string, visibleLabel: string | null): string {
  const raw = outcome.split(';', 1)[0].trim();
  if (!visibleLabel) return raw || 'Continuation';
  if (!raw || raw.toLowerCase() === visibleLabel.toLowerCase()) return visibleLabel;
  return `${visibleLabel} (${raw})`;
}

export function rawEdgeLabel(outcome: string): string {
  const branch = outcome.split(';', 1)[0].trim() || 'next';
  if (branch.length <= MAX_EDGE_LABEL_CHARACTERS) return branch;
  return `${branch.slice(0, MAX_EDGE_LABEL_CHARACTERS - 1).trimEnd()}…`;
}
