package com.fingaurd.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class FraudWebClientConfig {

    @Value("${fraud-detection.service.url:http://localhost:8000}")
    private String fraudServiceUrl;

    @Value("${fraud-detection.service.timeoutMs:2000}")
    private int timeoutMs;

    @Bean
    public WebClient fraudWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
            .responseTimeout(Duration.ofMillis(timeoutMs))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(timeoutMs / 1000))
                .addHandlerLast(new WriteTimeoutHandler(timeoutMs / 1000))
            );

        return builder
            .baseUrl(fraudServiceUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}


