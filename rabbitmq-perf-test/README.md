# RabbitMQ 队列规模压测工具

模拟 **几千 ~ 几万个队列、每个队列都有生产者和消费者** 的场景，压测 RabbitMQ 服务端在大规模队列下的稳定性。

## 核心模型

- 队列名：`{queuePrefix}{i}`，如 `perf-queue-0` ~ `perf-queue-9999`
- 消息通过 **默认（匿名）交换机 `""`** 发布，**routing key = 队列名**，消息直接路由到同名队列（无需显式绑定）
- 启动时自动批量声明全部队列（可关闭）
- 每个队列 1 个消费者；所有队列按生产者线程数均分，轮询发送
- **连接复用**：固定 `connectionCount` 个 TCP 连接，所有 channel 在连接上多路复用；多个消费者共享同一个 channel（`consumersPerChannel` 控制密度）
- 支持用户名 / 密码 / vhost 认证
- 周期性输出发送/消费速率、失败数、积压量、平均/最大延迟

## 构建

```bash
# Windows
.\build.ps1 [-Version 1.0.0]

# Linux
./build.sh [1.0.0]
```

产物：`dist/rabbitmq-perf-test-1.0.0.tar.gz`，解压后结构：

```
rabbitmq-perf-test-1.0.0/
├── bin/start.sh, bin/stop.sh   # Linux 启动/停止脚本
├── lib/*.jar                   # 可执行 fat jar
├── config/config.properties    # 配置文件
└── logs/                       # 运行日志目录
```

## 运行

### Linux（发布包，后台运行）

```bash
tar -xzf rabbitmq-perf-test-1.0.0.tar.gz
cd rabbitmq-perf-test-1.0.0
vi config/config.properties        # 修改 host/username/password 等

bin/start.sh                       # 后台启动，日志在 logs/rabbitmq-perf-test.out
bin/start.sh queueCount=10000 producerRate=50000   # 启动时覆盖配置
bin/stop.sh                        # 优雅停止（30 秒超时后强杀）
```

Java 优先使用 `JAVA_HOME/bin/java`，未设置 `JAVA_HOME` 时从 `PATH` 查找。JVM 堆内存可通过环境变量调整：`JAVA_OPTS="-Xmx4g" bin/start.sh`

### 直接运行（开发调试）

```bash
# 使用配置文件
java -jar target/rabbitmq-perf-test-1.0.0-jar-with-dependencies.jar --config=config/config.properties

# 命令行覆盖任意参数（key=value）
java -jar target/rabbitmq-perf-test-1.0.0-jar-with-dependencies.jar \
     --config=config/config.properties \
     host=192.168.1.10 username=admin password=secret \
     queueCount=10000 connectionCount=8 producerThreads=16 \
     producerRate=50000 durationSeconds=300
```

## 参数说明

| 参数 | 默认值 | 说明 |
|---|---|---|
| `host` / `port` / `vhost` | localhost / 5672 / / | 服务端地址 |
| `username` / `password` | guest / guest | 认证账号 |
| `connectionCount` | 4 | 复用的 TCP 连接数 |
| `queueCount` | 1000 | 队列总数（每个队列都有生产+消费） |
| `queuePrefix` | perf-queue- | 队列名前缀 |
| `queueDurable` | true | 队列是否持久化 |
| `autoDeclare` | true | 启动时是否声明队列 |
| `declareThreads` | 8 | 声明队列并发线程数 |
| `producerThreads` | 8 | 生产者线程数（队列均分到各线程） |
| `producerRate` | 0 | 全局发送速率（条/秒），0 = 不限速 |
| `messageSize` | 1024 | 消息体大小（字节） |
| `persistent` | false | 消息是否持久化 |
| `measureLatency` | true | 消息头 8 字节带时间戳，统计端到端延迟 |
| `consumerPrefetch` | 100 | 消费者 prefetch |
| `consumerAutoAck` | true | 自动确认（false 时逐条 ack） |
| `consumersPerChannel` | 100 | 每个 channel 挂载的消费者数 |
| `durationSeconds` | 0 | 压测时长（秒），0 = 持续运行直到 Ctrl+C |
| `reportIntervalSeconds` | 5 | 指标输出间隔 |

## 资源估算建议

- **1 万队列**：`connectionCount=8`，`producerThreads=16`，消费者 channel 约 100 个（`consumersPerChannel=100`）
- **5 万+ 队列**：`connectionCount=16`，适当增大 JVM 堆：`-Xmx4g`；同时注意服务端每个队列有内存开销，关注 RabbitMQ 内存水位
- 大规模压测建议先开 `producerRate` 限速，逐步加量观察服务端稳定性
