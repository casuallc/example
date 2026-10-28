package com.apusic.admq.example.jms.scenario;

import com.apusic.admq.example.jms.config.ClientConfig;

public interface TestScenario {

    void execute(ClientConfig config) throws Exception;
}
