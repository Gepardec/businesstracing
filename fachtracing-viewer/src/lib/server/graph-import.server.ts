import { readdir, readFile, realpath, stat } from 'node:fs/promises';
import { extname, relative, resolve, sep } from 'node:path';
import type { GraphImport } from './graph-catalog-repository.server';

export async function readGraphDirectory(directory: string): Promise<GraphImport[]> {
  const root = await realpath(resolve(directory));
  if (!(await stat(root)).isDirectory()) throw new Error('graph import path must be a directory');
  const names = (await readdir(root, { withFileTypes: true }))
    .filter((entry) => entry.isFile() && extname(entry.name).toLowerCase() === '.json')
    .map((entry) => entry.name)
    .sort();
  const result: GraphImport[] = [];
  for (const name of names) {
    const path = await realpath(resolve(root, name));
    const local = relative(root, path);
    if (local.startsWith(`..${sep}`) || local === '..') throw new Error('graph path escapes the selected directory');
    result.push({ path, bytes: await readFile(path) });
  }
  return result;
}
