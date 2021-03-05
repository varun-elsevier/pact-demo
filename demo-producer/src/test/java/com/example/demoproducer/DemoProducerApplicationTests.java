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
import foo.Author;
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
    @Output("authors")
    MessageChannel authors();

    @Input("source")
    MessageChannel source();
}

@Provider("author-provider")
@PactFolder("/Users/sharm38/projects/ux/demos/pact-demo/demo-processor/build/pacts")
@SpringBootTest
@EnableBinding(TestBindings.class)
public class DemoProducerApplicationTests {
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

    @State({"high_confidence_author", "low_confidence_author", "invalid_confidence_author"})
    public void highConfidenceAuthorState(Map providerState) {
        testBindings.source().send(new GenericMessage<>(providerState));
    }

    @PactVerifyProvider("message with a high confidence author entity")
    public String verifyHighConfidenceAuthor() throws InterruptedException {
        Author author = (Author) messageCollector.forChannel(testBindings.authors())
                .poll(2, TimeUnit.SECONDS).getPayload();
        return author.toString();
    }

    @PactVerifyProvider("message with a low confidence author entity")
    public String verifyLowConfidenceAuthor() throws InterruptedException {
        Author author = (Author) messageCollector.forChannel(testBindings.authors())
                .poll(2, TimeUnit.SECONDS).getPayload();
        return author.toString();
    }

    @PactVerifyProvider("message with a invalid confidence author entity")
    public String verifyInvalidConfidenceAuthor() throws InterruptedException {
        Author author = (Author) messageCollector.forChannel(testBindings.authors())
                .poll(2, TimeUnit.SECONDS).getPayload();
        return author.toString();
    }
}

