package com.sjsu.wildfirestorage;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

public class Client {
    public static Object get(WebClient webClient, MultiValueMap<String, String> queryParams, ParameterizedTypeReference parameterizedTypeReference) {
        Object response = webClient.get()
                .uri(uriBuilder -> uriBuilder.queryParams(queryParams).build())
                .retrieve()
                .bodyToMono(parameterizedTypeReference)
                .block();
        return response;
    }

    public static WebClient getWebClient(String path) {
        WebClient webClient = WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build()).baseUrl(path).build();
        return webClient;
    }

    public static Object post(WebClient webClient, Object body, ParameterizedTypeReference parameterizedTypeReference) throws ExecutionException, InterruptedException {

        var response = webClient.post()
                .body(Mono.just(body), body.getClass())
                .retrieve()
                .bodyToMono(parameterizedTypeReference)
                .retry(1)
                .toFuture()
                .get();
        return response;
    }

    public static Object post(WebClient webClient, Object body, ParameterizedTypeReference parameterizedTypeReference, Consumer<HttpHeaders> headers) throws ExecutionException, InterruptedException {

        var response = webClient.post()
                .headers(headers)
                .body(Mono.just(body), body.getClass())
                .retrieve()
                .bodyToMono(parameterizedTypeReference)
                .retry(1)
                .toFuture()
                .get();
        return response;
    }
}
