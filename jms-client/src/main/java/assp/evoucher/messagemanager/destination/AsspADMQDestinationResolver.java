package assp.evoucher.messagemanager.destination;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.Topic;

public class AsspADMQDestinationResolver extends AsspAbstractDestinationResolver {

    private final Map<String, Queue> queues = new ConcurrentHashMap<>();
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();

    public Queue resolveQueue(Session sc, String s) throws JMSException {
        Queue queue = this.queues.get(s);
        if (queue == null) {
            queue = sc.createQueue(s);
            this.queues.put(s, queue);
        }

        return queue;
    }

    public Topic resolveTopic(Session sc, String s) throws JMSException {
        Topic topic = this.topics.get(s);
        if (topic == null) {
            topic = sc.createTopic(s);
            this.topics.put(s, topic);
        }

        return topic;
    }
}
