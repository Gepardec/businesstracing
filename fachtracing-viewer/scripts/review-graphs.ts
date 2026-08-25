import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { parseGraphDocumentJson } from '../src/lib/contracts/graph-contract';
import { formatGraphReviewTable, graphReviewExitCode, reviewGraph } from '../src/lib/graph/layout-review';

async function main(paths: readonly string[]): Promise<number> {
  if (paths.length === 0) {
    console.error('Usage: npm run review:graphs -- <graph-a.json> [graph-b.json ...]');
    return 2;
  }

  const results = [];
  for (const path of paths) {
    const contents = await readFile(path);
    const graph = parseGraphDocumentJson(contents);
    const sha256 = createHash('sha256').update(contents).digest('hex');
    results.push(await reviewGraph(graph, path, sha256));
  }

  console.log(formatGraphReviewTable(results));
  console.log('\nJSON');
  console.log(JSON.stringify(results, null, 2));
  return graphReviewExitCode(results);
}

main(process.argv.slice(2))
  .then((exitCode) => {
    process.exitCode = exitCode;
  })
  .catch((error: unknown) => {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 2;
  });
