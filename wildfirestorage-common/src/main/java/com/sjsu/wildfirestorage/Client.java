package com.sjsu.wildfirestorage;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class Client {
    public static Object get(String path, MultiValueMap<String, String> queryParams, ParameterizedTypeReference parameterizedTypeReference) {
        WebClient webClient = WebClient.create(path);

        Object response = webClient.get()
                .uri(uriBuilder -> uriBuilder.queryParams(queryParams).build())
                .retrieve()
                .bodyToMono(parameterizedTypeReference)
                .block();
        return response;
    }

    public static Object post(String path, Object body, ParameterizedTypeReference parameterizedTypeReference){
        WebClient webClient = WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build()).baseUrl(path).build();

        Object response = webClient.post()
                .body(Mono.just(body), Object.class)
                .retrieve()
                .bodyToMono(parameterizedTypeReference)
                .block();
        return response;
    }
}
