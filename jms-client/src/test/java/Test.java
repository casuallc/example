import com.apusic.admq.jms.AdmqConnection;
import com.apusic.admq.jms.AdmqQueueConnection;
import com.apusic.admq.jms.AdmqQueueConnectionFactory;
import com.apusic.admq.jms.config.AdmqConfig;
import java.util.UUID;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

public class Test {

    public static void main(String[] args) throws Exception {
        transaction();
    }

    static void simpleSend() throws Exception {
        AdmqConfig config = new AdmqConfig();
        config.brokerURL("tcp://172.20.140.17:61616");
        config.username("admin");
        config.password("changeme");
        AdmqQueueConnectionFactory mqFactory = new AdmqQueueConnectionFactory(config);

        AdmqQueueConnection connection = mqFactory.createQueueConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue("test.queue");

        MessageProducer producer = session.createProducer(queue);
        TextMessage msg = session.createTextMessage();
        msg.setStringProperty("messageType", "0");
        msg.setStringProperty("message_id", UUID.randomUUID().toString());
        msg.setText("12313123123");
        producer.send(msg);

        producer.close();
        session.close();
        connection.close();
        System.out.println("send ");
    }

    static void transaction() throws Exception {
        AdmqConfig config = new AdmqConfig();
        config.brokerURL("tcp://172.20.140.17:61616");
        config.username("admin");
        config.password("changeme");
        AdmqQueueConnectionFactory mqFactory = new AdmqQueueConnectionFactory(config);

        AdmqQueueConnection connection = mqFactory.createQueueConnection();
        connection.start();
        Session session = connection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue("test.queue");

        MessageProducer producer = session.createProducer(queue);
        TextMessage msg = session.createTextMessage();
        msg.setStringProperty("messageType", "0");
        msg.setStringProperty("message_id", UUID.randomUUID().toString());
        msg.setText("12313123123");
        producer.send(msg);
        session.rollback();

        producer.close();
        session.close();
        connection.close();
        System.out.println("send ");
    }
}
