'use strict';

const fs = require('node:fs');

// Options point at the test environment by default. load() overlays the JSON
// config file (if present) and then the RABBITMQ_HOST / RABBITMQ_PORT /
// RABBITMQ_USERNAME / RABBITMQ_PASSWORD / RABBITMQ_VHOST environment variables.
const DEFAULTS = {
  host: '172.20.140.143',
  port: 5672,
  username: 'admq',
  password: 'changeme',
  virtualHost: 'apusic',
  exchange: 'demo.topic',
  queue: 'demo.queue',
  routingKey: 'demo.message',
  httpAddr: ':8080',
};

function load(configPath) {
  const opts = { ...DEFAULTS };

  if (configPath && fs.existsSync(configPath)) {
    let parsed;
    try {
      parsed = JSON.parse(fs.readFileSync(configPath, 'utf8'));
    } catch (err) {
      throw new Error(`parse config file ${configPath}: ${err.message}`);
    }
    Object.assign(opts, parsed);
  }

  if (process.env.RABBITMQ_HOST) opts.host = process.env.RABBITMQ_HOST;
  if (process.env.RABBITMQ_PORT) {
    const port = Number.parseInt(process.env.RABBITMQ_PORT, 10);
    if (Number.isNaN(port)) {
      throw new Error(`invalid RABBITMQ_PORT "${process.env.RABBITMQ_PORT}"`);
    }
    opts.port = port;
  }
  if (process.env.RABBITMQ_USERNAME) opts.username = process.env.RABBITMQ_USERNAME;
  if (process.env.RABBITMQ_PASSWORD) opts.password = process.env.RABBITMQ_PASSWORD;
  if (process.env.RABBITMQ_VHOST) opts.virtualHost = process.env.RABBITMQ_VHOST;

  return opts;
}

// httpAddr uses the Go-style ":8080" form; split into host/port for listen().
function parseHttpAddr(httpAddr) {
  const idx = httpAddr.lastIndexOf(':');
  if (idx < 0) return { host: undefined, port: Number.parseInt(httpAddr, 10) };
  const host = httpAddr.slice(0, idx) || undefined;
  const port = Number.parseInt(httpAddr.slice(idx + 1), 10);
  return { host, port };
}

module.exports = { DEFAULTS, load, parseHttpAddr };
