package org.springframework.jms.support.destination;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

public interface DestinationResolver {
    Destination resolveDestinationName(Session var1, String var2, boolean var3) throws JMSException;
}
