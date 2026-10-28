# admq 测试客户端

用 Java 11 实现（jakarta 命名空间）的 admq 测试客户端，不依赖 Spring Boot，用于在 Linux 服务器上验证 admq 的各项功能。

## 功能特性

- **消息模式**：Queue（点对点）、Topic（发布/订阅）
- **消息类型**：TextMessage、ObjectMessage、BytesMessage
- **消费模式**：同步 receive、异步 MessageListener
- **运行模式**：单独发送、单独消费、端到端
- **高级特性**：事务消息、大消息、顺序消息（JMSXGroupID）
- **传输加密**：标准 TLS（`ssl://`，TLSv1~v1.3 自适应、支持强制客户端校验）；
  GMTLS (TLCP) 国密传输加密（`gmtls://` scheme，SM2+SM3+SM4）
- **配置驱动**：通过 properties 配置文件切换不同测试场景
- **易于扩展**：策略模式，新增场景只需实现 TestScenario 接口

## 环境要求

- JDK 11+
- Maven 3.6+
- admq Broker（默认连接 tcp://localhost:61616）

## 快速开始

### 1. 编译

```bash
mvn clean package
```

执行后，所有依赖 jar 会自动复制到 `lib/` 目录。

### 2. 构建分发包

#### 方式一：Maven 自动打包（推荐）

```bash
mvn clean package
```

编译完成后自动产生 `target/jms-client-1.0.0-dist.zip`。

#### 方式二：PowerShell 脚本

```powershell
.\build.ps1
```

产出 `dist/jms-client-1.0.0.zip`，支持 `-Version` 参数自定义版本号。

### 3. 运行测试

**开发环境（源码目录）：**

```bash
./start.sh config/queue-send.properties
```

**分发包内（解压后）：**

```bash
cd jms-client-1.0.0
./bin/start.sh config/queue-send.properties
```

Windows 环境：

```cmd
bin\start.bat config\queue-send.properties
```

或通过 Maven 直接运行：

```bash
mvn exec:java -Dexec.args="config/queue-send.properties"
```

## 配置文件说明

所有配置文件位于 `config/` 目录，以下为主要参数：

| 参数 | 说明 | 示例 |
|------|------|------|
| `broker.url` | admq Broker 地址 | `tcp://localhost:61616` |
| `broker.username` | 认证用户名（可选） | `admin` |
| `broker.password` | 认证密码（可选） | `admin` |
| `scenario.type` | 测试场景类型 | `QUEUE`、`TOPIC`、`TRANSACTION`、`LARGE`、`ORDERED`、`GMTLS`、`TLS` |
| `run.mode` | 运行模式 | `SEND_ONLY`、`RECEIVE_ONLY`、`BOTH` |
| `destination.name` | 目标名称 | `test.queue` |
| `destination.type` | 目标类型 | `QUEUE`、`TOPIC` |
| `message.type` | 消息类型 | `TEXT`、`OBJECT`、`BYTES` |
| `message.count` | 消息数量 | `10` |
| `message.content` | 消息内容 | `Hello admq` |
| `message.size` | 消息大小（字节） | `1048576` |
| `message.group.id` | 消息组 ID（顺序消息） | `group-1` |
| `consumer.mode` | 消费模式 | `SYNC`、`ASYNC` |
| `consumer.threads` | 消费线程数 | `1` |
| `transaction.enabled` | 是否启用事务 | `true`、`false` |
| `receive.timeout` | 接收超时（毫秒） | `5000` |
| `gmtls.sign.cert` | 客户端签名证书（PEM，`gmtls://` 时必填） | `certs/client-sign.crt` |
| `gmtls.sign.key` | 客户端签名私钥（PKCS8 PEM，必填） | `certs/client-sign.key` |
| `gmtls.enc.cert` | 客户端加密证书（PEM，必填） | `certs/client-enc.crt` |
| `gmtls.enc.key` | 客户端加密私钥（PKCS8 PEM，必填） | `certs/client-enc.key` |
| `gmtls.ca.cert` | 根 CA 证书（PEM，必填） | `certs/ca.crt` |
| `gmtls.key.password` | 私钥口令（可选） | |
| `gmtls.cipher.suites` | 逗号分隔国密套件（可选） | `TLCP_ECDHE_SM4_GCM_SM3,...` |
| `tls.trust.store` | 客户端 truststore（`ssl://` 时必填，校验服务端） | `certs/tls/client-truststore.p12` |
| `tls.trust.store.password` | truststore 口令 | `changeme` |
| `tls.key.store` | 客户端 keystore（mTLS 双向认证时必填） | `certs/tls/client-keystore.p12` |
| `tls.key.store.password` | keystore 口令 | `changeme` |
| `tls.key.password` | 私钥口令（可选，默认同 keystore 口令） | |
| `tls.wrong.trust.store` | "错误 CA"truststore（TLS 验证场景负向用例） | `certs/tls/wrong-truststore.p12` |
| `tls.client.auth.required` | broker 开启 needClientAuth 时置 true（启用无证书拒绝用例） | `true`、`false` |

## 内置测试场景

### Queue 测试

```bash
# 发送
./start.sh config/queue-send.properties

# 消费
./start.sh config/queue-receive.properties

# 端到端
./start.sh config/queue-e2e.properties
```

### Topic 测试

```bash
# 发布
./start.sh config/topic-send.properties

# 订阅（需先启动）
./start.sh config/topic-subscribe.properties
```

### 事务消息测试

```bash
./start.sh config/transaction-send.properties
./start.sh config/transaction-receive.properties
```

### 大消息测试

```bash
./start.sh config/large-message-send.properties
```

### 顺序消息测试

```bash
./start.sh config/ordered-message-send.properties
```

### GMTLS 国密传输加密测试

基于腾讯 Kona SMSuite 的国密传输加密（SM2 握手与身份认证 + SM3 完整性 + SM4 通道加密，
协议 TLCPv1.1 / GM/T 0024）。对业务代码透明——仅连接配置不同，所有场景
（QUEUE、TOPIC、TRANSACTION 等）把 `broker.url` 换成 `gmtls://` 并补证书配置即可。

**前提**：

1. Broker 已启用 `gmtls://` connector（见 `admq-activemq` 仓库 `docs/gmtls.md`，默认端口 61617）
2. 客户端持有 SM2 双证书（签名 + 加密）与根 CA 证书。本仓库 `certs/` 目录已附带
   自签测试证书（仅限测试，SAN 含 `localhost`/`127.0.0.1`），说明见 `certs/README.md`

```bash
# 端到端
./start.sh config/gmtls-queue-e2e.properties

# 发送
./start.sh config/gmtls-queue-send.properties

# 消费
./start.sh config/gmtls-queue-receive.properties
```

failover 形式同样支持：`broker.url=failover:(gmtls://broker1:61617,gmtls://broker2:61617)`

#### GMTLS 加密验证场景（scenario.type=GMTLS）

`config/gmtls-verify.properties` 运行专项验证场景，逐项产出证据并断言，任一失败即退出码非 0：

| 步骤 | 验证点 | 通过判据 |
|------|--------|----------|
| 1/4 | 无证书接入拒绝 | `gmtls://` 不配证书时 `createConnection()` 抛出提示缺失证书的异常（防止静默降级明文） |
| 2/4 | TLCP 协商证据（SM2/SM3/SM4） | 裸握手断言：协议 = `TLCPv1.1`；套件含 `SM4`（通道加密）与 `SM3`（完整性）；对端证书签名算法含 `SM3`（SM2 证书，SM3withSM2） |
| 3/4 | 明文接入拒绝 | 普通 `tcp://` 连 gmtls 端口失败（端口只接受 TLCP 加密接入） |
| 4/4 | 加密通道端到端 | 经 `gmtls://` 完成 JMS 消息收发，内容一致 |

```bash
./start.sh config/gmtls-verify.properties
```

典型输出：

```
[GMTLS-VERIFY 2/4] probing TLCP handshake against localhost:61617
  negotiated protocol : TLCPv1.1
  negotiated suite    : TLCP_ECDHE_SM4_GCM_SM3
  peer certificate    : CN=server-sign, ...
  peer cert signature : SM3withSM2 (public key algorithm: EC)
  [assert] protocol is TLCPv1.1 (GM/T 0024) ... OK
  [assert] cipher suite uses SM4 channel encryption ... OK
  [assert] cipher suite uses SM3 integrity ... OK
  [assert] peer certificate is SM2 (SM3withSM2 signature) ... OK
[GMTLS-VERIFY 2/4] PASS: protocol=TLCPv1.1, suite=TLCP_ECDHE_SM4_GCM_SM3
```

#### 保密性抓包验证（传输内容不可窃听）

代码内无法抓包，用特征串配合抓包工具人工验证：

1. 开始抓包（Linux：`sudo tcpdump -i any -A port 61617`；Windows：Wireshark 选回环网卡，过滤 `tcp.port == 61617`）
2. 运行 `./start.sh config/gmtls-verify.properties`（消息内容为特征串 `GMTLS-SECRET-TOKEN-12345`）
3. **预期**：抓包结果中搜不到 `GMTLS-SECRET-TOKEN-12345`，报文为 TLCP 密文
4. **对照**：对明文端口跑 `./start.sh config/queue-e2e.properties` 并抓包 `port 61616`，能搜到明文消息

证书本身也可用 OpenSSL 3.x 验证为国密算法：`openssl x509 -in certs/client-sign.crt -noout -text`，
`Signature Algorithm` 应显示 `SM2-with-SM3`，公钥曲线为 `sm2p256v1`。

### TLS 传输加密测试

标准 TLS（`ssl://`）对消息传输加密，客户端通过 truststore 校验服务端身份；broker 开启
`needClientAuth=true` 时强制客户端持证书（mTLS 双向认证）。对业务代码透明——仅连接配置不同，
所有场景把 `broker.url` 换成 `ssl://` 并配 truststore 即可。

**前提**：

1. Broker 已启用 `ssl://` connector 并配置服务端 keystore（配置示例见 `certs/tls/README.md`，
   本文端口按 61618）
2. 客户端持有 truststore（信任签发 CA）。本仓库 `certs/tls/` 已附带 RSA 测试证书与
   keystore（仅限测试，口令 `changeme`），说明与重新生成脚本见 `certs/tls/README.md`

```bash
# 端到端
./start.sh config/tls-queue-e2e.properties
```

#### TLS 加密验证场景（scenario.type=TLS）

`config/tls-verify.properties` 运行专项验证场景，逐项产出证据并断言，任一失败即退出码非 0：

| 步骤 | 验证点 | 通过判据 |
|------|--------|----------|
| 1/5 | 服务器身份校验 | 用"错误 CA"truststore 握手必须失败（`PKIX path building failed` 类异常） |
| 2/5 | 版本自适应（V1~V1.3） | 分别强制单版本裸握手：协商结果 = 强制版本。`TLSv1.2` 必须 PASS；`TLSv1.3` 双方支持时 PASS；`TLSv1.1`/`TLSv1` 被 JVM 安全策略禁用时 SKIP（预期行为，启用方法见 `certs/tls/README.md`） |
| 3/5 | 强制客户端校验 | broker `needClientAuth=true` 时，无客户端证书握手必须失败（`certificate_required` 类异常） |
| 4/5 | 双向认证接入 | 携带客户端证书握手成功，日志输出双方证书主体与协商套件 |
| 5/5 | 加密通道端到端 | 经 `ssl://` 完成 JMS 消息收发，内容一致 |

```bash
./start.sh config/tls-verify.properties
```

典型输出（版本矩阵，JDK 17 环境）：

```
[TLS-VERIFY 2/5] TLS version adaptation against localhost:61618
  TLSv1.3 ... PASS (cipher: TLS_AES_256_GCM_SHA384)
  TLSv1.2 ... PASS (cipher: TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384)
  TLSv1.1 ... SKIP (SSLHandshakeException: No appropriate protocol ...)
  TLSv1 ... SKIP (SSLHandshakeException: No appropriate protocol ...)
[TLS-VERIFY 2/5] PASS: 2 version(s) negotiated, 2 skipped by security policy
```

#### 保密性抓包验证（传输内容不可窃听）

1. 开始抓包（Linux：`sudo tcpdump -i any -A port 61618`；Windows：Wireshark 过滤 `tcp.port == 61618`）
2. 运行 `./start.sh config/tls-verify.properties`（消息内容为特征串 `TLS-SECRET-TOKEN-67890`）
3. **预期**：抓包结果中搜不到 `TLS-SECRET-TOKEN-67890`，报文为 TLS 密文（Wireshark 可识别 TLS 记录层）
4. **对照**：对明文端口跑 `./start.sh config/queue-e2e.properties` 并抓包 `port 61616`，能搜到明文消息

## 扩展新功能

新增测试场景只需两步：

1. 实现 `TestScenario` 接口：

```java
public class MyTestScenario extends AbstractTestScenario {
    @Override
    protected void doExecute(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        // 自定义测试逻辑
    }
}
```

2. 在 `Main.java` 的 `createScenario()` 方法中注册：

```java
case "MY_SCENARIO":
    return new MyTestScenario();
```

3. 添加配置文件：

```properties
scenario.type=MY_SCENARIO
run.mode=SEND_ONLY
```

## 项目结构

```
jms-client/
├── pom.xml                          # Maven 构建配置
├── start.sh                         # Linux 启动脚本（开发环境）
├── start.bat                        # Windows 启动脚本（开发环境）
├── build.ps1                        # 独立 PowerShell 构建脚本
├── lib/                             # 运行时依赖（mvn package 后生成）
├── config/                          # 测试配置文件
├── certs/                           # GMTLS 测试证书（SM2 双证书 + 根 CA，仅限测试）
├── src/assembly/assembly.xml        # Maven Assembly 打包描述符
├── docs/
│   └── superpowers/specs/           # 设计文档
└── src/main/java/com/apusic/admq/example/jms/
    ├── Main.java                    # 程序入口
    ├── config/ClientConfig.java     # 配置解析
    ├── connection/ConnectionManager.java  # 连接管理
    ├── connection/TlsSupport.java         # TLS SSLContext 构建
    ├── producer/MessageProducer.java      # 消息生产
    ├── consumer/SyncConsumer.java         # 同步消费
    ├── consumer/AsyncConsumer.java        # 异步消费
    └── scenario/                  # 测试场景
        ├── TestScenario.java
        ├── AbstractTestScenario.java
        ├── QueueTestScenario.java
        ├── TopicTestScenario.java
        ├── TransactionTestScenario.java
        ├── LargeMessageTestScenario.java
        ├── OrderedMessageTestScenario.java
        ├── GmtlsTestScenario.java
        └── TlsTestScenario.java
```

### 分发包结构

```
jms-client-1.0.0/
├── bin/
│   ├── start.sh                     # Linux 启动脚本
│   └── start.bat                    # Windows 启动脚本
├── lib/
│   ├── jms-client-1.0.0.jar    # 主程序
│   └── *.jar                        # 依赖
├── config/
│   └── *.properties                 # 测试场景配置
└── README.md
```

## 日志

日志输出格式：

```
2026-05-25 10:00:00.000 [main] INFO  com.apusic.admq.example.jms.Main - admq Test Client Starting
2026-05-25 10:00:00.500 [main] INFO  c.e.a.p.MessageProducer - Starting to send 10 messages to test.queue
2026-05-25 10:00:00.600 [main] INFO  c.e.a.p.MessageProducer - Sent message 10/10 to test.queue
2026-05-25 10:00:00.600 [main] INFO  c.e.a.p.MessageProducer - Send completed in 100 ms, rate: 100 msg/s
```

日志级别在 `src/main/resources/logback.xml` 中配置。
