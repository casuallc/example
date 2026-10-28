# RabbitMQ .NET SDK — RabbitMQ.Client 7.x

使用官方 `RabbitMQ.Client` 7.x（全异步 API）：`BasicPublishAsync` 发送 + `AsyncEventingBasicConsumer` 后台消费，启动时自动声明交换机/队列/绑定。

## 拓扑

- Exchange：`demo.topic`（topic，durable）
- Queue：`demo.queue`（durable）
- Routing Key：`demo.message`

## 运行要求

- .NET 8 SDK（打包）/ .NET 8 运行时（运行），下载：https://dotnet.microsoft.com/download

## 打包与启动

```bash
# 打包并生成压缩包（源码 + 发布产物 + 启动脚本 + 配置）
bin/build.sh            # Linux
bin\build.ps1           # Windows（PowerShell）

# 解压压缩包后直接启动（同目录 appsettings.json 覆盖内置默认配置）
cd rabbitmq-dotnet-client-1.0.0
./start.sh              # Linux
./start.ps1             # Windows（PowerShell）

# 停止（结束占用 HTTP 端口的进程；端口默认从同目录配置文件读取，可 ./stop.sh [port] 覆盖）
./stop.sh               # Linux
./stop.ps1              # Windows（PowerShell）
```

产物为 `dist/rabbitmq-dotnet-client-1.0.0.zip`，解压后结构：

```
rabbitmq-dotnet-client-1.0.0/
├── rabbitmq-dotnet-client.dll (+ .exe 等发布产物)  # 可执行程序
├── appsettings.json                              # 配置文件，默认指向测试环境，可修改
├── start.sh / start.ps1                          # 启动脚本
├── stop.sh / stop.ps1                            # 停止脚本（按 HTTP 端口结束进程）
├── rabbitmq-dotnet-client.csproj / Program.cs / Rabbit/  # 源码
├── bin/                                          # 打包与启动脚本
├── config/appsettings.json.example               # 配置文件示例
└── README.md
```

## 配置

- 配置文件示例：`config/appsettings.json.example`（打包时自动复制为压缩包内的 `appsettings.json`），默认指向测试环境 `172.20.140.143:5672`（`admq` / `changeme`，vhost `apusic`）
- 也可用环境变量覆盖：`RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST`
- 端口：HTTP `8080`，可在 `appsettings.json` 的 `Kestrel` 段调整，或命令行传入 `--urls http://0.0.0.0:8081`

## 收发验证

发送消息：

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "hello rabbitmq"}'
```

控制台日志可见：

```
Sent message to exchange=demo.topic routingKey=demo.message: hello rabbitmq
Received message: hello rabbitmq
```

## 代码结构

| 文件 | 说明 |
|---|---|
| `Rabbit/RabbitOptions.cs` | 连接与拓扑配置（appsettings.json + 环境变量覆盖） |
| `Rabbit/RabbitConnection.cs` | 共享连接、拓扑声明、`BasicPublishAsync` 发送 |
| `Rabbit/RabbitConsumerService.cs` | `BackgroundService` + `AsyncEventingBasicConsumer` 消费 `demo.queue` |
| `Program.cs` | `POST /api/messages` 触发发送 |
| `bin/build.sh` / `bin/build.ps1` | 打包脚本，产出 `dist/` |
| `bin/start.sh` / `bin/start.ps1` | 启动脚本 |
| `bin/stop.sh` / `bin/stop.ps1` | 停止脚本（按 HTTP 端口定位并结束进程） |
| `config/appsettings.json.example` | 配置文件示例（测试环境） |
