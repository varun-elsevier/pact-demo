package com.example.demoproducer;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.AmpqTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.apache.avro.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import java.util.Map;
import java.util.concurrent.TimeUnit;

interface TestBindings {
    @Output("string-input")
    MessageChannel stringInput();

    @Input("source")
    MessageChannel source();
}

@Provider("string-provider")
@PactFolder("/Users/sharm38/projects/ux/demos/pact-demo/demo-processor/build/pacts")
@SpringBootTest
@EnableBinding(TestBindings.class)
public class DemoProducerApplicationTests {
    public static final String inputSchemaStr = "{\"type\": \"record\", \"name\": \"Input\", \"fields\": [{\"name\": \"value\", \"type\": \"string\"}]}";
    public static final Schema inputSchema = new Schema.Parser().parse(inputSchemaStr);
    public static final Logger LOGGER = LoggerFactory.getLogger(DemoProducerApplicationTests.class);

    @Autowired
    private TestBindings testBindings;

    @Autowired
    private MessageCollector messageCollector;


    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(Pact pact, Interaction interaction, PactVerificationContext context) {
        LOGGER.info("testTemplate called: " + pact.getProvider().getName() + ", " + interaction);
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new AmpqTestTarget());
    }

    @State("valid_number")
    public void validNumbersState(Map providerState) {
        testBindings.source().send(new GenericMessage<>(providerState.get("value")));
    }

    @PactVerifyProvider("message with a valid number value")
    public String verifyValidNumber() throws InterruptedException {
        Object payload = messageCollector.forChannel(testBindings.stringInput())
                .poll(1, TimeUnit.SECONDS).getPayload();
        System.out.println(payload);
        return payload.toString();
    }

    @State("invalid_number")
    public void invalidNumbersState(Map providerState) {
        testBindings.source().send(new GenericMessage<>(providerState.get("value")));
    }

    @PactVerifyProvider("message with a invalid number value")
    public String verifyInvalidNumber() throws InterruptedException {
        Object payload = messageCollector.forChannel(testBindings.stringInput())
                .poll(1, TimeUnit.SECONDS).getPayload();

        return payload.toString();
    }

    @State("negative_number")
    public void negativeNumbersState(Map providerState) {
        testBindings.source().send(new GenericMessage<>(providerState.get("value")));
    }

    @PactVerifyProvider("message with a negative number value")
    public String verifyNegativeNumber() throws InterruptedException {
        Object payload = messageCollector.forChannel(testBindings.stringInput())
                .poll(1, TimeUnit.SECONDS).getPayload();

        return payload.toString();
    }

}

