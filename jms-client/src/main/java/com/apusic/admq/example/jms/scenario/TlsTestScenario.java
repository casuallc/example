package com.apusic.admq.example.jms.scenario;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import com.apusic.admq.example.jms.connection.TlsSupport;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TLS (ssl://) 传输加密验证场景。逐步产出验证证据：
 *
 * <pre>
 * [1/5] 服务器身份校验 —— 用"错误 CA"truststore 握手必须失败（客户端确实校验了服务端证书）
 * [2/5] 版本自适应     —— 分别强制 TLSv1.3 / TLSv1.2 / TLSv1.1 / TLSv1 裸握手：
 *                        TLSv1.2 必须成功（所有受支持 JDK 默认启用）；
 *                        TLSv1.3 在双方支持时成功，否则 SKIP；
 *                        TLSv1.1 / TLSv1 被 JVM 安全策略禁用时 SKIP（jdk.tls.disabledAlgorithms，
 *                        属预期安全加固，非功能缺陷；启用方法见 README）
 * [3/5] 强制客户端校验 —— broker 开启 needClientAuth 时（tls.client.auth.required=true），
 *                        无客户端证书握手必须失败
 * [4/5] 双向认证接入   —— 携带客户端证书握手成功（配置了 tls.key.store 时）
 * [5/5] 加密通道端到端 —— 经 ssl:// 完成 JMS 收发（run.mode 控制，默认 BOTH）
 * </pre>
 *
 * 保密性（传输内容不可窃听）需配合抓包验证：加密通道抓包结果中搜不到消息明文，
 * 步骤见 README「TLS 传输加密测试」。
 */
public class TlsTestScenario extends AbstractTestScenario {

    private static final Pattern TLS_ENDPOINT = Pattern.compile("ssl://([^:/,)+]+):(\\d+)");

    /** 版本探针：required=必须 PASS；否则 PASS/SKIP 均可（取决于双方安全策略）。 */
    private static final String[][] VERSION_PROBES = {
            {"TLSv1.3", "optional"},
            {"TLSv1.2", "required"},
            {"TLSv1.1", "optional"},
            {"TLSv1",   "optional"},
    };

    private String brokerHost;
    private int brokerPort;

    @Override
    public void execute(ClientConfig config) throws Exception {
        if (!config.isTls()) {
            throw new IllegalArgumentException(
                    "TLS scenario requires broker.url with ssl:// scheme, got: " + config.getBrokerUrl());
        }
        if (config.getTlsTrustStore().isEmpty()) {
            throw new IllegalArgumentException("TLS scenario requires tls.trust.store to verify the broker");
        }
        parseBrokerEndpoint(config);

        verifyServerIdentityRejected(config);
        verifyVersionNegotiation(config);
        verifyClientAuthEnforced(config);
        verifyMutualHandshake(config);

        LOGGER.info("[TLS-VERIFY 5/5] JMS end-to-end over encrypted channel, run.mode={}", config.getRunMode());
        super.execute(config);

        LOGGER.info("============================================================");
        LOGGER.info("TLS verification PASSED: server identity enforced, version");
        LOGGER.info("negotiation adaptive, encrypted channel carries JMS traffic.");
        LOGGER.info("============================================================");
    }

    @Override
    protected void doExecute(ClientConfig config, ConnectionManager connectionManager) throws Exception {
        if (config.isSendOnly()) {
            sendOnly(config, connectionManager);
        } else if (config.isReceiveOnly()) {
            receiveOnly(config, connectionManager);
        } else {
            sendAndReceive(config, connectionManager);
        }
    }

    /** [1/5] 错误 CA 必须拒绝服务端证书。 */
    private void verifyServerIdentityRejected(ClientConfig config) throws Exception {
        if (config.getTlsWrongTrustStore().isEmpty()) {
            LOGGER.info("[TLS-VERIFY 1/5] SKIP: tls.wrong.trust.store not configured");
            return;
        }
        LOGGER.info("[TLS-VERIFY 1/5] handshake with wrong CA truststore must be rejected");
        SSLContext context = TlsSupport.build(config, false, config.getTlsWrongTrustStore());
        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(brokerHost, brokerPort)) {
            socket.startHandshake();
            throw new IllegalStateException(
                    "ASSERT FAILED: server certificate was accepted with a wrong CA (server identity not verified)");
        } catch (SSLHandshakeException e) {
            LOGGER.info("[TLS-VERIFY 1/5] PASS: wrong CA rejected as expected ({}: {})",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /** [2/5] 版本自适应矩阵：强制单版本握手，协商结果必须与强制版本一致。 */
    private void verifyVersionNegotiation(ClientConfig config) throws Exception {
        LOGGER.info("[TLS-VERIFY 2/5] TLS version adaptation against {}:{}", brokerHost, brokerPort);
        boolean withClientCert = config.hasTlsKeyStore();
        int passed = 0;
        int skipped = 0;

        for (String[] probe : VERSION_PROBES) {
            String version = probe[0];
            boolean required = "required".equals(probe[1]);
            SSLContext context = TlsSupport.build(config, withClientCert, null);
            try (SSLSocket socket = (SSLSocket) context.getSocketFactory()
                    .createSocket(brokerHost, brokerPort)) {
                socket.setEnabledProtocols(new String[]{version});
                socket.startHandshake();
                String negotiated = socket.getSession().getProtocol();
                String suite = socket.getSession().getCipherSuite();
                check("negotiated protocol equals forced version " + version, version.equals(negotiated));
                LOGGER.info("  {} ... PASS (cipher: {})", version, suite);
                passed++;
            } catch (SSLHandshakeException | java.net.SocketException e) {
                if (required) {
                    throw new IllegalStateException("ASSERT FAILED: " + version
                            + " must be negotiated but handshake failed: " + e.getMessage(), e);
                }
                LOGGER.info("  {} ... SKIP ({}: {})", version, e.getClass().getSimpleName(), e.getMessage());
                skipped++;
            }
        }

        check("at least one TLS version negotiated", passed > 0);
        LOGGER.info("[TLS-VERIFY 2/5] PASS: {} version(s) negotiated, {} skipped by security policy",
                passed, skipped);
    }

    /** [3/5] broker 强制客户端校验时，无证书接入必须失败。 */
    private void verifyClientAuthEnforced(ClientConfig config) throws Exception {
        if (!config.isTlsClientAuthRequired()) {
            LOGGER.info("[TLS-VERIFY 3/5] SKIP: tls.client.auth.required=false "
                    + "(broker 未开启 needClientAuth 时本步不适用)");
            return;
        }
        LOGGER.info("[TLS-VERIFY 3/5] client without certificate must be rejected (needClientAuth)");
        SSLContext context = TlsSupport.build(config, false, null);
        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(brokerHost, brokerPort)) {
            socket.setSoTimeout(3000);
            socket.startHandshake();
            // TLS 1.3 下服务端在握手完成后才校验客户端证书，拒绝以 post-handshake alert 到达；
            // 读一次以触发该 alert：异常或连接关闭 = 已拒绝；收到数据 = 未拒绝
            int b = socket.getInputStream().read();
            if (b >= 0) {
                throw new IllegalStateException("ASSERT FAILED: client without certificate was NOT rejected. "
                        + "确认 broker ssl connector 已配置 needClientAuth=true");
            }
            LOGGER.info("[TLS-VERIFY 3/5] PASS: certificate-less connection closed by server");
        } catch (java.net.SocketTimeoutException e) {
            throw new IllegalStateException("ASSERT FAILED: client without certificate completed "
                    + "handshake and connection stayed open (broker 未强制客户端校验，"
                    + "确认 ssl connector 已配置 needClientAuth=true)", e);
        } catch (IOException e) {
            LOGGER.info("[TLS-VERIFY 3/5] PASS: certificate-less client rejected ({}: {})",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /** [4/5] 携带客户端证书的双向认证握手。 */
    private void verifyMutualHandshake(ClientConfig config) throws Exception {
        if (!config.hasTlsKeyStore()) {
            LOGGER.info("[TLS-VERIFY 4/5] SKIP: tls.key.store not configured");
            return;
        }
        LOGGER.info("[TLS-VERIFY 4/5] mutual handshake with client certificate");
        SSLContext context = TlsSupport.build(config, true, null);
        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(brokerHost, brokerPort)) {
            socket.startHandshake();
            X509Certificate peer = (X509Certificate) socket.getSession().getPeerCertificates()[0];
            X509Certificate local = socket.getSession().getLocalCertificates() != null
                    ? (X509Certificate) socket.getSession().getLocalCertificates()[0] : null;

            LOGGER.info("  negotiated : {} / {}", socket.getSession().getProtocol(),
                    socket.getSession().getCipherSuite());
            LOGGER.info("  server cert: {}", peer.getSubjectX500Principal());
            LOGGER.info("  client cert: {}", local != null ? local.getSubjectX500Principal() : "(none)");
            check("client certificate was presented", local != null);
            LOGGER.info("[TLS-VERIFY 4/5] PASS: mutual TLS established");
        }
    }

    private void parseBrokerEndpoint(ClientConfig config) {
        Matcher matcher = TLS_ENDPOINT.matcher(config.getBrokerUrl());
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Cannot parse ssl:// host:port from broker.url: " + config.getBrokerUrl());
        }
        this.brokerHost = matcher.group(1);
        this.brokerPort = Integer.parseInt(matcher.group(2));
    }

    private void check(String label, boolean ok) {
        if (!ok) {
            throw new IllegalStateException("ASSERT FAILED: " + label);
        }
        LOGGER.info("  [assert] {} ... OK", label);
    }
}
