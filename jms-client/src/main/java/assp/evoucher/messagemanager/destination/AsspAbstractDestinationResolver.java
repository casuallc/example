package assp.evoucher.messagemanager.destination;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import org.springframework.jms.support.destination.DestinationResolver;

public abstract class AsspAbstractDestinationResolver implements DestinationResolver {
    public AsspAbstractDestinationResolver() {
    }

    public Destination resolveDestinationName(Session session, String s, boolean flag) throws JMSException {
        return (Destination)(flag ? this.resolveTopic(session, s) : this.resolveQueue(session, s));
    }

    public abstract Queue resolveQueue(Session var1, String var2) throws JMSException;

    public abstract Topic resolveTopic(Session var1, String var2) throws JMSException;

    public void setqMgrName(String evConfValue) {
    }

    public void setCCSID(Integer integer) {
    }

    public void setPersistence(int i) {
    }

    public void setTargetClient(int i) {

    }
}
