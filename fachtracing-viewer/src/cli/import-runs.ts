import { closeDatabase } from '../lib/server/database.server';
import { RunImportRepository } from '../lib/server/run-import-repository.server';
import { readRunDirectory } from '../lib/server/run-import.server';

function directoryArgument(args: readonly string[]): string {
  const index = args.indexOf('--directory');
  const value = index >= 0 ? args[index + 1] : undefined;
  if (!value || value.startsWith('--')) throw new Error('usage: npm run import-runs -- --directory <path>');
  return value;
}

try {
  const items = await readRunDirectory(directoryArgument(process.argv.slice(2)));
  const imported = await new RunImportRepository().importRuns(items);
  process.stdout.write(`Imported ${imported} run file${imported === 1 ? '' : 's'}.\n`);
} catch (error) {
  process.stderr.write(`${error instanceof Error ? error.message : 'Run import failed.'}\n`);
  process.exitCode = 1;
} finally {
  await closeDatabase();
}
