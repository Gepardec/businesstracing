import pg from 'pg';

const { Pool } = pg;

let pool: pg.Pool | undefined;

export function database(): pg.Pool {
  if (pool) return pool;
  pool = new Pool({
    connectionString: process.env.FACHTRACING_DATABASE_URL,
    host: process.env.FACHTRACING_POSTGRES_HOST,
    port: process.env.FACHTRACING_POSTGRES_PORT ? Number(process.env.FACHTRACING_POSTGRES_PORT) : undefined,
    database: process.env.FACHTRACING_POSTGRES_DATABASE,
    user: process.env.FACHTRACING_POSTGRES_USER,
    password: process.env.FACHTRACING_POSTGRES_PASSWORD,
    max: 5,
    connectionTimeoutMillis: 5_000,
    idleTimeoutMillis: 30_000,
    statement_timeout: 30_000,
    application_name: 'fachtracing-viewer'
  });
  pool.on('error', () => {
    // The request path returns a generic problem response. Never log query data or credentials here.
  });
  return pool;
}

export async function closeDatabase(): Promise<void> {
  if (!pool) return;
  const current = pool;
  pool = undefined;
  await current.end();
}
