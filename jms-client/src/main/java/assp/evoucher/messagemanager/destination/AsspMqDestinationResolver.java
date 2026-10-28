package assp.evoucher.messagemanager.destination;

import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.Topic;

public class AsspMqDestinationResolver extends AsspAbstractDestinationResolver {
    @Override
    public Queue resolveQueue(Session var1, String var2) throws JMSException {
        return null;
    }

    @Override
    public Topic resolveTopic(Session var1, String var2) throws JMSException {
        return null;
    }
}
