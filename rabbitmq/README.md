# RabbitMQ 多语言 SDK 收发消息示例

本目录包含多个相互独立的示例代码库，演示不同语言/框架下 RabbitMQ 的消息收发。

## 示例清单

| 目录 | 技术栈 | SDK | 状态 |
|---|---|---|---|
| `rabbitmq-java-springboot` | Java 17 / Spring Boot 3 | spring-boot-starter-amqp（RabbitTemplate + @RabbitListener） | ✅ 已完成 |
| `rabbitmq-java-spring-cloud-stream` | Java 17 / Spring Cloud Stream | spring-cloud-stream-binder-rabbit（StreamBridge + Consumer 函数） | ✅ 已完成 |
| `rabbitmq-go-amqp091` | Go | github.com/rabbitmq/amqp091-go | ✅ 已完成 |
| `rabbitmq-dotnet-client` | .NET 8 | RabbitMQ.Client 7.x | ✅ 已完成 |
| `rabbitmq-nodejs-amqplib` | Node.js 18+ | amqplib | ✅ 已完成 |

---

## 基准规范（所有示例必须遵守）

### 1. 统一消息拓扑（跨语言互通）

所有示例使用相同的 Exchange / Queue / Routing Key，**任一示例发出的消息可被任一其它示例消费**：

| 项 | 值 |
|---|---|
| Exchange | `demo.topic`（topic 类型，durable） |
| Queue | `demo.queue`（durable） |
| Routing / Binding Key | `demo.message` |
| 消息体 | UTF-8 文本 |

### 2. 统一连接配置

- 示例配置文件**默认指向测试环境**：`172.20.140.143:5672`，用户名 `admq`，密码 `changeme`，vhost `apusic`
- 同时支持环境变量覆盖：`RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST`

### 3. 每个示例必须提供的内容

| 内容 | 说明 |
|---|---|
| 源码 | 完整可编译的工程源码 |
| `bin/build.sh` + `bin/build.ps1` | 打包脚本，产出最终压缩包（Windows 脚本用 PowerShell，**不用 .bat**） |
| `bin/start.sh` + `bin/start.ps1` | 启动脚本，解压后可直接运行 |
| `bin/stop.sh` + `bin/stop.ps1` | 停止脚本，按 HTTP 端口定位并结束进程（避免端口残留） |
| `config/` 配置文件示例 | 打包时复制为压缩包内的默认配置文件，指向测试环境 |
| `README.md` | 运行、配置、收发验证说明 |

### 4. 交付物标准：单压缩包

每个示例的打包脚本产出一个压缩包 `dist/<示例名>-<版本>.zip`，**同时包含源码和可执行程序**，解压即可运行，结构基准：

```
<示例名>-<版本>/
├── <可执行程序>            # jar / 二进制 / 入口脚本
├── <配置文件>              # 默认可直接连测试环境，可修改
├── start.sh / start.ps1   # 启动脚本
├── stop.sh / stop.ps1     # 停止脚本
├── <源码目录>              # 完整工程源码
├── bin/                   # 打包与启动脚本
├── config/                # 配置文件示例
└── README.md
```

### 5. 验证要求

- 打包脚本必须实际执行通过，压缩包内容核对齐全
- 可执行程序必须连测试环境完成一次**发送 → 消费**冒烟验证
