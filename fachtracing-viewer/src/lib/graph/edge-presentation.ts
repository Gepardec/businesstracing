export interface EdgePresentationState {
  readonly secondary: boolean;
  readonly branch: boolean;
  readonly current: boolean;
  readonly onPath: boolean;
  readonly inspected: boolean;
}

export function isQuietReference(state: EdgePresentationState): boolean {
  return state.secondary && !state.branch && !state.current && !state.onPath && !state.inspected;
}

export function shouldShowEdgeLabel(label: string, state: EdgePresentationState): boolean {
  if (!label) return false;
  return !isQuietReference(state);
}
