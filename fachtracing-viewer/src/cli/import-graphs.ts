import { closeDatabase } from '../lib/server/database.server';
import { GraphCatalogRepository } from '../lib/server/graph-catalog-repository.server';
import { readGraphDirectory } from '../lib/server/graph-import.server';

function directoryArgument(args: readonly string[]): string {
  const index = args.indexOf('--directory');
  const value = index >= 0 ? args[index + 1] : undefined;
  if (!value || value.startsWith('--')) throw new Error('usage: npm run import-graphs -- --directory <path>');
  return value;
}

try {
  const items = await readGraphDirectory(directoryArgument(process.argv.slice(2)));
  const imported = await new GraphCatalogRepository().importGraphs(items);
  process.stdout.write(`Imported ${imported} graph file${imported === 1 ? '' : 's'}.\n`);
} catch (error) {
  process.stderr.write(`${error instanceof Error ? error.message : 'Graph import failed.'}\n`);
  process.exitCode = 1;
} finally {
  await closeDatabase();
}
