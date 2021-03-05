package com.example.demoprocessor;

import foo.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.function.Function;
import java.util.function.Predicate;

@SpringBootApplication
public class DemoProcessorApplication {
    public static final Logger LOGGER = LoggerFactory.getLogger(DemoProcessorApplication.class);

    static Predicate<Author> highConfidenceFilter =
            author -> author.getConfidence() >= 8 && author.getConfidence() <= 10;
    static Predicate<Author> lowConfidenceFilter =
            author -> author.getConfidence() > 1 && author.getConfidence() < 8;
    static Predicate<Author> invalidEntityFilter =
            author -> author.getConfidence() < 1 || author.getConfidence() > 10;

    @Bean
    public Function<Flux<Author>, Tuple3<Flux<Author>, Flux<Author>, Flux<Author>>> processor() {

        return flux -> {
            Flux<Author> connectedFlux = flux.publish().autoConnect(4);

            connectedFlux.subscribe(author -> LOGGER.info("received {}", author));
            Flux<Author> highConfidence = connectedFlux.filter(highConfidenceFilter);
            Flux<Author> lowConfidence = connectedFlux.filter(lowConfidenceFilter);
            Flux<Author> invalidEntities = connectedFlux.filter(invalidEntityFilter);

            return Tuples.of(highConfidence, lowConfidence, invalidEntities);
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoProcessorApplication.class, args);
    }

}
