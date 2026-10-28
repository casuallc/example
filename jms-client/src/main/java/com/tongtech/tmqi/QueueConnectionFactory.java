package com.tongtech.tmqi;

import jakarta.jms.Connection;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.QueueConnection;

public class QueueConnectionFactory implements jakarta.jms.QueueConnectionFactory {
    public QueueConnectionFactory() throws JMSException {}
    public void setProperty(String name, String value) {}

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
