package com.ibm.mq.jms;

import java.io.Serializable;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;

public class MQQueue extends MQDestination implements Queue, Referenceable, Serializable {

    @Override
    public String getQueueName() throws JMSException {
        return "";
    }

    @Override
    public Reference getReference() throws NamingException {
        return null;
    }
}