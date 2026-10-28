package assp.evoucher.messagemanager.adapter;

import assp.evoucher.common.adapter.ControlParamsHandler;
import assp.evoucher.common.util.PropertiesHander;
import com.apusic.admq.jms.AdmqConnection;
import com.apusic.admq.jms.AdmqConnectionFactory;
import com.apusic.admq.jms.AdmqQueueConnection;
import com.apusic.admq.jms.AdmqQueueConnectionFactory;
import com.apusic.admq.jms.config.AdmqConfig;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import jakarta.jms.Connection;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Repository;

@Repository("queueFactoryAdapter")
public class QueueFactoryAdapter implements QueueConnectionFactory, InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(QueueFactoryAdapter.class);
    private static QueueConnectionFactory iN;

    public QueueFactoryAdapter() {
    }

    public Connection createConnection() throws JMSException {
        return iN.createConnection();
    }

    public Connection createConnection(String arg0, String arg1) throws JMSException {
        return iN.createConnection(arg0, arg1);
    }

    public JMSContext createContext() {
        return null;
    }

    public JMSContext createContext(String s, String s1) {
        return null;
    }

    public JMSContext createContext(String s, String s1, int i) {
        return null;
    }

    public JMSContext createContext(int i) {
        return null;
    }

    public QueueConnection createQueueConnection() throws JMSException {
        return iN.createQueueConnection();
    }

    public QueueConnection createQueueConnection(String arg0, String arg1) throws JMSException {
        return iN.createQueueConnection(arg0, arg1);
    }

    public void afterPropertiesSet() {
        if ("TLQ".equals(ControlParamsHandler.getM_MESSAGE_SERVICE())) {
            try {
                String hostName = PropertiesHander.getEVConfValue("evoucher.service.msg.tlq.HostName");
                int port = Integer.valueOf(PropertiesHander.getEVConfValue("evoucher.service.msg.tlq.JMSPort"));
                com.tongtech.tmqi.QueueConnectionFactory tlqFactory = new com.tongtech.tmqi.QueueConnectionFactory();
                String url = "tlkq://" + hostName + ":" + port;
                tlqFactory.setProperty("tmqiAddressList", url);
                iN = tlqFactory;
            } catch (JMSException var6) {
                JMSException e = var6;
                logger.error("QueueConnectionFactory 建立连接失败...");
                e.printStackTrace();
            }
        } else if ("MQ".equals(ControlParamsHandler.getM_MESSAGE_SERVICE())) {
            try {
                MQQueueConnectionFactory mqFactory = new MQQueueConnectionFactory();
                mqFactory.setHostName(PropertiesHander.getEVConfValue("evoucher.service.msg.mq.HostName"));
                mqFactory.setPort(Integer.valueOf(PropertiesHander.getEVConfValue("evoucher.service.msg.mq.Port")));
                mqFactory.setCCSID(Integer.valueOf(PropertiesHander.getEVConfValue("evoucher.service.msg.mq.CCSID")));
                mqFactory.setChannel(PropertiesHander.getEVConfValue("evoucher.service.msg.mq.Channel"));
                mqFactory.setQueueManager(PropertiesHander.getEVConfValue("evoucher.service.msg.mq.QMgr"));
                mqFactory.setTransportType(1);
                iN = mqFactory;
            } catch (Exception var5) {
                Exception e = var5;
                logger.error("MQQueueConnectionFactory 建立连接失败...");
                e.printStackTrace();
            }
        } else if ("ADMQ".equals(ControlParamsHandler.getM_MESSAGE_SERVICE())) {
            try {
                AdmqConfig config = new AdmqConfig();
                config.brokerURL("tcp://10.100.57.27:61616");
                config.username("admin");
                config.password("changeme");
                config.detectionPort(0);
                AdmqQueueConnectionFactory mqFactory = new AdmqQueueConnectionFactory(config);
                iN = mqFactory;

                AdmqQueueConnection connection = mqFactory.createQueueConnection();
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue queue = session.createQueue("qu01");

                MessageProducer producer = session.createProducer(queue);
                TextMessage message = session.createTextMessage("123");
                producer.send(message);

                producer.close();
                session.close();
                connection.close();
            } catch (Exception var5) {
                Exception e = var5;
                logger.error("MQQueueConnectionFactory 建立连接失败...");
                e.printStackTrace();
            }
        }

    }
}