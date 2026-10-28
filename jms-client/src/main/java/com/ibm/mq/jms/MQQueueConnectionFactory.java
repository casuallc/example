package com.ibm.mq.jms;

import jakarta.jms.Connection;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;

public class MQQueueConnectionFactory implements QueueConnectionFactory {
    public void setHostName(String hostName) {}
    public void setPort(int port) {}
    public void setCCSID(int ccsid) {}
    public void setChannel(String channel) {}
    public void setQueueManager(String queueManager) {}
    public void setTransportType(int transportType) {}

    @Override
    public QueueConnection createQueueConnection() throws JMSException { return null; }
    @Override
    public QueueConnection createQueueConnection(String userName, String password) throws JMSException { return null; }
    @Override
    public Connection createConnection() throws JMSException { return null; }
    @Override
    public Connection createConnection(String userName, String password) throws JMSException { return null; }
    @Override
    public JMSContext createContext() { return null; }
    @Override
    public JMSContext createContext(String userName, String password) { return null; }
    @Override
    public JMSContext createContext(String userName, String password, int sessionMode) { return null; }
    @Override
    public JMSContext createContext(int sessionMode) { return null; }
}
