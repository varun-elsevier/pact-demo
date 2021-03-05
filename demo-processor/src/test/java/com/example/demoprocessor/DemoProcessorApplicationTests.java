package com.example.demoprocessor;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import foo.Author;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

interface TestBindings {
    @Input("authors")
    MessageChannel input();

    @Output("passed-evaluations")
    MessageChannel passedEvaluations();

    @Output("failed-evaluations")
    MessageChannel failedEvaluations();

    @Output("un-processable-evaluations")
    MessageChannel unProcessableEvaluations();
}

@SpringBootTest
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerType = ProviderType.ASYNCH, providerName = "author-provider")
@EnableBinding(TestBindings.class)
public class DemoProcessorApplicationTests {

    @Autowired
    private TestBindings testBindings;

    @Autowired
    private MessageCollector messageCollector;

    @Pact(consumer = "evaluation")
    MessagePact highConfidenceAuthorPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("name", "Foo Bar");
        body.decimalType("confidence", 0.90);

        Map<String, ?> providerState = Map.of("name", "Foo Bar", "confidence", 0.90);

        return builder.given("high_confidence_author", providerState)
                .expectsToReceive("message with a high confidence author entity")
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "highConfidenceAuthorPact")
    void verifyHighConfidenceAuthorProcessing(MessagePact messagePact) throws InterruptedException, JsonProcessingException {
        Author author = pactBodyAsAuthor(messagePact);
        testBindings.input().send(new GenericMessage<>(author));

        BlockingQueue<Message<?>> channel = messageCollector.forChannel(testBindings.passedEvaluations());
        Author result = (Author) channel.poll(2, TimeUnit.SECONDS).getPayload();

        assertThat(result.getName()).isEqualTo("Foo Bar");
        assertThat(result.getConfidence()).isEqualTo(0.9f);
    }

    @Pact(consumer = "evaluation")
    MessagePact lowConfidenceAuthorPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("name", "Foo Bar");
        body.decimalType("confidence", 0.50);

        Map<String, ?> providerState = Map.of("name", "Foo Bar", "confidence", 0.50);

        return builder.given("low_confidence_author", providerState)
                .expectsToReceive("message with a low confidence author entity")
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "lowConfidenceAuthorPact")
    void verifyLowConfidenceAuthorProcessing(MessagePact messagePact) throws InterruptedException, JsonProcessingException {
        Author author = pactBodyAsAuthor(messagePact);
        testBindings.input().send(new GenericMessage<>(author));

        BlockingQueue<Message<?>> channel = messageCollector.forChannel(testBindings.failedEvaluations());
        Author result = (Author) channel.poll(2, TimeUnit.SECONDS).getPayload();

        assertThat(result.getName()).isEqualTo("Foo Bar");
        assertThat(result.getConfidence()).isEqualTo(0.5f);
    }

    @Pact(consumer = "evaluation")
    MessagePact invalidConfidenceAuthorPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("name", "Foo Bar");
        body.decimalType("confidence", 1.50);

        Map<String, ?> providerState = Map.of("name", "Foo Bar", "confidence", 1.50);

        return builder.given("invalid_confidence_author", providerState)
                .expectsToReceive("message with a invalid confidence author entity")
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "invalidConfidenceAuthorPact")
    void verifyInvalidConfidenceAuthorProcessing(MessagePact messagePact) throws InterruptedException, JsonProcessingException {
        Author author = pactBodyAsAuthor(messagePact);
        testBindings.input().send(new GenericMessage<>(author));

        BlockingQueue<Message<?>> channel = messageCollector.forChannel(testBindings.unProcessableEvaluations());
        Author result = (Author) channel.poll(2, TimeUnit.SECONDS).getPayload();

        assertThat(result.getName()).isEqualTo("Foo Bar");
        assertThat(result.getConfidence()).isEqualTo(1.5f);
    }


    private Author pactBodyAsAuthor(MessagePact messagePact) throws JsonProcessingException {
        String contents = messagePact.getMessages().get(0).contentsAsString();
        return new ObjectMapper().readValue(contents, Author.class);
    }
}
