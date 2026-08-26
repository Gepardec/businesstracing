import { readdir, readFile, realpath, stat } from 'node:fs/promises';
import { relative, resolve, sep } from 'node:path';

export interface RunImport {
  path: string;
  bytes: Buffer;
}

export async function readRunDirectory(directory: string): Promise<RunImport[]> {
  const root = await realpath(resolve(directory));
  if (!(await stat(root)).isDirectory()) throw new Error('run import path must be a directory');
  const names = (await readdir(root, { withFileTypes: true }))
    .filter((entry) => entry.isFile() && entry.name.endsWith('.decision.json'))
    .map((entry) => entry.name)
    .sort();
  const result: RunImport[] = [];
  for (const name of names) {
    const path = await realpath(resolve(root, name));
    const local = relative(root, path);
    if (local.startsWith(`..${sep}`) || local === '..') throw new Error('run path escapes the selected directory');
    result.push({ path, bytes: await readFile(path) });
  }
  return result;
}
