import { ContractError, parseDeveloperGraphJson, type GraphModel } from '$contracts/graph-contract';

export const MAX_GRAPH_FILE_BYTES = 5 * 1024 * 1024;

export interface BrowserGraphFile {
  readonly name: string;
  readonly size: number;
  arrayBuffer(): Promise<ArrayBuffer>;
}

export async function parseGraphFile(file: BrowserGraphFile): Promise<GraphModel> {
  if (!file.name.toLowerCase().endsWith('.json')) {
    throw new ContractError('Select a JSON file with a .json name.');
  }
  if (file.size === 0) throw new ContractError('The selected JSON file is empty.');
  if (file.size > MAX_GRAPH_FILE_BYTES) {
    throw new ContractError('The selected JSON file is larger than the 5 MiB limit.');
  }
  return parseDeveloperGraphJson(new Uint8Array(await file.arrayBuffer()));
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KiB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MiB`;
}
