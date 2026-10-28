# RabbitMQ Node.js SDK — amqplib

使用 `amqplib`：`publish` 发送（durable + persistent）+ 后台消费（断线自动重连重试），启动时自动声明交换机/队列/绑定。

## 拓扑

- Exchange：`demo.topic`（topic，durable）
- Queue：`demo.queue`（durable）
- Routing Key：`demo.message`

## 运行要求

- Node.js 18+（打包与运行均需要）
- 打包阶段需联网安装依赖（`npm ci --omit=dev`）；压缩包内含 `node_modules`，解压后离线可运行

## 打包与启动

```bash
# 打包并生成压缩包（源码 + node_modules + 启动脚本 + 配置）
bin/build.sh            # Linux / Git Bash
bin\build.ps1           # Windows（PowerShell）

# 解压压缩包后直接启动（同目录 config.json 覆盖内置默认配置）
cd rabbitmq-nodejs-amqplib-1.0.0
./start.sh              # Linux
./start.ps1             # Windows（PowerShell）

# 停止（结束占用 HTTP 端口的进程；端口默认从同目录配置文件读取，可 ./stop.sh [port] 覆盖）
./stop.sh               # Linux
./stop.ps1              # Windows（PowerShell）
```

产物为 `dist/rabbitmq-nodejs-amqplib-1.0.0.zip`，解压后结构：

```
rabbitmq-nodejs-amqplib-1.0.0/
├── node_modules/               # 生产依赖（amqplib）
├── config.json                 # 配置文件，默认指向测试环境，可修改
├── start.sh / start.ps1        # 启动脚本
├── stop.sh / stop.ps1          # 停止脚本（按 HTTP 端口结束进程）
├── package.json / src/         # 源码
├── bin/                        # 打包与启动脚本
├── config/config.json.example  # 配置文件示例
└── README.md
```

## 配置

- 配置文件示例：`config/config.json.example`（打包时自动复制为压缩包内的 `config.json`），默认指向测试环境 `172.20.140.143:5672`（`admq` / `changeme`，vhost `apusic`）
- 配置优先级（高到低）：环境变量 > `config.json` > 内置默认值
- 环境变量覆盖：`RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST`
- 指定其他配置文件路径：`./start.sh --config /path/to/config.json`
- HTTP 端口：`config.json` 的 `httpAddr`（默认 `:8080`）

## 收发验证

发送消息：

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{"Message": "hello rabbitmq"}'
```

控制台日志可见：

```
Sent message to exchange=demo.topic routingKey=demo.message: hello rabbitmq
Received message: hello rabbitmq
```

## 代码结构

| 文件 | 说明 |
|---|---|
| `src/config.js` | 连接与拓扑配置（config.json + 环境变量覆盖） |
| `src/rabbit.js` | 共享连接、拓扑声明、`publish` 发送、后台消费（断线 5s 重试） |
| `src/server.js` | `POST /api/messages` 触发发送，信号优雅退出 |
| `bin/build.sh` / `bin/build.ps1` | 打包脚本，产出 `dist/` |
| `bin/start.sh` / `bin/start.ps1` | 启动脚本 |
| `bin/stop.sh` / `bin/stop.ps1` | 停止脚本（按 HTTP 端口定位并结束进程） |
| `config/config.json.example` | 配置文件示例（测试环境） |
