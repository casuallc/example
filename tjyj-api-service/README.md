# tjyj-api-service

轻量 HTTP 代理服务，基于 EMQX 5.x HTTP API 汇总 topic 的消息数量与客户端信息，组装成 Pulsar `topic stats` 格式返回，同时提供 topic 监控管理端点和 Pulsar 风格的 broker health 接口。

## 功能

- `GET /admin/v2/persistent/{tenant}/{namespace}/{topic}/stats`
  - 支持多层 topic，例如 `/admin/v2/persistent/mqtt-public/default/home/warn/sub/1/stats`，此时 topic 为 `home/warn/sub/1`
  - 调用 EMQX 5.x API：
    - `GET /api/v5/mqtt/topic_metrics/{topic}`（消息计数；topic 未注册监控时返回 404，计数回退为按客户端 `send_msg`/`recv_msg` 汇总）
    - `GET /api/v5/subscriptions?topic=...`（自动分页）
    - `GET /api/v5/clients/{clientid}`（并发查询每个订阅客户端）
  - 组装成 Pulsar topic stats JSON 返回。

- topic 监控管理（代理 EMQX `mqtt/topic_metrics`，topic 支持多层路径）：
  - `POST /api/v1/topic_metrics/{topic}` — 创建监控（已存在时视为成功）
  - `GET /api/v1/topic_metrics/{topic}` — 查询单个 topic 监控（未注册返回 404）
  - `DELETE /api/v1/topic_metrics/{topic}` — 删除监控
  - `GET /api/v1/topic_metrics` — 列出全部已注册监控

- `GET /admin/v2/brokers/health`
  - 检查本机 `5682` 端口是否在监听。
  - 返回纯文本 `ok` 或 `failed`。

## 运行

### Windows 本机编译（需要本机安装 Go）

```bash
# 1. 安装依赖并编译
go mod tidy
go build -o tjyj-api-service.exe .

# 2. 运行（必须设置 EMQX API 密钥，在 EMQX Dashboard -> 系统管理 -> API 密钥 中创建）
set EMQX_API_KEY=your_api_key
set EMQX_API_SECRET=your_api_secret
./tjyj-api-service.exe

# 3. 或使用环境变量自定义
set EMQX_BASE_URL=http://127.0.0.1:18083
set EMQX_API_KEY=your_api_key
set EMQX_API_SECRET=your_api_secret
set SERVER_PORT=8080
./tjyj-api-service.exe
```

### WSL 交叉编译（推荐：一次打出 ARM/x86 多架构包）

直接在 PowerShell 里执行 `build-wsl.ps1`，脚本会自动进入 WSL、安装 Go、设置国内模块代理，并编译出多架构二进制：

```powershell
# 进入项目目录
cd D:\apusic\admq\example\tjyj-api-service

# 直接运行（WSL 里需要 sudo 安装 Go 时会交互提示）
.\build-wsl.ps1

# 或者传入 sudo 密码，实现无人值守
.\build-wsl.ps1 -SudoPassword 'your_wsl_password'

# 也可以指定版本号
.\build-wsl.ps1 -Version 1.0.0
```

输出到 `dist/` 目录：

```
dist/
├── tjyj-api-service_linux_amd64
├── tjyj-api-service_linux_arm64
├── tjyj-api-service_windows_amd64.exe
└── tjyj-api-service_windows_arm64.exe
```

把对应平台的二进制拷贝到目标虚拟机即可运行。

## 配置

通过环境变量配置，默认值如下：

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `SERVER_PORT` | `8080` | 服务监听端口 |
| `EMQX_BASE_URL` | `http://127.0.0.1:18083` | EMQX 5.x HTTP API 基础地址 |
| `EMQX_API_KEY` | *必填* | EMQX API 密钥（Basic 认证用户名） |
| `EMQX_API_SECRET` | *必填* | EMQX API 密钥对应的 Secret（Basic 认证密码） |
| `EMQX_TIMEOUT` | `10s` | 下游 HTTP 调用超时 |
| `EMQX_PAGE_SIZE` | `500` | 订阅列表每页大小 |
| `EMQX_MAX_CONCURRENCY` | `20` | 并发查询客户端详情数 |
| `HEALTH_PORT` | `5682` | health 检查端口 |
| `HEALTH_TIMEOUT` | `2s` | health 检查连接超时 |

## 示例

```bash
# topic stats（单层 topic）
curl http://localhost:8080/admin/v2/persistent/mqtt-public/default/topic01/stats

# topic stats（多层 topic）
curl http://localhost:8080/admin/v2/persistent/mqtt-public/default/home/warn/sub/1/stats

# 创建 / 查询 / 删除 topic 监控
curl -X POST http://localhost:8080/api/v1/topic_metrics/home/warn/sub/1
curl http://localhost:8080/api/v1/topic_metrics/home/warn/sub/1
curl -X DELETE http://localhost:8080/api/v1/topic_metrics/home/warn/sub/1

# 列出全部已注册监控
curl http://localhost:8080/api/v1/topic_metrics

# health
curl http://localhost:8080/admin/v2/brokers/health
```

## Linux 部署（systemd）

### 方式一：使用自动安装脚本（推荐）

从 `release/` 目录下载对应架构的压缩包上传到服务器，解压后执行 `install.sh`：

```bash
# 以 x86_64 为例
tar -xzvf tjyj-api-service-linux-amd64.tar.gz
cd tjyj-api-service-linux-amd64
sudo ./install.sh
```

脚本会自动完成：

- 检测 CPU 架构并选择对应二进制
- 创建运行用户 `tjyj`
- 复制二进制、环境变量配置和 systemd unit 文件
- 加载 systemd、启用并启动服务

### 升级

新版本发布后，用同样的压缩包执行 `upgrade.sh`，会保留现有 `env.conf`，只替换二进制和 service 文件并重启：

```bash
tar -xzvf tjyj-api-service-linux-amd64.tar.gz
cd tjyj-api-service-linux-amd64
sudo ./upgrade.sh
```

### 方式二：手动安装

```bash
# 1. 把二进制和配置文件上传到服务器
sudo mkdir -p /opt/tjyj-api-service
sudo cp tjyj-api-service_linux_amd64 /opt/tjyj-api-service/tjyj-api-service
sudo cp systemd/env.conf /opt/tjyj-api-service/env.conf
sudo chmod +x /opt/tjyj-api-service/tjyj-api-service

# 2. 创建运行用户
sudo useradd -r -s /bin/false tjyj
sudo chown -R tjyj:tjyj /opt/tjyj-api-service

# 3. 安装 systemd 服务文件
sudo cp systemd/tjyj-api-service.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable tjyj-api-service
sudo systemctl start tjyj-api-service

# 4. 查看状态
sudo systemctl status tjyj-api-service
sudo journalctl -u tjyj-api-service -f
```

## 技术栈

- Go 1.22+
- [Gin](https://github.com/gin-gonic/gin) Web 框架
- 标准 `net/http` 客户端
