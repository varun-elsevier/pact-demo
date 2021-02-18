package com.example.demoprocessor;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@SpringBootApplication
public class DemoProcessorApplication {
    public static final Logger LOGGER = LoggerFactory.getLogger(DemoProcessorApplication.class);
    public static final String entitySchemaStr = "{\"type\": \"record\", \"name\": \"Result\", \"fields\": [{\"name\": \"value\", \"type\": \"int\"}]}";
    public static final Schema entitySchema = new Schema.Parser().parse(entitySchemaStr);

    @Bean
    public Function<Flux<GenericRecord>, Flux<GenericRecord>> processor() {

        return flux -> flux
                .map(this::result)
                .onErrorContinue((throwable, o) -> LOGGER.error("", throwable));
    }

    public GenericRecord result(GenericRecord record) {
        GenericData.Record result = new GenericData.Record(entitySchema);
		Integer value = Integer.valueOf(record.get("value").toString());
		result.put("value", value);
        return result;
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoProcessorApplication.class, args);
    }

}
