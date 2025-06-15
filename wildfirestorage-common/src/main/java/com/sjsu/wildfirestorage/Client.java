package com.sjsu.wildfirestorage;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

public class Client {
    public static <T> T get(WebClient webClient,
                             MultiValueMap<String, String> queryParams,
                             ParameterizedTypeReference<T> parameterizedTypeReference) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.queryParams(queryParams).build())
                .retrieve()
                .bodyToMono(parameterizedTypeReference)
                .block();
    }

    public static WebClient getWebClient(String path) {
        WebClient webClient = WebClient.builder()
                .exchangeStrategies(ExchangeStrategies.builder()
                                            .codecs(configurer -> configurer.defaultCodecs()
                                                    .maxInMemorySize(16 * 1024 * 1024))
                                            .build())
                .baseUrl(path)
                .build();
        return webClient;
    }

    public static WebClient getWebClient(String path, String token) {
        return WebClient.builder()
                .exchangeStrategies(ExchangeStrategies.builder()
                                            .codecs(configurer -> configurer.defaultCodecs()
                                                    .maxInMemorySize(16 * 1024 * 1024))
                                            .build())
                .baseUrl(path)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    public static Object post(WebClient webClient,
                              Object body,
                              ParameterizedTypeReference parameterizedTypeReference) throws ExecutionException,
            InterruptedException {

        var response = webClient.post()
                .body(Mono.just(body), body.getClass())
                .retrieve()
                .bodyToMono(parameterizedTypeReference)
                .retry(1)
                .toFuture()
                .get();
        return response;
    }

    public static <T> T post(WebClient webClient,
                              Object body,
                              ParameterizedTypeReference<T> parameterizedTypeReference,
                              Consumer<HttpHeaders> headers) throws ExecutionException, InterruptedException {

        return webClient.post()
                .headers(headers)
                .body(Mono.just(body), body.getClass())
                .retrieve()
                .bodyToMono(parameterizedTypeReference)
                .retry(1)
                .onErrorComplete(e -> {
                    System.err.println(e.getMessage());
                    return true;
                })
                .block();
    }

    public static <T> T get(String url, String token) throws WebClientResponseException {
        return getWebClient(url, token)
                .get()
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<T>() {})
                .retry(1)
                .block();
    }

    public static <T> T post(String url,
                             Object body,
                             String token) {
        return getWebClient(url, token)
                .post()
                .body(Mono.just(body), body.getClass())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<T>() {})
                .retry(1)
                .onErrorComplete(e -> {
                    System.err.println(e.getMessage());
                    return true;
                })
                .block();
    }
}
