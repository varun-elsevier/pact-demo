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
// NOTE: In production this will be replaced with a pact broker to get the contracts.
@PactFolder("../demo-processor/build/pacts")
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

    @State({"valid_confidence"})
    public void highConfidenceAuthorState(Map providerState) {
        testBindings.source().send(new GenericMessage<>(providerState));
    }

    @PactVerifyProvider("message containing author entity with a valid confidence (between 0 and 1)")
    public String verifyHighConfidenceAuthor() throws InterruptedException {
        Author author = (Author) messageCollector.forChannel(testBindings.authors())
                .poll(2, TimeUnit.SECONDS).getPayload();
        return author.toString();
    }
}

