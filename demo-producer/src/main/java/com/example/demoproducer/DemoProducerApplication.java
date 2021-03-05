package com.example.demoproducer;

import foo.Author;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;

@SpringBootApplication
public class DemoProducerApplication {

    @Bean
    public Function<Flux<Map<String, ?>>, Flux<Author>> processor() {

        return (flux) -> flux
                .map(dataMap -> new Author(dataMap.get("name").toString(), ((BigDecimal)dataMap.get("confidence")).floatValue())).log();
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoProducerApplication.class, args);
    }

}
