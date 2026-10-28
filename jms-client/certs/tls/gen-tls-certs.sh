#!/usr/bin/env bash
# 生成 TLS 测试证书与 keystore（仅限测试！）
# 产物：测试 CA、服务端证书（broker 用）、客户端证书（mTLS 用）、各类 PKCS12 keystore、
#       以及一个"错误 CA" truststore（用于服务器身份校验负向用例）
# 依赖：OpenSSL 3.x + JDK keytool
set -euo pipefail

# Git Bash (MSYS) 会把 /C=CN/... 形式的 -subj 误转成 Windows 路径
export MSYS_NO_PATHCONV=1

cd "$(dirname "$0")"

PASSWORD="${TLS_STORE_PASSWORD:-changeme}"
DAYS=3650
SUBJ_BASE="/C=CN/ST=Beijing/O=Apusic Test"

echo "== 1. 测试 CA =="
openssl genrsa -out ca.key 2048
openssl req -x509 -new -nodes -key ca.key -sha256 -days $DAYS \
    -subj "$SUBJ_BASE/CN=admq-tls-test-ca" \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -out ca.crt

echo "== 2. 服务端证书（CN=localhost, SAN=localhost/127.0.0.1）=="
cat > server-ext.cnf <<'EOF'
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:localhost,IP:127.0.0.1
EOF
openssl genrsa -out server.key 2048
openssl req -new -key server.key -subj "$SUBJ_BASE/CN=localhost" -out server.csr
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
    -days $DAYS -sha256 -extfile server-ext.cnf \
    -out server.crt

echo "== 3. 客户端证书（mTLS 用）=="
cat > client-ext.cnf <<'EOF'
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=clientAuth
EOF
openssl genrsa -out client.key 2048
openssl req -new -key client.key -subj "$SUBJ_BASE/CN=admq-tls-client" -out client.csr
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
    -days $DAYS -sha256 -extfile client-ext.cnf \
    -out client.crt

echo "== 4. 错误 CA（服务器身份校验负向用例）=="
openssl genrsa -out rogue-ca.key 2048
openssl req -x509 -new -nodes -key rogue-ca.key -sha256 -days $DAYS \
    -subj "$SUBJ_BASE/CN=admq-tls-rogue-ca" \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -out rogue-ca.crt

echo "== 5. PKCS12 keystore（口令：$PASSWORD）=="
# broker 侧：服务端私钥 + 证书链
openssl pkcs12 -export -in server.crt -inkey server.key -certfile ca.crt \
    -name server -out server-keystore.p12 -passout pass:"$PASSWORD"
# 客户端侧（mTLS）：客户端私钥 + 证书链
openssl pkcs12 -export -in client.crt -inkey client.key -certfile ca.crt \
    -name client -out client-keystore.p12 -passout pass:"$PASSWORD"
# 客户端侧 truststore：信任测试 CA（校验服务端）
rm -f client-truststore.p12
keytool -importcert -noprompt -storetype PKCS12 -alias ca -file ca.crt \
    -keystore client-truststore.p12 -storepass "$PASSWORD"
# broker 侧 truststore：信任测试 CA（needClientAuth 时校验客户端）
rm -f server-truststore.p12
keytool -importcert -noprompt -storetype PKCS12 -alias ca -file ca.crt \
    -keystore server-truststore.p12 -storepass "$PASSWORD"
# 错误 truststore：只信"错误 CA"，握手必须失败
rm -f wrong-truststore.p12
keytool -importcert -noprompt -storetype PKCS12 -alias rogue-ca -file rogue-ca.crt \
    -keystore wrong-truststore.p12 -storepass "$PASSWORD"

rm -f server.csr client.csr ca.srl server-ext.cnf client-ext.cnf

echo "完成。keystore 口令：$PASSWORD"
echo "  broker 用：server-keystore.p12 / server-truststore.p12"
echo "  client 用：client-keystore.p12 / client-truststore.p12"
