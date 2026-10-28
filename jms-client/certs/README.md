# 测试证书

本目录存放传输加密测试用证书（**仅限测试使用**）：

- 根目录：`gmtls://`（TLCP 国密）客户端 SM2 双证书与根 CA 证书（PEM 格式）
- `tls/`：`ssl://`（标准 TLS）RSA 测试 CA、服务端/客户端 keystore 等，详见 `tls/README.md`

## GMTLS 证书（根目录）

| 文件 | 说明 |
|------|------|
| `ca.crt` | 根 CA 证书，用于校验服务端 |
| `client-sign.crt` / `client-sign.key` | 客户端签名证书与私钥（PKCS8 PEM） |
| `client-enc.crt` / `client-enc.key` | 客户端加密证书与私钥（PKCS8 PEM） |

这些是自签证书，复制自 `admq-jms-client` 仓库的 `poc/gmtls/certs/`。重新生成方式
（需 OpenSSL 3.x，含 SM2/SM3）：

```bash
bash poc/gmtls/scripts/gen-gmtls-certs.sh
```

生产环境证书应由国密 CA 签发。测试证书 SAN 含 `localhost`/`127.0.0.1`，
连接其他主机时需确保证书 SAN 与主机名匹配。
