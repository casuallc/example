'use strict';

const amqp = require('amqplib');

// Connection owns the shared RabbitMQ connection and the channel used for
// publishing. Initialization is lazy and idempotent so the consumer and the
// HTTP endpoint can start in any order. On unexpected connection loss the
// cached state is dropped and the next operation reconnects.
class Connection {
  constructor(opts) {
    this.opts = opts;
    this.conn = null;
    this.pubCh = null;
    this.initializing = null;
  }

  // ensureInitialized connects to RabbitMQ and declares the topology once.
  async ensureInitialized() {
    if (this.conn && this.pubCh) return;
    if (this.initializing) return this.initializing;

    this.initializing = (async () => {
      const url = {
        protocol: 'amqp',
        hostname: this.opts.host,
        port: this.opts.port,
        username: this.opts.username,
        password: this.opts.password,
        vhost: this.opts.virtualHost,
      };
      let conn;
      try {
        conn = await amqp.connect(url);
      } catch (err) {
        throw new Error(
          `dial ${this.opts.host}:${this.opts.port} vhost=${this.opts.virtualHost}: ${err.message}`
        );
      }
      conn.on('error', (err) => console.error(`Connection error: ${err.message}`));
      conn.on('close', () => {
        if (this.conn === conn) {
          this.conn = null;
          this.pubCh = null;
          console.error('Connection closed, will reconnect on next operation');
        }
      });

      const ch = await conn.createChannel();
      await declareTopology(ch, this.opts);
      this.conn = conn;
      this.pubCh = ch;
      console.log(
        `Connected to RabbitMQ ${this.opts.host}:${this.opts.port} vhost=${this.opts.virtualHost}`
      );
    })();

    try {
      await this.initializing;
    } finally {
      this.initializing = null;
    }
  }

  // newChannel opens a channel on the shared connection (for the consumer).
  async newChannel() {
    await this.ensureInitialized();
    return this.conn.createChannel();
  }

  // publish sends a UTF-8 text message to the demo exchange.
  async publish(message) {
    await this.ensureInitialized();
    this.pubCh.publish(this.opts.exchange, this.opts.routingKey, Buffer.from(message, 'utf8'), {
      contentType: 'text/plain',
      persistent: true,
    });
    console.log(
      `Sent message to exchange=${this.opts.exchange} routingKey=${this.opts.routingKey}: ${message}`
    );
  }

  async close() {
    const { conn, pubCh } = this;
    this.conn = null;
    this.pubCh = null;
    if (pubCh) await pubCh.close().catch(() => {});
    if (conn) await conn.close().catch(() => {});
  }
}

// declareTopology declares the shared demo topology; safe to call repeatedly.
async function declareTopology(ch, opts) {
  await ch.assertExchange(opts.exchange, 'topic', { durable: true });
  await ch.assertQueue(opts.queue, { durable: true });
  await ch.bindQueue(opts.queue, opts.exchange, opts.routingKey);
}

// consume runs the consumer loop on the shared demo queue until stopped.
// It declares the topology itself (idempotent) so the sample works even if
// nothing has published yet. On connection loss it retries with a fixed backoff.
async function consume(conn, opts, shouldStop) {
  while (!shouldStop()) {
    try {
      await consumeOnce(conn, opts, shouldStop);
      return;
    } catch (err) {
      if (shouldStop()) return;
      console.error(`Consumer error: ${err.message}, retrying in 5s`);
      await sleep(5000);
    }
  }
}

async function consumeOnce(conn, opts, shouldStop) {
  const ch = await conn.newChannel();
  try {
    await declareTopology(ch, opts);
    console.log(
      `Consuming queue=${opts.queue} bound to exchange=${opts.exchange} routingKey=${opts.routingKey}`
    );

    await new Promise((resolve, reject) => {
      ch.on('error', reject);
      ch.on('close', () => reject(new Error('consumer channel closed')));
      ch.consume(
        opts.queue,
        (msg) => {
          if (!msg) return;
          console.log(`Received message: ${msg.content.toString('utf8')}`);
          ch.ack(msg);
          if (shouldStop()) resolve();
        },
        { noAck: false }
      ).catch(reject);
      // Poll the stop flag so a quiet queue does not block shutdown.
      const timer = setInterval(() => {
        if (shouldStop()) {
          clearInterval(timer);
          resolve();
        }
      }, 500);
    });
  } finally {
    await ch.close().catch(() => {});
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

module.exports = { Connection, declareTopology, consume };
