package com.example.demoproducer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@SpringBootApplication
public class DemoProducerApplication {
	public static final String inputSchemaStr = "{\"type\": \"record\", \"name\": \"Input\", \"fields\": [{\"name\": \"value\", \"type\": \"string\"}]}";
	public static final Schema inputSchema = new Schema.Parser().parse(inputSchemaStr);

    @Bean
    public Function<Flux<String>, Flux<GenericRecord>> processor() {

        return (flux) -> flux
                .map(DemoProducerApplication::recordOf).log();
    }

    public static GenericRecord recordOf(String number) {
        GenericData.Record result = new GenericData.Record(inputSchema);
        result.put("value", number);
        return result;
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoProducerApplication.class, args);
    }

}
