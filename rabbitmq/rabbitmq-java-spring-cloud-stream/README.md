# RabbitMQ Java SDK — Spring Cloud Stream 集成

使用 `spring-cloud-stream-binder-rabbit`：`StreamBridge` 发送 + 函数式 `Consumer` 消费，启动时由 binder 自动声明交换机/队列/绑定。

## 拓扑

- Exchange：`demo.topic`（topic，durable）
- Queue：`demo.queue`（durable）
- Routing / Binding Key：`demo.message`

Spring Cloud Stream 默认命名与基准拓扑不同，本示例通过配置对齐：

| 配置 | 作用 |
|---|---|
| `bindings.demoConsumer-in-0.destination=demo.topic` | 绑定到交换机 `demo.topic` |
| `bindings.demoConsumer-in-0.group=demo.queue` + `queue-name-group-only=true` | 队列名固定为 `demo.queue`（默认会拼成 `demo.topic.demo.queue`） |
| `consumer.binding-routing-key=demo.message` | 绑定键固定为 `demo.message`（默认 `#`） |
| `producer.routing-key-expression='demo.message'` | 发送路由键固定为 `demo.message`（默认为 destination 名） |

## 打包与启动

```bash
# 打包并生成压缩包（源码 + 可执行 jar + 启动脚本 + 配置）
bin/build.sh            # Linux
bin\build.ps1           # Windows（PowerShell）

# 解压压缩包后直接启动（同目录 application.yml 覆盖 jar 内置默认配置）
cd rabbitmq-java-spring-cloud-stream-1.0.0
./start.sh              # Linux
./start.ps1             # Windows（PowerShell）

# 停止（结束占用 HTTP 端口的进程；端口默认从同目录配置文件读取，可 ./stop.sh [port] 覆盖）
./stop.sh               # Linux
./stop.ps1              # Windows（PowerShell）
```

产物为 `dist/rabbitmq-java-spring-cloud-stream-1.0.0.zip`，解压后结构：

```
rabbitmq-java-spring-cloud-stream-1.0.0/
├── rabbitmq-java-spring-cloud-stream-1.0.0.jar   # 可执行程序
├── application.yml                                # 配置文件，默认指向测试环境，可修改
├── start.sh / start.ps1                           # 启动脚本
├── stop.sh / stop.ps1                             # 停止脚本（按 HTTP 端口结束进程）
├── pom.xml / src/                                 # 源码
├── bin/                                           # 打包与启动脚本
├── config/application.yml.example                 # 配置文件示例
└── README.md
```

## 配置

- 配置文件示例：`config/application.yml.example`（打包时自动复制为压缩包内的 `application.yml`），默认指向测试环境 `172.20.140.143:5672`（`admq` / `changeme`，vhost `apusic`）
- 也可用环境变量覆盖：`RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST`
- 端口：HTTP `8080`，可用 `JAVA_OPTS` 或命令行 `--server.port=` 调整

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
| `src/main/resources/application.yml` | binder 绑定与拓扑对齐配置 |
| `MessageConsumerConfig.java` | 函数式 `Consumer<String> demoConsumer` 消费 `demo.queue` |
| `MessageProducer.java` | `StreamBridge.send("demoProducer-out-0", ...)` 发送 |
| `MessageController.java` | `POST /api/messages` 触发发送 |
| `bin/build.sh` / `bin/build.ps1` | 打包脚本，产出 `dist/` |
| `bin/start.sh` / `bin/start.ps1` | 启动脚本 |
| `bin/stop.sh` / `bin/stop.ps1` | 停止脚本（按 HTTP 端口定位并结束进程） |
| `config/application.yml.example` | 配置文件示例（测试环境） |
