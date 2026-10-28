package com.apusic.admq.example.jms.scenario;

import com.apusic.admq.example.jms.config.ClientConfig;
import com.apusic.admq.example.jms.connection.ConnectionManager;
import com.apusic.admq.jms.AdmqConnectionFactory;
import com.apusic.admq.jms.config.AdmqConfig;
import com.apusic.admq.jms.gmtls.GmtlsSslContexts;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GMTLS (TLCP) 国密传输加密验证场景。逐步产出验证证据，全部通过才算 PASS：
 *
 * <pre>
 * [1/4] 无证书接入拒绝 —— gmtls:// 未配置双证书时 createConnection 必须抛异常
 *                       （防止客户端静默降级为明文接入）
 * [2/4] TLCP 协商证据  —— 对 broker 端口做裸 TLCP 握手，断言：
 *                       协议 = TLCPv1.1 (GM/T 0024)、套件含 SM4（通道加密）与
 *                       SM3（完整性）、对端证书为 SM2（SM3withSM2 签名）
 * [3/4] 明文接入拒绝   —— 普通 tcp:// 连 gmtls 端口必须失败（端口只接受 TLCP 加密接入）
 * [4/4] 加密通道端到端 —— 经 gmtls:// 完成 JMS 收发（run.mode 控制，默认 BOTH）
 * </pre>
 *
 * 保密性（传输内容不可窃听）需配合抓包验证：加密通道抓包结果中搜不到消息明文，
 * 步骤见 README「GMTLS 国密传输加密测试」。
 */
public class GmtlsTestScenario extends AbstractTestScenario {

    private static final Pattern GMTLS_ENDPOINT = Pattern.compile("gmtls://([^:/,)+]+):(\\d+)");

    private String brokerHost;
    private int brokerPort;

    @Override
    public void execute(ClientConfig config) throws Exception {
        if (!config.isGmtls()) {
            throw new IllegalArgumentException(
                    "GMTLS scenario requires broker.url with gmtls:// scheme, got: " + config.getBrokerUrl());
        }
        parseBrokerEndpoint(config);

        verifyMissingCertRejected(config);
        verifyTlcpNegotiation(config);
        verifyPlaintextRejected();

        LOGGER.info("[GMTLS-VERIFY 4/4] JMS end-to-end over encrypted channel, run.mode={}", config.getRunMode());
        super.execute(config);

        LOGGER.info("============================================================");
        LOGGER.info("GMTLS verification PASSED: TLCPv1.1 negotiated, SM2 peer cert,");
        LOGGER.info("SM4 channel encryption + SM3 integrity, plaintext access rejected.");
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

    /** [1/4] gmtls:// 未配置证书时必须被拒绝（GmtlsConfigurator 在联网前抛出，确定性检查）。 */
    private void verifyMissingCertRejected(ClientConfig config) {
        LOGGER.info("[GMTLS-VERIFY 1/4] gmtls:// without certificates must be rejected");
        try {
            new AdmqConnectionFactory(new AdmqConfig().brokerURL(config.getBrokerUrl())).createConnection();
            throw new IllegalStateException("ASSERT FAILED: gmtls:// without certificates was NOT rejected");
        } catch (JMSException e) {
            check("rejection mentions missing certificate",
                    e.getMessage() != null && e.getMessage().contains("Cert"));
            LOGGER.info("[GMTLS-VERIFY 1/4] PASS: rejected as expected ({}: {})",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /** [2/4] 裸 TLCP 握手，产出协议 / 套件 / 证书算法证据。 */
    private void verifyTlcpNegotiation(ClientConfig config) throws Exception {
        LOGGER.info("[GMTLS-VERIFY 2/4] probing TLCP handshake against {}:{}", brokerHost, brokerPort);

        char[] keyPassword = config.getGmtlsKeyPassword().isEmpty()
                ? null : config.getGmtlsKeyPassword().toCharArray();
        SSLContext context = GmtlsSslContexts.fromPem(
                config.getGmtlsSignCert(), config.getGmtlsSignKey(),
                config.getGmtlsEncCert(), config.getGmtlsEncKey(),
                config.getGmtlsCaCert(), keyPassword);

        try (SSLSocket socket = (SSLSocket) context.getSocketFactory()
                .createSocket(brokerHost, brokerPort)) {
            socket.setEnabledProtocols(new String[]{GmtlsSslContexts.PROTOCOL});
            if (!config.getGmtlsCipherSuites().isEmpty()) {
                socket.setEnabledCipherSuites(config.getGmtlsCipherSuites().split(","));
            }
            socket.startHandshake();

            String protocol = socket.getSession().getProtocol();
            String suite = socket.getSession().getCipherSuite();
            X509Certificate peer = (X509Certificate) socket.getSession().getPeerCertificates()[0];

            LOGGER.info("  negotiated protocol : {}", protocol);
            LOGGER.info("  negotiated suite    : {}", suite);
            LOGGER.info("  peer certificate    : {}", peer.getSubjectX500Principal());
            LOGGER.info("  peer cert signature : {} (public key algorithm: {})",
                    peer.getSigAlgName(), peer.getPublicKey().getAlgorithm());

            check("protocol is TLCPv1.1 (GM/T 0024)", GmtlsSslContexts.PROTOCOL.equals(protocol));
            check("cipher suite uses SM4 channel encryption", suite.contains("SM4"));
            check("cipher suite uses SM3 integrity", suite.contains("SM3"));
            check("peer certificate is SM2 (SM3withSM2 signature)",
                    peer.getSigAlgName() != null && peer.getSigAlgName().contains("SM3"));
            LOGGER.info("[GMTLS-VERIFY 2/4] PASS: protocol={}, suite={}", protocol, suite);
        }
    }

    /** [3/4] 普通 tcp:// 明文协议连 gmtls 端口必须失败。 */
    private void verifyPlaintextRejected() {
        LOGGER.info("[GMTLS-VERIFY 3/4] plaintext tcp:// access to gmtls port must be rejected");
        String plainUrl = "tcp://" + brokerHost + ":" + brokerPort
                + "?soTimeout=5000&connectionTimeout=5000";
        try {
            Connection connection = new AdmqConnectionFactory(plainUrl).createConnection();
            connection.start();
            connection.close();
            throw new IllegalStateException("ASSERT FAILED: plaintext tcp:// was NOT rejected by gmtls port");
        } catch (JMSException e) {
            LOGGER.info("[GMTLS-VERIFY 3/4] PASS: plaintext rejected ({}: {})",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void parseBrokerEndpoint(ClientConfig config) {
        Matcher matcher = GMTLS_ENDPOINT.matcher(config.getBrokerUrl());
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Cannot parse gmtls host:port from broker.url: " + config.getBrokerUrl());
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
