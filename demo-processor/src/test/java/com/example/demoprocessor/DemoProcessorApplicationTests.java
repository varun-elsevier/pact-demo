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
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
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

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

interface TestBindings {
    @Input("string-input")
    MessageChannel stringInput();

    @Output("integer-output")
    MessageChannel integerOutput();
}

@SpringBootTest
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerType = ProviderType.SYNCH, providerName = "string-provider")
@EnableBinding(TestBindings.class)
public class DemoProcessorApplicationTests {
    public static final String inputSchemaStr = "{\"type\": \"record\", \"name\": \"Input\", \"fields\": [{\"name\": \"value\", \"type\": \"string\"}]}";
    public static final Schema inputSchema = new Schema.Parser().parse(inputSchemaStr);

    @Autowired
    private TestBindings testBindings;

    @Autowired
    private MessageCollector messageCollector;

    @Pact(consumer = "str-to-int-mapper")
    MessagePact validNumberMessage(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("value", "5");

        return builder.given("valid_number", singletonMap("value", "5"))
                .expectsToReceive("message with a valid number value")
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "validNumberMessage")
    void convertsStringToIntAndSentToOutputChannel(MessagePact messagePact) throws InterruptedException, JsonProcessingException {
        HashMap<String, String> map = pactBodyAsMap(messagePact);

        testBindings.stringInput().send(new GenericMessage<>(inputRecordOf(map)));

        BlockingQueue<Message<?>> channel = messageCollector.forChannel(testBindings.integerOutput());
        GenericRecord result = (GenericRecord) channel.poll(1, TimeUnit.SECONDS).getPayload();

        assertThat(result.get("value")).isEqualTo(5);
    }

    @Pact(consumer = "str-to-int-mapper")
    MessagePact invalidNumberMessage(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("value", "foo");

        return builder.given("invalid_number", singletonMap("value", "foo"))
                .expectsToReceive("message with a invalid number value")
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "invalidNumberMessage")
    void invalidNumbersShouldNotHaveAnyMessagesPublishedOnOutputChannel(MessagePact messagePact) throws InterruptedException, JsonProcessingException {
        HashMap<String, String> map = pactBodyAsMap(messagePact);

        testBindings.stringInput().send(new GenericMessage<>(inputRecordOf(map)));

        BlockingQueue<Message<?>> channel = messageCollector.forChannel(testBindings.integerOutput());
        Message<?> message = channel.poll(1, TimeUnit.SECONDS);

        assertThat(message).isNull();
    }

    @Pact(consumer = "str-to-int-mapper")
    MessagePact negativeNumberMessage(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("value", "");


        return builder.given("negative_number")
                .expectsToReceive("message with a negative number value")
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "negativeNumberMessage")
    void convertsNegativeNumberStringToIntAndSentToOutputChannel(MessagePact messagePact) throws InterruptedException, JsonProcessingException {
        HashMap<String, String> map = pactBodyAsMap(messagePact);

        testBindings.stringInput().send(new GenericMessage<>(inputRecordOf(map)));

        BlockingQueue<Message<?>> channel = messageCollector.forChannel(testBindings.integerOutput());
        GenericRecord result = (GenericRecord) channel.poll(1, TimeUnit.SECONDS).getPayload();

        assertThat(result.get("value")).isEqualTo(-5);
    }

    private HashMap<String, String> pactBodyAsMap(MessagePact messagePact) throws JsonProcessingException {
        String contents = messagePact.getMessages().get(0).contentsAsString();
        HashMap<String, String> map = new ObjectMapper().readValue(contents, HashMap.class);
        return map;
    }

    public static GenericRecord inputRecordOf(HashMap<String, String> map) {
        GenericData.Record record = new GenericData.Record(inputSchema);
        map.forEach(record::put);
        return record;
    }

}
