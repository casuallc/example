# TLS 测试证书

本目录存放标准 TLS (`ssl://`) 测试用证书与 PKCS12 keystore（**仅限测试**）：

| 文件 | 使用方 | 说明 |
|------|--------|------|
| `ca.crt` | 双方 | 测试根 CA（RSA 2048，自签） |
| `server-keystore.p12` | Broker | 服务端私钥 + 证书（CN=localhost，SAN 含 localhost/127.0.0.1） |
| `server-truststore.p12` | Broker | 信任测试 CA，`needClientAuth=true` 时校验客户端证书 |
| `client-keystore.p12` | 客户端 | 客户端私钥 + 证书（mTLS 双向认证用） |
| `client-truststore.p12` | 客户端 | 信任测试 CA，校验服务端证书 |
| `wrong-truststore.p12` | 客户端 | 只信"错误 CA"，用于服务器身份校验负向用例 |

所有 keystore 口令均为 `changeme`。`*.key` 为对应私钥（测试用途，随仓库分发）。

## 重新生成

需 OpenSSL 3.x + JDK keytool：

```bash
bash certs/tls/gen-tls-certs.sh   # 可用 TLS_STORE_PASSWORD 环境变量改口令
```

## Broker 配置（activemq.xml）

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:amq="http://activemq.apache.org/schema/core" ...>

    <amq:sslContext>
        <amq:sslContext keyStore="conf/tls/server-keystore.p12" keyStorePassword="changeme"
                        trustStore="conf/tls/server-truststore.p12" trustStorePassword="changeme"/>
    </amq:sslContext>

    <transportConnectors>
        <!-- needClientAuth=true 启用强制客户端校验（mTLS）；单向认证去掉该参数 -->
        <transportConnector name="ssl" uri="ssl://0.0.0.0:61618?needClientAuth=true"/>
    </transportConnectors>
</beans>
```

## TLS 版本说明

TLSv1.2 / TLSv1.3 在受支持的 JDK（8+ / 11+）上默认启用。TLSv1 / TLSv1.1 自
2021 年起的 JDK 更新版本（含 JDK 17）起被 `jdk.tls.disabledAlgorithms` 默认禁用，
属预期安全加固；如需验证旧版本协商，编辑 `$JAVA_HOME/conf/security/java.security`
从 `jdk.tls.disabledAlgorithms` 中移除 `TLSv1, TLSv1.1` 后重启（broker 与客户端两侧）。
