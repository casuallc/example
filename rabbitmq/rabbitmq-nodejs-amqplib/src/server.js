'use strict';

const http = require('node:http');
const path = require('node:path');

const config = require('./config');
const rabbit = require('./rabbit');

function parseArgs(argv) {
  let configPath = path.join(process.cwd(), 'config.json');
  for (let i = 0; i < argv.length; i += 1) {
    if (argv[i] === '--config' || argv[i] === '-config') {
      configPath = argv[i + 1];
      i += 1;
    }
  }
  return { configPath };
}

async function main() {
  const { configPath } = parseArgs(process.argv.slice(2));
  const opts = config.load(configPath);
  const conn = new rabbit.Connection(opts);

  let stopping = false;
  const shouldStop = () => stopping;

  // Background consumer with reconnect retry.
  const consumerDone = rabbit.consume(conn, opts, shouldStop);

  const server = http.createServer((req, res) => {
    if (req.method !== 'POST' || req.url !== '/api/messages') {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'not found' }));
      return;
    }
    let body = '';
    req.on('data', (chunk) => {
      body += chunk;
    });
    req.on('end', async () => {
      let payload;
      try {
        payload = JSON.parse(body);
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'invalid JSON body' }));
        return;
      }
      const message = String(payload.Message ?? '');
      try {
        await conn.publish(message);
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
        return;
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'sent', Message: message }));
    });
  });

  const { host, port } = config.parseHttpAddr(opts.httpAddr);
  server.listen(port, host, () => {
    console.log(`HTTP listening on ${opts.httpAddr}`);
  });

  const shutdown = () => {
    if (stopping) return;
    stopping = true;
    console.log('Shutting down...');
    server.close();
    consumerDone
      .catch(() => {})
      .then(() => conn.close())
      .then(() => process.exit(0));
    // Force-exit if graceful shutdown stalls.
    setTimeout(() => process.exit(0), 5000).unref();
  };
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);
}

main().catch((err) => {
  console.error(err.message);
  process.exit(1);
});
