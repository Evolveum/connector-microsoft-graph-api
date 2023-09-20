package com.evolveum.polygon.connector.msgraphapi;

public class MockGraphEndpoint extends GraphEndpoint {

    MockGraphEndpoint(MSGraphConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected void authenticate() {
        // Do nothing
    }

    @Override
    protected void initHttpClient() {
        // Do nothing
    }
}
